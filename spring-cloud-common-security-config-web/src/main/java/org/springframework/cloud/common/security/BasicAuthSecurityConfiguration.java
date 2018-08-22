/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.common.security;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.cloud.common.security.support.OnSecurityEnabledAndOAuth2Disabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.cloud.common.security.support.SecurityConfigUtils;
import org.springframework.cloud.common.security.support.SecurityStateBean;
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
import org.springframework.session.MapSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.accept.ContentNegotiationStrategy;

/**
 * Setup Spring Security with Basic Authentication for the Rest Endpoints and the
 * Dashboard of Spring Cloud Data Flow.
 * <p>
 * For the OAuth2-specific configuration see {@link OAuthSecurityConfiguration}.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 *
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
	private AuthorizationProperties authorizationProperties;

	@Autowired
	private SecurityStateBean securityStateBean;

	@Bean
	public SessionRepository<? extends Session> sessionRepository() {
		return new MapSessionRepository(new ConcurrentHashMap<>());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		final RequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(contentNegotiationStrategy,
				MediaType.TEXT_HTML);
		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(SecurityConfigUtils.BASIC_AUTH_REALM_NAME);
		basicAuthenticationEntryPoint.afterPropertiesSet();
		this.authorizationProperties.getAuthenticatedPaths().add("/");
		this.authorizationProperties.getPermitAllPaths().add(this.authorizationProperties.getDashboardUrl());
		this.authorizationProperties.getPermitAllPaths().add(dashboard("/**"));
		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security = http.csrf()
				.disable().authorizeRequests()
				.antMatchers(this.authorizationProperties.getAuthenticatedPaths().toArray(new String[0])).authenticated()
				.antMatchers(this.authorizationProperties.getPermitAllPaths().toArray(new String[0]))
				.permitAll();
		if (this.authorizationProperties.isEnabled()) {
			security = SecurityConfigUtils.configureSimpleSecurity(security, authorizationProperties);
		}
		final String loginPage = dashboard(this.authorizationProperties.getLoginUrl());
		security.and().formLogin().loginPage(loginPage)
				.loginProcessingUrl(dashboard(this.authorizationProperties.getLoginProcessingUrl()))
				.defaultSuccessUrl(dashboard("/")).permitAll().and().logout()
				.logoutUrl(dashboard(this.authorizationProperties.getLogoutUrl()))
				.logoutSuccessUrl(dashboard(this.authorizationProperties.getLogoutSuccessUrl()))
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()).permitAll().and().httpBasic().and()
				.exceptionHandling()
				.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint(loginPage), textHtmlMatcher)
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, AnyRequestMatcher.INSTANCE);
		if (this.authorizationProperties.isEnabled()) {
			security.anyRequest().denyAll();
		}
		else {
			security.anyRequest().authenticated();
		}
		final SessionRepositoryFilter sessionRepositoryFilter = new SessionRepositoryFilter(
				sessionRepository());
		sessionRepositoryFilter.setHttpSessionIdResolver(HeaderHttpSessionIdResolver.xAuthToken());

		http.addFilterBefore(sessionRepositoryFilter, ChannelProcessingFilter.class).csrf().disable();
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
		this.securityStateBean.setAuthenticationEnabled(true);
		this.securityStateBean.setAuthorizationEnabled(true);
	}

	/**
	 * Turn a relative link of the UI app to an absolute one, prepending its path.
	 *
	 * @param path relative UI path
	 * @return the absolute UI path
	 */
	private String dashboard(String path) {
		return this.authorizationProperties.getDashboardUrl() + path;
	}

}
