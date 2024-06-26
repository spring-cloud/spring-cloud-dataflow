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

package org.springframework.cloud.skipper.server.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.common.security.AuthorizationProperties;
import org.springframework.cloud.common.security.OAuthSecurityConfiguration;
import org.springframework.cloud.common.security.support.OnOAuth2SecurityEnabled;
import org.springframework.cloud.common.security.support.SecurityConfigUtils;
import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@Configuration(proxyBeanMethods = false)
@Conditional(OnOAuth2SecurityEnabled.class)
public class SkipperOAuthSecurityConfiguration extends OAuthSecurityConfiguration {

	@Autowired
	private SecurityStateBean securityStateBean;

	@Autowired
	private AuthorizationProperties authorizationProperties;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(SecurityConfigUtils.BASIC_AUTH_REALM_NAME);
		basicAuthenticationEntryPoint.afterPropertiesSet();

		if (opaqueTokenIntrospector != null) {
			BasicAuthenticationFilter basicAuthenticationFilter = new BasicAuthenticationFilter(
					getProviderManager(),
					basicAuthenticationEntryPoint
			);
			http.addFilter(basicAuthenticationFilter);
		}

		getAuthorizationProperties().getAnonymousPaths().add(authorizationProperties.getLoginUrl());

		// All paths should be available only for authenticated users
		getAuthorizationProperties().getAuthenticatedPaths().add("/");
		getAuthorizationProperties().getAuthenticatedPaths().add(this.authorizationProperties.getDashboardUrl());
		getAuthorizationProperties().getAuthenticatedPaths().add(dashboard(authorizationProperties, "/**"));

		// Permit for all users as the visibility is managed through roles
		getAuthorizationProperties().getPermitAllPaths().add(this.authorizationProperties.getDashboardUrl());
		getAuthorizationProperties().getPermitAllPaths().add(dashboard(authorizationProperties, "/**"));

		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security;

		if (AuthorizationProperties.FRONTEND_LOGIN_URL.equals(this.authorizationProperties.getLoginUrl())) {
			security =
					http.authorizeRequests()
							.antMatchers(this.authorizationProperties.getAuthenticatedPaths().toArray(new String[0]))
							.authenticated()
							.antMatchers(this.authorizationProperties.getPermitAllPaths().toArray(new String[0]))
							.permitAll()
							.antMatchers(this.authorizationProperties.getAnonymousPaths().toArray(new String[0]))
							.anonymous();

		} else {
			security =
					http.authorizeRequests()
							.antMatchers(this.authorizationProperties.getPermitAllPaths().toArray(new String[0]))
							.permitAll()
							.antMatchers(this.authorizationProperties.getAuthenticatedPaths().toArray(new String[0]))
							.authenticated()
							.antMatchers(this.authorizationProperties.getAnonymousPaths().toArray(new String[0]))
							.anonymous();
		}

		security = SecurityConfigUtils.configureSimpleSecurity(security, getAuthorizationProperties());
		security.anyRequest().denyAll();

		http.httpBasic().and()
				.logout()
				.logoutSuccessUrl(getAuthorizationProperties().getLogoutSuccessUrl())
				.and().csrf().disable()
				.exceptionHandling()
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, new AntPathRequestMatcher("/api/**"))
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, new AntPathRequestMatcher("/actuator/**"));

		if (getOpaqueTokenIntrospector() != null) {
			http.oauth2ResourceServer()
					.opaqueToken()
					.introspector(getOpaqueTokenIntrospector());
		}
		else if (getoAuth2ResourceServerProperties().getJwt().getJwkSetUri() != null) {
			http.oauth2ResourceServer()
					.jwt()
					.jwtAuthenticationConverter(grantedAuthoritiesExtractor());
		}

		getSecurityStateBean().setAuthenticationEnabled(true);
	}
}
