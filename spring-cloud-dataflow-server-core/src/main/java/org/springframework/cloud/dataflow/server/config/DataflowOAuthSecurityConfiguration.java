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

package org.springframework.cloud.dataflow.server.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.cloud.common.security.AuthorizationProperties;
import org.springframework.cloud.common.security.OAuthClientConfiguration;
import org.springframework.cloud.common.security.ProviderRoleMapping;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.common.security.support.AccessTokenClearingLogoutSuccessHandler;
import org.springframework.cloud.common.security.support.MappingJwtGrantedAuthoritiesConverter;
import org.springframework.cloud.common.security.support.OnOAuth2SecurityEnabled;
import org.springframework.cloud.common.security.support.SecurityConfigUtils;
import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
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

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
@Configuration
@Conditional(OnOAuth2SecurityEnabled.class)
@Import({ OAuthClientConfiguration.class })
@EnableWebSecurity
public class DataflowOAuthSecurityConfiguration {

	private final OpaqueTokenIntrospector opaqueTokenIntrospector;
	private final AuthenticationManager authenticationManager;
	private final AuthorizationProperties authorizationProperties;
	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> plainOauth2UserService;
	private final OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;
	private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;
	private final OAuth2ClientProperties oauth2ClientProperties;
	private final SecurityStateBean securityStateBean;
	private final OAuth2TokenUtilsService oauth2TokenUtilsService;

	public DataflowOAuthSecurityConfiguration(ObjectProvider<OpaqueTokenIntrospector> opaqueTokenIntrospector,
			ObjectProvider<AuthenticationManager> authenticationManager,
			ObjectProvider<AuthorizationProperties> authorizationProperties,
			ObjectProvider<OAuth2UserService<OAuth2UserRequest, OAuth2User>> plainOauth2UserService,
			ObjectProvider<OAuth2UserService<OidcUserRequest, OidcUser>> oidcUserService,
			ObjectProvider<OAuth2ResourceServerProperties> oAuth2ResourceServerProperties,
			ObjectProvider<OAuth2ClientProperties> oauth2ClientProperties,
			ObjectProvider<SecurityStateBean> securityStateBean,
			ObjectProvider<OAuth2TokenUtilsService> oauth2TokenUtilsService
			) {
		this.opaqueTokenIntrospector = opaqueTokenIntrospector.getIfAvailable();
		this.authenticationManager = authenticationManager.getIfAvailable();
		this.authorizationProperties = authorizationProperties.getIfAvailable();
		this.plainOauth2UserService = plainOauth2UserService.getIfAvailable();
		this.oidcUserService = oidcUserService.getIfAvailable();
		this.oAuth2ResourceServerProperties = oAuth2ResourceServerProperties.getIfAvailable();
		this.oauth2ClientProperties = oauth2ClientProperties.getIfAvailable();
		this.securityStateBean = securityStateBean.getIfAvailable();
		this.oauth2TokenUtilsService = oauth2TokenUtilsService.getIfAvailable();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(SecurityConfigUtils.BASIC_AUTH_REALM_NAME);
		basicAuthenticationEntryPoint.afterPropertiesSet();

		if (opaqueTokenIntrospector != null) {
			BasicAuthenticationFilter basicAuthenticationFilter = new BasicAuthenticationFilter(
					authenticationManager, basicAuthenticationEntryPoint);
			http.addFilter(basicAuthenticationFilter);
		}

		List<String> authenticatedPaths = new ArrayList<>(authorizationProperties.getAuthenticatedPaths());
		authenticatedPaths.add("/");
		authenticatedPaths.add(dashboard(authorizationProperties, "/**"));
		authenticatedPaths.add(authorizationProperties.getDashboardUrl());

		List<String> permitAllPaths = new ArrayList<>(authorizationProperties.getPermitAllPaths());
		permitAllPaths.add(this.authorizationProperties.getDashboardUrl());
		permitAllPaths.add(dashboard(authorizationProperties, "/**"));

		http.authorizeHttpRequests(auth -> {
			auth.requestMatchers(permitAllPaths.toArray(new String[0])).permitAll();
			auth.requestMatchers(authenticatedPaths.toArray(new String[0])).authenticated();
			SecurityConfigUtils.configureSimpleSecurity(auth, authorizationProperties);
		});

		http.httpBasic(Customizer.withDefaults());

		http.logout(auth -> {
			auth.logoutSuccessHandler(logoutSuccessHandler(authorizationProperties, oauth2TokenUtilsService));
		});

		http.csrf(AbstractHttpConfigurer::disable);

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

	private static String dashboard(AuthorizationProperties authorizationProperties, String path) {
		return authorizationProperties.getDashboardUrl() + path;
	}

	private LogoutSuccessHandler logoutSuccessHandler(AuthorizationProperties authorizationProperties,
			OAuth2TokenUtilsService oauth2TokenUtilsService) {
		AccessTokenClearingLogoutSuccessHandler logoutSuccessHandler =
				new AccessTokenClearingLogoutSuccessHandler(oauth2TokenUtilsService);
		logoutSuccessHandler.setDefaultTargetUrl(dashboard(authorizationProperties, "/logout-success-oauth.html"));
		return logoutSuccessHandler;
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

	private Converter<Jwt, AbstractAuthenticationToken> grantedAuthoritiesExtractor() {
		String providerId = OAuthClientConfiguration.calculateDefaultProviderId(authorizationProperties, oauth2ClientProperties);
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

}
