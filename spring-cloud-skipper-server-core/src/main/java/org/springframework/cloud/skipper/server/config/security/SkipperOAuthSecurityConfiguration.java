/*
 * Copyright 2016-2017 the original author or authors.
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

import javax.servlet.Filter;

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
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@EnableOAuth2Client
@Configuration
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
		final Filter oauthFilter = oauthFilter();
		final BasicAuthenticationFilter basicAuthenticationFilter = new BasicAuthenticationFilter(
				providerManager(), basicAuthenticationEntryPoint);
		http.addFilterAfter(oauthFilter, basicAuthenticationFilter.getClass());
		http.addFilterBefore(basicAuthenticationFilter, oauthFilter.getClass());
		http.addFilterBefore(oAuth2AuthenticationProcessingFilter(), basicAuthenticationFilter.getClass());
		this.authorizationProperties.getAuthenticatedPaths().add(dashboard("/**"));
		this.authorizationProperties.getAuthenticatedPaths().add(dashboard(""));

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
				.logoutSuccessUrl(dashboard("/logout-success-oauth.html"))
				.and().csrf().disable()
				.exceptionHandling()
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, new AntPathRequestMatcher("/api/**"))
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, new AntPathRequestMatcher("/actuator/**"))
				.defaultAuthenticationEntryPointFor(
						new LoginUrlAuthenticationEntryPoint(this.authorizationProperties.getLoginProcessingUrl()),
						AnyRequestMatcher.INSTANCE);
		this.securityStateBean.setAuthenticationEnabled(true);
	}
}
