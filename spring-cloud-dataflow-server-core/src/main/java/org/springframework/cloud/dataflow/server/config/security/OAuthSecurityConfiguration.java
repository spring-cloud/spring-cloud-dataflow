/*
 * Copyright 2016-2017 the original author or authors.
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
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Enabled;
import org.springframework.cloud.dataflow.server.config.security.support.SecurityStateBean;
import org.springframework.cloud.dataflow.server.service.impl.ManualOAuthAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import static org.springframework.cloud.dataflow.server.controller.UiController.dashboard;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@EnableOAuth2Sso
@Configuration
@EnableWebSecurity
@Conditional(OnSecurityEnabledAndOAuth2Enabled.class)
public class OAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	private ResourceServerTokenServices tokenServices;

	@Autowired
	private SecurityStateBean securityStateBean;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.addFilterBefore(oAuth2AuthenticationProcessingFilter(), AbstractPreAuthenticatedProcessingFilter.class);
		http.authorizeRequests().antMatchers("/security/info**", "/login**", dashboard("/logout-success-oauth.html"),
				dashboard("/styles/**"), dashboard("/images/**"), dashboard("/fonts/**"), dashboard("/lib/**")

		).permitAll().anyRequest().authenticated().and().httpBasic().and().logout()
				.logoutSuccessUrl(dashboard("/logout-success-oauth.html")).and().csrf().disable();

		securityStateBean.setAuthenticationEnabled(true);
		securityStateBean.setAuthorizationEnabled(false);
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		return new ManualOAuthAuthenticationProvider();
	}

	private OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter() {
		final OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter = new OAuth2AuthenticationProcessingFilter();
		oAuth2AuthenticationProcessingFilter.setAuthenticationManager(oauthAuthenticationManager());
		oAuth2AuthenticationProcessingFilter.setStateless(false);
		return oAuth2AuthenticationProcessingFilter;
	}

	private AuthenticationManager oauthAuthenticationManager() {
		final OAuth2AuthenticationManager oauthAuthenticationManager = new OAuth2AuthenticationManager();
		oauthAuthenticationManager.setTokenServices(tokenServices);
		return oauthAuthenticationManager;
	}
}
