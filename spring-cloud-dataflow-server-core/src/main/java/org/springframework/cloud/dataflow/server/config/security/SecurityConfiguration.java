/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Setup Spring Security for the Rest Endpoints of Spring Cloud Data Flow. For the
 * OAuth2-specific configuration see {@link OAuthSecurityConfiguration}.
 *
 * An (optionally) injected {@link AuthenticationProvider} will be used if available,
 * e.g. via OAuth2-specific configuration.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 * @see OAuthSecurityConfiguration
 *
 */
@Configuration
@ConditionalOnProperty("security.basic.enabled")
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired(required=false)
	private AuthenticationProvider authenticationProvider;

	@Autowired
	private SecurityProperties securityProperties;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(securityProperties.getBasic().getRealm());
		basicAuthenticationEntryPoint.afterPropertiesSet();

		http.antMatcher("/**")
			.authorizeRequests()
				.antMatchers(
					"/security/info**", "/login**", "/dashboard/logout-success.html",
					"/dashboard/styles/**", "/dashboard/images/**", "/dashboard/fonts/**",
					"/dashboard/lib/**").permitAll()
				.anyRequest().authenticated()
			.and().httpBasic()
			.and().exceptionHandling()
					.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint,
							AnyRequestMatcher.INSTANCE)
			.and().logout().logoutSuccessUrl("/dashboard/logout-success.html")
			.and().csrf().disable();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		if (authenticationProvider != null) {
			auth.authenticationProvider(this.authenticationProvider);
		}
		else {
			super.configure(auth);
		}
	}
}