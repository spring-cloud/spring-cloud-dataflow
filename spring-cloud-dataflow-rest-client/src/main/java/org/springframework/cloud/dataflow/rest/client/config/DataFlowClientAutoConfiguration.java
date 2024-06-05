/*
 * Copyright 2016-2019 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
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
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
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
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class to provide default beans for {@link DataFlowOperations} and {@link org.springframework.cloud.dataflow.rest.client.dsl.StreamBuilder} instances.
 * @author Vinicius Carvalho
 * @author Gunnar Hillert
 */
@AutoConfiguration
@EnableConfigurationProperties(DataFlowClientProperties.class)
public class DataFlowClientAutoConfiguration {

	private static Log logger = LogFactory.getLog(DataFlowClientAutoConfiguration.class);

	private static final String DEFAULT_REGISTRATION_ID = "default";

	@Autowired
	private DataFlowClientProperties properties;

	private RestTemplate restTemplate;

	@Autowired
	private @Nullable ClientRegistrationRepository clientRegistrations;

	@Autowired
	private @Nullable OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient;

	@Autowired
	private @Nullable OAuth2ClientProperties oauth2ClientProperties;

	public DataFlowClientAutoConfiguration(@Nullable RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(DataFlowOperations.class)
	public DataFlowOperations dataFlowOperations(@Nullable ObjectMapper mapper) throws Exception{
		RestTemplate template = DataFlowTemplate.prepareRestTemplate(restTemplate);
		final HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create(new URI(properties.getServerUri()))
				.skipTlsCertificateVerification(properties.isSkipSslValidation());

		if (StringUtils.hasText(this.properties.getAuthentication().getAccessToken())) {
			template.getInterceptors().add(new OAuth2AccessTokenProvidingClientHttpRequestInterceptor(this.properties.getAuthentication().getAccessToken()));
			logger.debug("Configured OAuth2 Access Token for accessing the Data Flow Server");
		}
		else if (StringUtils.hasText(this.properties.getAuthentication().getClientId())) {
			ClientRegistration clientRegistration = clientRegistrations.findByRegistrationId(DEFAULT_REGISTRATION_ID);
			template.getInterceptors().add(clientCredentialsTokenResolvingInterceptor(clientRegistration,
					clientRegistrations, this.properties.getAuthentication().getClientId()));
			logger.debug("Configured OAuth2 Client Credentials for accessing the Data Flow Server");
		}
		else if(!ObjectUtils.isEmpty(properties.getAuthentication().getBasic().getUsername()) &&
				!ObjectUtils.isEmpty(properties.getAuthentication().getBasic().getPassword())){
			httpClientConfigurer.basicAuthCredentials(properties.getAuthentication().getBasic().getUsername(),
					properties.getAuthentication().getBasic().getPassword());
			template.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());
		}
		else if (oauth2ClientProperties != null && !oauth2ClientProperties.getRegistration().isEmpty()
				&& StringUtils.hasText(properties.getAuthentication().getOauth2().getUsername())
				&& StringUtils.hasText(properties.getAuthentication().getOauth2().getPassword())) {
			ClientHttpRequestInterceptor bearerTokenResolvingInterceptor = bearerTokenResolvingInterceptor(
					oauth2ClientProperties, properties.getAuthentication().getOauth2().getUsername(),
					properties.getAuthentication().getOauth2().getPassword(),
					properties.getAuthentication().getOauth2().getClientRegistrationId());
			template.getInterceptors().add(bearerTokenResolvingInterceptor);
			logger.debug("Configured OAuth2 Bearer Token resolving for accessing the Data Flow Server");
		}
		else {
			logger.debug("Not configuring security for accessing the Data Flow Server");
		}

		return new DataFlowTemplate(new URI(properties.getServerUri()), template, mapper);
	}

	@Bean
	@ConditionalOnMissingBean(StreamBuilder.class)
	public StreamBuilder streamBuilder(DataFlowOperations dataFlowOperations){
		return Stream.builder(dataFlowOperations);
	}

	@ConditionalOnProperty(prefix = DataFlowPropertyKeys.PREFIX + "client.authentication", name = "client-id")
	@Configuration(proxyBeanMethods = false)
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
			ClientRegistration clientRegistration, ClientRegistrationRepository clientRegistrationRepository,
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

	private static final Authentication DEFAULT_PRINCIPAL = createAuthentication("dataflow-client-principal");

	private ClientRegistrationRepository shellClientRegistrationRepository(OAuth2ClientProperties properties) {
		var oauthClientPropsMapper = new OAuth2ClientPropertiesMapper(properties);
		return new InMemoryClientRegistrationRepository(oauthClientPropsMapper.asClientRegistrations().values().stream().toList());
	}

	private OAuth2AuthorizedClientService shellAuthorizedClientService(ClientRegistrationRepository shellClientRegistrationRepository) {
		return new InMemoryOAuth2AuthorizedClientService(shellClientRegistrationRepository);
	}

	private OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository shellClientRegistrationRepository,
			OAuth2AuthorizedClientService shellAuthorizedClientService) {
		AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
			shellClientRegistrationRepository, shellAuthorizedClientService);
		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
			.password()
			.refreshToken()
			.build();
		manager.setAuthorizedClientProvider(authorizedClientProvider);
		manager.setContextAttributesMapper(request -> {
			Map<String, Object> contextAttributes = new HashMap<>();
			request.getAttributes().forEach((k, v) -> {
				if (OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME.equals(k)
						|| OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME.equals(k)) {
					contextAttributes.put(k, v);
				}
			});
			return contextAttributes;
		});
		return manager;
	}

	private ClientHttpRequestInterceptor bearerTokenResolvingInterceptor(
			OAuth2ClientProperties properties, String username, String password, String clientRegistrationId) {
		ClientRegistrationRepository shellClientRegistrationRepository = shellClientRegistrationRepository(properties);
		OAuth2AuthorizedClientService shellAuthorizedClientService = shellAuthorizedClientService(shellClientRegistrationRepository);
		OAuth2AuthorizedClientManager authorizedClientManager = authorizedClientManager(
				shellClientRegistrationRepository, shellAuthorizedClientService);

		if (properties.getRegistration() != null && properties.getRegistration().size() == 1) {
			// if we have only one, use that
			clientRegistrationId = properties.getRegistration().entrySet().iterator().next().getKey();
		}

		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
				.principal(DEFAULT_PRINCIPAL)
				.attribute(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username)
				.attribute(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password)
				.build();

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
