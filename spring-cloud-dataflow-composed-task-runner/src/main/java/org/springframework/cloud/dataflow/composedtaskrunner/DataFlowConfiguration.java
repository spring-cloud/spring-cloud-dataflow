/*
 * Copyright 2017-2021 the original author or authors.
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

import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.support.OnOAuth2ClientCredentialsEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Configures the beans required for Connectivity to the Data Flow Server.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@Conditional(OnOAuth2ClientCredentialsEnabled.class)
public class DataFlowConfiguration {
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
				.clientAuthenticationMethod(properties.getOauth2ClientCredentialsClientAuthenticationMethod())
				.build();
		return new InMemoryClientRegistrationRepository(clientRegistration);
	}

	@Bean
	OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient() {
		return new DefaultClientCredentialsTokenResponseClient();
	}
}
