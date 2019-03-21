/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.security;

import static org.springframework.cloud.dataflow.server.controller.UiController.dashboard;

import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Enabled;
import org.springframework.cloud.dataflow.server.service.impl.ManualOAuthAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 *
 */
@EnableOAuth2Sso
@Configuration
@Conditional(OnSecurityEnabledAndOAuth2Enabled.class)
public class OAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.antMatcher("/**")
		.authorizeRequests()
			.antMatchers(
					"/security/info**",
					"/login**",
					dashboard("/logout-success-oauth.html"),
					dashboard("/styles/**"),
					dashboard("/images/**"),
					dashboard("/fonts/**"),
					dashboard("/lib/**")

					).permitAll()
			.anyRequest().authenticated()
		.and().httpBasic()
		.and().logout().logoutSuccessUrl(dashboard("/logout-success-oauth.html"))
		.and().csrf().disable();
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(authenticationProvider());
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		return new ManualOAuthAuthenticationProvider();
	}

}
