/*
 * Copyright 2016-2022 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultPasswordTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2PasswordGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
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
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@Configuration(proxyBeanMethods = false)
// SCDF 3.0 Migration - Need to re add this later with a different class or bean.
// @ConditionalOnClass(WebSecurityConfigurerAdapter.class)
// @ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.ANY)
@EnableWebSecurity
@Conditional(OnOAuth2SecurityEnabled.class)
@Import({
		OAuthSecurityConfiguration.OAuth2AccessTokenResponseClientConfig.class,
		OAuthSecurityConfiguration.OAuth2AuthenticationFailureEventConfig.class,
		OAuthSecurityConfiguration.OpaqueTokenIntrospectorConfig.class,
		OAuthSecurityConfiguration.OidcUserServiceConfig.class,
		OAuthSecurityConfiguration.PlainOauth2UserServiceConfig.class,
		OAuthSecurityConfiguration.WebClientConfig.class,
		OAuthSecurityConfiguration.AuthoritiesMapperConfig.class,
		OAuthSecurityConfiguration.OAuth2TokenUtilsServiceConfig.class,
		OAuthSecurityConfiguration.LogoutSuccessHandlerConfig.class,
		OAuthSecurityConfiguration.ProviderManagerConfig.class,
		OAuthSecurityConfiguration.AuthenticationProviderConfig.class
})
public class OAuthSecurityConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(OAuthSecurityConfiguration.class);

	@Autowired
	protected OAuth2ClientProperties oauth2ClientProperties;

	@Autowired
	protected SecurityStateBean securityStateBean;

	@Autowired
	protected SecurityProperties securityProperties;

	@Autowired
	protected ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	protected AuthorizationProperties authorizationProperties;

	@Autowired
	protected OAuth2ResourceServerProperties oAuth2ResourceServerProperties;

	@Autowired
	protected OAuth2UserService<OAuth2UserRequest, OAuth2User> plainOauth2UserService;

	@Autowired
	protected OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

	@Autowired
	protected LogoutSuccessHandler logoutSuccessHandler;

	protected OpaqueTokenIntrospector opaqueTokenIntrospector;

	protected ProviderManager providerManager;

	public AuthorizationProperties getAuthorizationProperties() {
		return authorizationProperties;
	}

	public void setAuthorizationProperties(AuthorizationProperties authorizationProperties) {
		this.authorizationProperties = authorizationProperties;
	}

	public OpaqueTokenIntrospector getOpaqueTokenIntrospector() {
		return opaqueTokenIntrospector;
	}

	@Autowired(required = false)
	public void setOpaqueTokenIntrospector(OpaqueTokenIntrospector opaqueTokenIntrospector) {
		this.opaqueTokenIntrospector = opaqueTokenIntrospector;
	}

	public ProviderManager getProviderManager() {
		return providerManager;
	}

	@Autowired(required = false)
	public void setProviderManager(ProviderManager providerManager) {
		this.providerManager = providerManager;
	}

	public OAuth2ResourceServerProperties getoAuth2ResourceServerProperties() {
		return oAuth2ResourceServerProperties;
	}

	public void setoAuth2ResourceServerProperties(OAuth2ResourceServerProperties oAuth2ResourceServerProperties) {
		this.oAuth2ResourceServerProperties = oAuth2ResourceServerProperties;
	}

	public SecurityStateBean getSecurityStateBean() {
		return securityStateBean;
	}

	public void setSecurityStateBean(SecurityStateBean securityStateBean) {
		this.securityStateBean = securityStateBean;
	}

	protected HttpBasicConfigurer configure(HttpSecurity http) throws Exception {

		final RequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(
				new BrowserDetectingContentNegotiationStrategy(),
				MediaType.TEXT_HTML);

		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(SecurityConfigUtils.BASIC_AUTH_REALM_NAME);
		basicAuthenticationEntryPoint.afterPropertiesSet();

		if (opaqueTokenIntrospector != null) {
			BasicAuthenticationFilter basicAuthenticationFilter = new BasicAuthenticationFilter(
					providerManager, basicAuthenticationEntryPoint);
			http.addFilter(basicAuthenticationFilter);
		}

		this.authorizationProperties.getAuthenticatedPaths().add("/");
		this.authorizationProperties.getAuthenticatedPaths()
				.add(dashboard(authorizationProperties, "/**"));
		this.authorizationProperties.getAuthenticatedPaths()
				.add(this.authorizationProperties.getDashboardUrl());
		this.authorizationProperties.getPermitAllPaths()
				.add(this.authorizationProperties.getDashboardUrl());
		this.authorizationProperties.getPermitAllPaths()
				.add(dashboard(authorizationProperties, "/**"));
		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security =

				http.authorizeRequests()
						.requestMatchers(this.authorizationProperties.getPermitAllPaths()
								.toArray(new String[0]))
						.permitAll()
						.requestMatchers(this.authorizationProperties.getAuthenticatedPaths()
								.toArray(new String[0]))
						.authenticated();
		security = SecurityConfigUtils.configureSimpleSecurity(security, this.authorizationProperties);
		security.anyRequest().denyAll();


		ExceptionHandlingConfigurer configurer = http.httpBasic().and()
				.logout()
				.logoutSuccessHandler(logoutSuccessHandler)
				.and().csrf().disable()
				.exceptionHandling()
				// for UI not to send basic auth header
				.defaultAuthenticationEntryPointFor(
						new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
						new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"))
				.defaultAuthenticationEntryPointFor(
						new LoginUrlAuthenticationEntryPoint(this.authorizationProperties.getLoginProcessingUrl()),
						textHtmlMatcher)
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, AnyRequestMatcher.INSTANCE);

		http.oauth2Login().userInfoEndpoint()
				.userService(this.plainOauth2UserService)
				.oidcUserService(this.oidcUserService);

		if (opaqueTokenIntrospector != null) {
			http.oauth2ResourceServer()
					.opaqueToken()
					.introspector(opaqueTokenIntrospector);
		}
		else if (oAuth2ResourceServerProperties.getJwt().getJwkSetUri() != null) {
			http.oauth2ResourceServer()
					.jwt()
					.jwtAuthenticationConverter(grantedAuthoritiesExtractor());
		}

		this.securityStateBean.setAuthenticationEnabled(true);
		return http.getConfigurer(HttpBasicConfigurer.class);
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
		protected OAuth2UserService<OAuth2UserRequest, OAuth2User> plainOauth2UserService(AuthoritiesMapper authoritiesMapper) {
			return new CustomPlainOAuth2UserService(authoritiesMapper);
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class OAuth2AuthorizedClientManagerConfig {
		@Bean
		protected OAuth2AuthorizedClientManager authorizedClientManager(
				ClientRegistrationRepository clientRegistrationRepository,
				OAuth2AuthorizedClientRepository authorizedClientRepository) {
			OAuth2AuthorizedClientProvider authorizedClientProvider =
					OAuth2AuthorizedClientProviderBuilder.builder()
							.authorizationCode()
							.refreshToken()
							.clientCredentials()
							.password()
							.build();
			DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
					clientRegistrationRepository, authorizedClientRepository);
			authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
			return authorizedClientManager;
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
			}
			else {
				authorityMapper = new ExternalOauth2ResourceAuthoritiesMapper(
						URI.create(authorizationProperties.getExternalAuthoritiesUrl()));
			}
			return authorityMapper;
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
	protected static class OAuth2AuthenticationFailureEventConfig {
		@EventListener
		public void handleOAuth2AuthenticationFailureEvent(
				AbstractAuthenticationFailureEvent authenticationFailureEvent) {
			logger.warn("An authentication failure event occurred while accessing a REST resource that requires authentication.",
					authenticationFailureEvent.getException());
		}
	}

	@Configuration(proxyBeanMethods = false)
	protected static class OAuth2AccessTokenResponseClientConfig {
		@Bean
		OAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> oAuth2PasswordTokenResponseClient() {
			return new DefaultPasswordTokenResponseClient();
		}
	}

	protected static class BrowserDetectingContentNegotiationStrategy extends HeaderContentNegotiationStrategy {
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
