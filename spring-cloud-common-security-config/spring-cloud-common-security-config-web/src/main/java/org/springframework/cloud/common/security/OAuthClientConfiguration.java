/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.cloud.common.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.common.security.support.AuthoritiesMapper;
import org.springframework.cloud.common.security.support.CustomAuthoritiesOpaqueTokenIntrospector;
import org.springframework.cloud.common.security.support.CustomOAuth2OidcUserService;
import org.springframework.cloud.common.security.support.CustomPlainOAuth2UserService;
import org.springframework.cloud.common.security.support.DefaultAuthoritiesMapper;
import org.springframework.cloud.common.security.support.DefaultOAuth2TokenUtilsService;
import org.springframework.cloud.common.security.support.ExternalOauth2ResourceAuthoritiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultPasswordTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2PasswordGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration(proxyBeanMethods = false)
public class OAuthClientConfiguration {

	@Configuration(proxyBeanMethods = false)
	protected static class OAuth2AccessTokenResponseClientConfig {
		@Bean
		OAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> oAuth2PasswordTokenResponseClient() {
			return new DefaultPasswordTokenResponseClient();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.opaquetoken", value = "introspection-uri")
	protected static class AuthenticationProviderConfig {

		protected OpaqueTokenIntrospector opaqueTokenIntrospector;

		@Autowired(required = false)
		public void setOpaqueTokenIntrospector(OpaqueTokenIntrospector opaqueTokenIntrospector) {
			this.opaqueTokenIntrospector = opaqueTokenIntrospector;
		}

		@Bean
		protected AuthenticationProvider authenticationProvider(
				OAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> oAuth2PasswordTokenResponseClient,
				ClientRegistrationRepository clientRegistrationRepository,
				AuthorizationProperties authorizationProperties,
				OAuth2ClientProperties oauth2ClientProperties) {
			return new ManualOAuthAuthenticationProvider(
					oAuth2PasswordTokenResponseClient,
					clientRegistrationRepository,
					this.opaqueTokenIntrospector,
					calculateDefaultProviderId(authorizationProperties, oauth2ClientProperties));

		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.opaquetoken", value = "introspection-uri")
	protected static class ProviderManagerConfig {
		private AuthenticationProvider authenticationProvider;

		protected AuthenticationProvider getAuthenticationProvider() {
			return authenticationProvider;
		}

		@Autowired(required = false)
		protected void setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
			this.authenticationProvider = authenticationProvider;
		}

		@Bean
		protected ProviderManager providerManager() {
			List<AuthenticationProvider> providers = new ArrayList<>();
			providers.add(authenticationProvider);
			return new ProviderManager(providers);
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class OAuth2TokenUtilsServiceConfig {
		@Bean
		protected OAuth2TokenUtilsService oauth2TokenUtilsService(OAuth2AuthorizedClientService oauth2AuthorizedClientService) {
			return new DefaultOAuth2TokenUtilsService(oauth2AuthorizedClientService);
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class AuthoritiesMapperConfig {

		@Bean
		protected AuthoritiesMapper authorityMapper(AuthorizationProperties authorizationProperties,
				OAuth2ClientProperties oAuth2ClientProperties) {
			AuthoritiesMapper authorityMapper;
			if (!StringUtils.hasText(authorizationProperties.getExternalAuthoritiesUrl())) {
				authorityMapper = new DefaultAuthoritiesMapper(
						authorizationProperties.getProviderRoleMappings(),
						calculateDefaultProviderId(authorizationProperties, oAuth2ClientProperties));
			} else {
				authorityMapper = new ExternalOauth2ResourceAuthoritiesMapper(
						URI.create(authorizationProperties.getExternalAuthoritiesUrl()));
			}
			return authorityMapper;
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class OidcUserServiceConfig {

		@Bean
		protected OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(AuthoritiesMapper authoritiesMapper) {
			return new CustomOAuth2OidcUserService(authoritiesMapper);
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class PlainOauth2UserServiceConfig {

		@Bean
		protected OAuth2UserService<OAuth2UserRequest, OAuth2User> plainOauth2UserService(
				AuthoritiesMapper authoritiesMapper) {
			return new CustomPlainOAuth2UserService(authoritiesMapper);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.opaquetoken", value = "introspection-uri")
	protected static class OpaqueTokenIntrospectorConfig {
		@Bean
		protected OpaqueTokenIntrospector opaqueTokenIntrospector(OAuth2ResourceServerProperties oAuth2ResourceServerProperties,
				AuthoritiesMapper authoritiesMapper) {
			return new CustomAuthoritiesOpaqueTokenIntrospector(
					oAuth2ResourceServerProperties.getOpaquetoken().getIntrospectionUri(),
					oAuth2ResourceServerProperties.getOpaquetoken().getClientId(),
					oAuth2ResourceServerProperties.getOpaquetoken().getClientSecret(),
					authoritiesMapper);
		}
	}

	public static String calculateDefaultProviderId(AuthorizationProperties authorizationProperties, OAuth2ClientProperties oauth2ClientProperties) {
		if (authorizationProperties.getDefaultProviderId() != null) {
			return authorizationProperties.getDefaultProviderId();
		}
		else if (oauth2ClientProperties.getRegistration().size() == 1) {
			return oauth2ClientProperties.getRegistration().entrySet().iterator().next()
					.getKey();
		}
		else if (oauth2ClientProperties.getRegistration().size() > 1
				&& !StringUtils.hasText(authorizationProperties.getDefaultProviderId())) {
			throw new IllegalStateException("defaultProviderId must be set if more than 1 Registration is provided.");
		}
		else {
			throw new IllegalStateException("Unable to retrieve default provider id.");
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class WebClientConfig {

		@Bean
		protected WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
			ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
					new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
			oauth2Client.setDefaultOAuth2AuthorizedClient(true);
			return WebClient.builder()
					.apply(oauth2Client.oauth2Configuration())
					.build();
		}
	}


}
