/*
 * Copyright 2016-2024 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.common.security.support.AccessTokenClearingLogoutSuccessHandler;
import org.springframework.cloud.common.security.support.AuthoritiesMapper;
import org.springframework.cloud.common.security.support.CustomAuthoritiesOpaqueTokenIntrospector;
import org.springframework.cloud.common.security.support.CustomOAuth2OidcUserService;
import org.springframework.cloud.common.security.support.CustomPlainOAuth2UserService;
import org.springframework.cloud.common.security.support.DefaultAuthoritiesMapper;
import org.springframework.cloud.common.security.support.DefaultOAuth2TokenUtilsService;
import org.springframework.cloud.common.security.support.ExternalOauth2ResourceAuthoritiesMapper;
import org.springframework.cloud.common.security.support.MappingJwtGrantedAuthoritiesConverter;
import org.springframework.cloud.common.security.support.OnOAuth2SecurityEnabled;
import org.springframework.cloud.common.security.support.SecurityConfigUtils;
import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultPasswordTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2PasswordGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@Conditional(OnOAuth2SecurityEnabled.class)
public class OAuthSecurityConfiguration {

	@Autowired
	protected OAuth2ClientProperties oauth2ClientProperties;

	@Autowired
	protected SecurityStateBean securityStateBean;

	@Autowired
	protected AuthorizationProperties authorizationProperties;

	@Autowired
	protected OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

	@Autowired(required = false)
	protected OpaqueTokenIntrospector opaqueTokenIntrospector;

	@Autowired
	protected OAuth2UserService<OAuth2UserRequest, OAuth2User> plainOauth2UserService;

	@Autowired
	protected OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

	@Autowired
	protected LogoutSuccessHandler logoutSuccessHandler;

	@Autowired(required = false)
	protected ProviderManager providerManager;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(SecurityConfigUtils.BASIC_AUTH_REALM_NAME);
		basicAuthenticationEntryPoint.afterPropertiesSet();

		if (opaqueTokenIntrospector != null) {
			BasicAuthenticationFilter basicAuthenticationFilter = new BasicAuthenticationFilter(
					providerManager, basicAuthenticationEntryPoint);
			http.addFilter(basicAuthenticationFilter);
		}

		http.authorizeHttpRequests(auth -> {
			auth.anyRequest().authenticated();
		});

		http.httpBasic(auth -> {
		});

		http.logout(auth -> {
			auth.logoutSuccessHandler(logoutSuccessHandler);
		});

		http.csrf(auth -> {
			auth.disable();
		});

		http.exceptionHandling(auth -> {
			auth.defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
					new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));
			RequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(
					new BrowserDetectingContentNegotiationStrategy(), MediaType.TEXT_HTML);
			auth.defaultAuthenticationEntryPointFor(
					new LoginUrlAuthenticationEntryPoint(this.authorizationProperties.getLoginProcessingUrl()),
					textHtmlMatcher);
			auth.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, AnyRequestMatcher.INSTANCE);
		});

		http.oauth2Login(auth -> {
			auth.userInfoEndpoint(customizer -> {
				customizer.userService(plainOauth2UserService).oidcUserService(oidcUserService);
			});
		});

		http.oauth2ResourceServer(resourceserver -> {
			if (opaqueTokenIntrospector != null) {
				resourceserver.opaqueToken(opaqueToken -> {
					opaqueToken.introspector(opaqueTokenIntrospector);
				});
			}
			else if (oAuth2ResourceServerProperties.getJwt().getJwkSetUri() != null) {
				resourceserver.jwt(jwt -> {
					jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor());
				});
			}
		});

		securityStateBean.setAuthenticationEnabled(true);

		return http.build();
	}

	private static String calculateDefaultProviderId(AuthorizationProperties authorizationProperties, OAuth2ClientProperties oauth2ClientProperties) {
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
	protected static class LogoutSuccessHandlerConfig {
		@Bean
		protected LogoutSuccessHandler logoutSuccessHandler(AuthorizationProperties authorizationProperties,
				OAuth2TokenUtilsService oauth2TokenUtilsService) {
			AccessTokenClearingLogoutSuccessHandler logoutSuccessHandler =
					new AccessTokenClearingLogoutSuccessHandler(oauth2TokenUtilsService);
			logoutSuccessHandler.setDefaultTargetUrl(dashboard(authorizationProperties, "/logout-success-oauth.html"));
			return logoutSuccessHandler;
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

	protected static String dashboard(AuthorizationProperties authorizationProperties, String path) {
		return authorizationProperties.getDashboardUrl() + path;
	}

	protected Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
		String providerId = calculateDefaultProviderId(authorizationProperties, oauth2ClientProperties);
		ProviderRoleMapping providerRoleMapping = authorizationProperties.getProviderRoleMappings()
				.get(providerId);

		JwtAuthenticationConverter jwtAuthenticationConverter =
				new JwtAuthenticationConverter();

		MappingJwtGrantedAuthoritiesConverter converter = new MappingJwtGrantedAuthoritiesConverter();
		converter.setAuthorityPrefix("");
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
		if (providerRoleMapping != null) {
			converter.setAuthoritiesMapping(providerRoleMapping.getRoleMappings());
			converter.setGroupAuthoritiesMapping(providerRoleMapping.getGroupMappings());
			if (StringUtils.hasText(providerRoleMapping.getPrincipalClaimName())) {
				jwtAuthenticationConverter.setPrincipalClaimName(providerRoleMapping.getPrincipalClaimName());
			}
		}
		return jwtAuthenticationConverter;
	}

	private static class BrowserDetectingContentNegotiationStrategy extends HeaderContentNegotiationStrategy {
		@Override
		public List<MediaType> resolveMediaTypes(NativeWebRequest request) throws HttpMediaTypeNotAcceptableException {
			final List<MediaType> supportedMediaTypes = super.resolveMediaTypes(request);
			final String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
			if (userAgent != null && userAgent.contains("Mozilla/5.0")
					&& !supportedMediaTypes.contains(MediaType.APPLICATION_JSON)) {
				return Collections.singletonList(MediaType.TEXT_HTML);
			}
			return Collections.singletonList(MediaType.APPLICATION_JSON);
		}
	}

}
