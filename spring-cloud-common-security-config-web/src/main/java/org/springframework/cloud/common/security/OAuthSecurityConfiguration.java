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
package org.springframework.cloud.common.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
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
 */
@Configuration
@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
@ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.ANY)
@EnableWebSecurity
@Conditional(OnOAuth2SecurityEnabled.class)
public class OAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OAuthSecurityConfiguration.class);

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
	protected OAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> oAuth2PasswordTokenResponseClient;

	@Autowired
	protected ClientRegistrationRepository clientRegistrationRepository;

	@Autowired(required = false)
	protected OpaqueTokenIntrospector opaqueTokenIntrospector;

	@Autowired
	protected OAuth2AuthorizedClientService oauth2AuthorizedClientService;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		final RequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(
				new BrowserDetectingContentNegotiationStrategy(),
				MediaType.TEXT_HTML);

		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(SecurityConfigUtils.BASIC_AUTH_REALM_NAME);
		basicAuthenticationEntryPoint.afterPropertiesSet();

		if (opaqueTokenIntrospector != null) {
			BasicAuthenticationFilter basicAuthenticationFilter = new BasicAuthenticationFilter(
					providerManager(), basicAuthenticationEntryPoint);
			http.addFilter(basicAuthenticationFilter);
		}

		this.authorizationProperties.getAuthenticatedPaths().add("/");
		this.authorizationProperties.getAuthenticatedPaths().add(dashboard("/**"));
		this.authorizationProperties.getAuthenticatedPaths().add(this.authorizationProperties.getDashboardUrl());
		this.authorizationProperties.getPermitAllPaths().add(this.authorizationProperties.getDashboardUrl());
		this.authorizationProperties.getPermitAllPaths().add(dashboard("/**"));
		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security =

				http.authorizeRequests()
						.antMatchers(this.authorizationProperties.getPermitAllPaths().toArray(new String[0]))
						.permitAll()
						.antMatchers(this.authorizationProperties.getAuthenticatedPaths().toArray(new String[0]))
						.authenticated();
		security = SecurityConfigUtils.configureSimpleSecurity(security, this.authorizationProperties);
		security.anyRequest().denyAll();


		http.httpBasic().and()
				.logout()
				.logoutSuccessHandler(logoutSuccessHandler())
				.and().csrf().disable()
				.exceptionHandling()
				.defaultAuthenticationEntryPointFor(
						new LoginUrlAuthenticationEntryPoint(this.authorizationProperties.getLoginProcessingUrl()),
						textHtmlMatcher)
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, AnyRequestMatcher.INSTANCE);

		http.oauth2Login().userInfoEndpoint()
			.userService(this.plainOauth2UserService())
			.oidcUserService(this.oidcUserService());

		if (opaqueTokenIntrospector != null) {
			http.oauth2ResourceServer()
				.opaqueToken()
					.introspector(opaqueTokenIntrospector());
		} else if (oAuth2ResourceServerProperties.getJwt().getJwkSetUri() != null) {
			http.oauth2ResourceServer()
				.jwt()
					.jwtAuthenticationConverter(grantedAuthoritiesExtractor());
		}

		this.securityStateBean.setAuthenticationEnabled(true);
	}

	protected Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
		String providerId = calculateDefaultProviderId();
		ProviderRoleMapping providerRoleMapping = authorizationProperties.getProviderRoleMappings().get(providerId);

		JwtAuthenticationConverter jwtAuthenticationConverter =
				new JwtAuthenticationConverter();

		MappingJwtGrantedAuthoritiesConverter converter = new MappingJwtGrantedAuthoritiesConverter();
		converter.setAuthorityPrefix("");
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
		if (providerRoleMapping != null) {
			converter.setAuthoritiesMapping(providerRoleMapping.getRoleMappings());
		}
		return jwtAuthenticationConverter;
	}


	@Bean
	@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.opaquetoken", value = "introspection-uri")
	protected OpaqueTokenIntrospector opaqueTokenIntrospector() {
		return new CustomAuthoritiesOpaqueTokenIntrospector(
				this.oAuth2ResourceServerProperties.getOpaquetoken().getIntrospectionUri(),
				this.oAuth2ResourceServerProperties.getOpaquetoken().getClientId(),
				this.oAuth2ResourceServerProperties.getOpaquetoken().getClientSecret(),
				authorityMapper());
	}

	@Bean
	protected OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
		return new CustomOAuth2OidcUserService(authorityMapper());
	}

	@Bean
	protected OAuth2UserService<OAuth2UserRequest, OAuth2User> plainOauth2UserService() {
		return new CustomPlainOAuth2UserService(authorityMapper());
	}

	@Bean
	public OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientRepository authorizedClientRepository) {

		OAuth2AuthorizedClientProvider authorizedClientProvider =
				OAuth2AuthorizedClientProviderBuilder.builder()
						.authorizationCode()
						.refreshToken()
						.clientCredentials()
						.password()
						.build();

		DefaultOAuth2AuthorizedClientManager authorizedClientManager =
				new DefaultOAuth2AuthorizedClientManager(
						clientRegistrationRepository, authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		return authorizedClientManager;
	}

	@Bean
	WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
				new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
		oauth2Client.setDefaultOAuth2AuthorizedClient(true);
		return WebClient.builder()
				.apply(oauth2Client.oauth2Configuration())
				.build();
	}

	@Bean
	public AuthoritiesMapper authorityMapper() {
		AuthoritiesMapper authorityMapper;

		if (StringUtils.isEmpty(authorizationProperties.getExternalAuthoritiesUrl())) {
			authorityMapper = new DefaultAuthoritiesMapper(
					authorizationProperties.getProviderRoleMappings(),
					this.calculateDefaultProviderId());
		}
		else {
			authorityMapper = new ExternalOauth2ResourceAuthoritiesMapper(
				URI.create(authorizationProperties.getExternalAuthoritiesUrl()));
		}
		return authorityMapper;
	}

	@Bean
	LogoutSuccessHandler logoutSuccessHandler() {
		final AccessTokenClearingLogoutSuccessHandler logoutSuccessHandler =
				new AccessTokenClearingLogoutSuccessHandler(this.oauth2TokenUtilsService());
		logoutSuccessHandler.setDefaultTargetUrl(dashboard("/logout-success-oauth.html"));
		return logoutSuccessHandler;
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.opaquetoken", value = "introspection-uri")
	protected AuthenticationProvider authenticationProvider() {
		return new ManualOAuthAuthenticationProvider(
			this.oAuth2PasswordTokenResponseClient,
			this.clientRegistrationRepository,
			this.opaqueTokenIntrospector,
			this.calculateDefaultProviderId());

	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.opaquetoken", value = "introspection-uri")
	protected ProviderManager providerManager() {
		List<AuthenticationProvider> providers = new ArrayList<>();
		providers.add(authenticationProvider());
		ProviderManager providerManager = new ProviderManager(providers);
		return providerManager;
	}

	@Bean
	protected OAuth2TokenUtilsService oauth2TokenUtilsService() {
		return new DefaultOAuth2TokenUtilsService(this.oauth2AuthorizedClientService);
	}

	@EventListener
	public void handleOAuth2AuthenticationFailureEvent(
			AbstractAuthenticationFailureEvent authenticationFailureEvent) {
		logger.warn("An authentication failure event occurred while accessing a REST resource that requires authentication.",
				authenticationFailureEvent.getException());
	}

	protected String dashboard(String path) {
		return this.authorizationProperties.getDashboardUrl() + path;
	}

	protected static class BrowserDetectingContentNegotiationStrategy extends HeaderContentNegotiationStrategy {

		@Override
		public List<MediaType> resolveMediaTypes(NativeWebRequest request)
				throws HttpMediaTypeNotAcceptableException {
			final List<MediaType> supportedMediaTypes = super.resolveMediaTypes(request);

			final String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
			if (userAgent != null && userAgent.contains("Mozilla/5.0")
					&& !supportedMediaTypes.contains(MediaType.APPLICATION_JSON)) {

				return Collections.singletonList(MediaType.TEXT_HTML);
			}
			return Collections.singletonList(MediaType.APPLICATION_JSON);
		}
	}

	@Bean
	OAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> oAuth2PasswordTokenResponseClient() {
		return new DefaultPasswordTokenResponseClient();
	}

	private String calculateDefaultProviderId() {
		if (this.authorizationProperties.getDefaultProviderId() != null) {
			return this.authorizationProperties.getDefaultProviderId();
		}
		else if (this.oauth2ClientProperties.getRegistration().size() == 1) {
			return this.oauth2ClientProperties.getRegistration().entrySet().iterator().next().getKey();
		}
		else if (this.oauth2ClientProperties.getRegistration().size() > 1
				&& StringUtils.isEmpty(this.authorizationProperties.getDefaultProviderId())) {
			throw new IllegalStateException("defaultProviderId must be set if more than 1 Registration is provided.");
		}
		else {
			throw new IllegalStateException("Unable to retrieve default provider id.");
		}
	}

}
