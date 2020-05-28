/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.common.security.core.support.OAuth2AccessTokenProvidingClientHttpRequestInterceptor;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.support.OnOAuth2ClientCredentialsEnabled;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Configures the beans required for Connectivity to the Data Flow Server.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@EnableConfigurationProperties(ComposedTaskProperties.class)
public class DataFlowConfiguration {
	private static Log logger = LogFactory.getLog(org.springframework.cloud.dataflow.composedtaskrunner.DataFlowConfiguration.class);

	@Autowired
	private ComposedTaskProperties properties;

	@Bean
	public TaskOperations taskOperations(DataFlowOperations dataFlowOperations) {
		return dataFlowOperations.taskOperations();
	}

	/**
	 * @param clientRegistrations Can be null. Only required for Client Credentials Grant authentication
	 * @param clientCredentialsTokenResponseClient Can be null. Only required for Client Credentials Grant authentication
	 * @return DataFlowOperations
	 */
	@Bean
	public DataFlowOperations dataFlowOperations(
		@Autowired(required = false) ClientRegistrationRepository clientRegistrations,
		@Autowired(required = false) OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient) {

		final RestTemplate restTemplate = DataFlowTemplate.getDefaultDataflowRestTemplate();
		validateUsernamePassword(this.properties.getDataflowServerUsername(), this.properties.getDataflowServerPassword());

		HttpClientConfigurer clientHttpRequestFactoryBuilder = null;

		if (this.properties.getOauth2ClientCredentialsClientId() != null
				|| StringUtils.hasText(this.properties.getDataflowServerAccessToken())
				|| (StringUtils.hasText(this.properties.getDataflowServerUsername())
						&& StringUtils.hasText(this.properties.getDataflowServerPassword()))) {
			clientHttpRequestFactoryBuilder = HttpClientConfigurer.create(this.properties.getDataflowServerUri());
		}

		String accessTokenValue = null;

		if (this.properties.getOauth2ClientCredentialsClientId() != null) {
			final ClientRegistration clientRegistration = clientRegistrations.findByRegistrationId("default");
			final OAuth2ClientCredentialsGrantRequest grantRequest = new OAuth2ClientCredentialsGrantRequest(clientRegistration);
			final OAuth2AccessTokenResponse res = clientCredentialsTokenResponseClient.getTokenResponse(grantRequest);
			accessTokenValue = res.getAccessToken().getTokenValue();
			logger.debug("Configured OAuth2 Client Credentials for accessing the Data Flow Server");
		}
		else if (StringUtils.hasText(this.properties.getDataflowServerAccessToken())) {
			accessTokenValue = this.properties.getDataflowServerAccessToken();
			logger.debug("Configured OAuth2 Access Token for accessing the Data Flow Server");
		}
		else if (StringUtils.hasText(this.properties.getDataflowServerUsername())
				&& StringUtils.hasText(this.properties.getDataflowServerPassword())) {
			accessTokenValue = null;
			clientHttpRequestFactoryBuilder.basicAuthCredentials(properties.getDataflowServerUsername(), properties.getDataflowServerPassword());
			logger.debug("Configured basic security for accessing the Data Flow Server");
		}
		else {
			logger.debug("Not configuring basic security for accessing the Data Flow Server");
		}

		if (accessTokenValue != null) {
			restTemplate.getInterceptors().add(new OAuth2AccessTokenProvidingClientHttpRequestInterceptor(accessTokenValue));
		}

		if (clientHttpRequestFactoryBuilder != null) {
			restTemplate.setRequestFactory(clientHttpRequestFactoryBuilder.buildClientHttpRequestFactory());
		}

		return new DataFlowTemplate(this.properties.getDataflowServerUri(), restTemplate);
	}

	private void validateUsernamePassword(String userName, String password) {
		if (!StringUtils.isEmpty(password) && StringUtils.isEmpty(userName)) {
			throw new IllegalArgumentException("A password may be specified only together with a username");
		}
		if (StringUtils.isEmpty(password) && !StringUtils.isEmpty(userName)) {
			throw new IllegalArgumentException("A username may be specified only together with a password");
		}
	}

	@Configuration
	@Conditional(OnOAuth2ClientCredentialsEnabled.class)
	static class clientCredentialsConfiguration {
		@Bean
		public InMemoryClientRegistrationRepository clientRegistrationRepository(
				ComposedTaskProperties properties) {
			final ClientRegistration clientRegistration = ClientRegistration
					.withRegistrationId("default")
					.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
					.tokenUri(properties.getOauth2ClientCredentialsTokenUri())
					.clientId(properties.getOauth2ClientCredentialsClientId())
					.clientSecret(properties.getOauth2ClientCredentialsClientSecret())
					.scope(properties.getOauth2ClientCredentialsScopes())
					.build();
			return new InMemoryClientRegistrationRepository(clientRegistration);
		}

		@Bean
		OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient() {
			return new DefaultClientCredentialsTokenResponseClient();
		}
	}
}
