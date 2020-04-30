/*
 * Copyright 2016-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.rest.client.config;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.common.security.core.support.OAuth2AccessTokenProvidingClientHttpRequestInterceptor;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.dsl.Stream;
import org.springframework.cloud.dataflow.rest.client.dsl.StreamBuilder;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class to provide default beans for {@link DataFlowOperations} and {@link org.springframework.cloud.dataflow.rest.client.dsl.StreamBuilder} instances.
 * @author Vinicius Carvalho
 * @author Gunnar Hillert
 */
@Configuration
@EnableConfigurationProperties(DataFlowClientProperties.class)
public class DataFlowClientAutoConfiguration {

	private static Log logger = LogFactory.getLog(DataFlowClientAutoConfiguration.class);

	private static final String DEFAULT_REGISTRATION_ID = "default";

	@Autowired
	private DataFlowClientProperties properties;

	@Autowired(required = false)
	private RestTemplate restTemplate;

	@Autowired
	private @Nullable ClientRegistrationRepository clientRegistrations;

	@Bean
	@ConditionalOnMissingBean(DataFlowOperations.class)
	public DataFlowOperations dataFlowOperations() throws Exception{
		RestTemplate template = DataFlowTemplate.prepareRestTemplate(restTemplate);
		final HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create(new URI(properties.getServerUri()))
				.skipTlsCertificateVerification(properties.isSkipSslValidation());

		if (StringUtils.hasText(this.properties.getAuthentication().getAccessToken())) {
			template.getInterceptors().add(new OAuth2AccessTokenProvidingClientHttpRequestInterceptor(this.properties.getAuthentication().getAccessToken()));
			logger.debug("Configured OAuth2 Access Token for accessing the Data Flow Server");
		}
		else if (StringUtils.hasText(this.properties.getAuthentication().getClientId())) {
			template.getInterceptors().add(clientCredentialsTokenResolvingInterceptor(clientRegistrations,
					this.properties.getAuthentication().getClientId()));
			logger.debug("Configured OAuth2 Client Credentials for accessing the Data Flow Server");
		}
		else if(!StringUtils.isEmpty(properties.getAuthentication().getBasic().getUsername()) &&
				!StringUtils.isEmpty(properties.getAuthentication().getBasic().getPassword())){
			httpClientConfigurer.basicAuthCredentials(properties.getAuthentication().getBasic().getUsername(), properties.getAuthentication().getBasic().getPassword());
			template.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());
		}
		else {
			logger.debug("Not configuring security for accessing the Data Flow Server");
		}
		return new DataFlowTemplate(new URI(properties.getServerUri()), template);
	}

	@Bean
	@ConditionalOnMissingBean(StreamBuilder.class)
	public StreamBuilder streamBuilder(DataFlowOperations dataFlowOperations){
		return Stream.builder(dataFlowOperations);
	}

	@ConditionalOnProperty(prefix = DataFlowPropertyKeys.PREFIX + "client.authentication", name = "client-id")
	@Configuration
	static class ClientCredentialsConfiguration {

		@Bean
		public InMemoryClientRegistrationRepository clientRegistrationRepository(
			DataFlowClientProperties properties) {
			ClientRegistration clientRegistration = ClientRegistration
				.withRegistrationId(DEFAULT_REGISTRATION_ID)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.tokenUri(properties.getAuthentication().getTokenUri())
				.clientId(properties.getAuthentication().getClientId())
				.clientSecret(properties.getAuthentication().getClientSecret())
				.scope(properties.getAuthentication().getScope())
				.build();
			return new InMemoryClientRegistrationRepository(clientRegistration);
		}

		@Bean
		OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient() {
			return new DefaultClientCredentialsTokenResponseClient();
		}
	}

	private ClientHttpRequestInterceptor clientCredentialsTokenResolvingInterceptor(
			ClientRegistrationRepository clientRegistrationRepository,
			String clientId) {
		Authentication principal = createAuthentication(clientId);
		OAuth2AuthorizedClientService authorizedClientService = new InMemoryOAuth2AuthorizedClientService(
				clientRegistrationRepository);
		AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				clientRegistrationRepository, authorizedClientService);
		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				.clientCredentials().build();
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
				.withClientRegistrationId(DEFAULT_REGISTRATION_ID).principal(principal).build();

		return (request, body, execution) -> {
			OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
			request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
			return execution.execute(request, body);
		};
	}

	private static Authentication createAuthentication(final String principalName) {
		return new AbstractAuthenticationToken(null) {
			private static final long serialVersionUID = -2038812908189509872L;

			@Override
			public Object getCredentials() {
				return "";
			}

			@Override
			public Object getPrincipal() {
				return principalName;
			}
		};
	}
}
