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
package org.springframework.cloud.dataflow.server.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Disabled;
import org.springframework.cloud.dataflow.server.config.security.support.SecurityConfigUtils;
import org.springframework.cloud.dataflow.server.config.security.support.SecurityStateBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.session.ExpiringSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.HeaderHttpSessionStrategy;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.accept.ContentNegotiationStrategy;

import static org.springframework.cloud.dataflow.server.controller.UiController.dashboard;

/**
 * Setup Spring Security with Basic Authentication for the Rest Endpoints and the
 * Dashboard of Spring Cloud Data Flow.
 * <p>
 * For the OAuth2-specific configuration see {@link OAuthSecurityConfiguration}.
 *
 * @author Gunnar Hillert
 * @see OAuthSecurityConfiguration
 * @since 1.0
 */
@Configuration
@Conditional(OnSecurityEnabledAndOAuth2Disabled.class)
@EnableWebSecurity
public class BasicAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	private ContentNegotiationStrategy contentNegotiationStrategy;

	@Autowired
	private SecurityProperties securityProperties;

	@Autowired
	private AuthorizationConfig authorizationConfig;

	@Autowired
	private SecurityStateBean securityStateBean;

	@Bean
	public SessionRepository<ExpiringSession> sessionRepository() {
		return new MapSessionRepository();
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		final RequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(contentNegotiationStrategy,
				MediaType.TEXT_HTML);

		final String loginPage = dashboard("/#/login");

		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(securityProperties.getBasic().getRealm());
		basicAuthenticationEntryPoint.afterPropertiesSet();

		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security = http.csrf()
				.disable().authorizeRequests().antMatchers("/").authenticated().antMatchers(dashboard("/**"),
						"/authenticate", "/security/info", "/dashboard", "/features", "/assets/**")
				.permitAll();

		if (authorizationConfig.isEnabled()) {
			security = SecurityConfigUtils.configureSimpleSecurity(security, authorizationConfig);
		}

		security.and().formLogin().loginPage(loginPage).loginProcessingUrl(dashboard("/login"))
				.defaultSuccessUrl(dashboard("/")).permitAll().and().logout().logoutUrl(dashboard("/logout"))
				.logoutSuccessUrl(dashboard("/logout-success.html"))
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()).permitAll().and().httpBasic().and()
				.exceptionHandling()
				.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint(loginPage), textHtmlMatcher)
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, AnyRequestMatcher.INSTANCE);

		if (authorizationConfig.isEnabled()) {
			security.anyRequest().denyAll();
		}
		else {
			security.anyRequest().authenticated();
		}

		final SessionRepositoryFilter<ExpiringSession> sessionRepositoryFilter = new SessionRepositoryFilter<ExpiringSession>(
				sessionRepository());
		sessionRepositoryFilter.setHttpSessionStrategy(new HeaderHttpSessionStrategy());

		http.addFilterBefore(sessionRepositoryFilter, ChannelProcessingFilter.class).csrf().disable();
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);

		securityStateBean.setAuthenticationEnabled(true);
		securityStateBean.setAuthorizationEnabled(true);
	}

}
