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

import static org.springframework.cloud.dataflow.server.controller.UiController.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Disabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationStrategy;

/**
 * Setup Spring Security with Basic Authentication for the Rest Endpoints and the
 * Dashboard of Spring Cloud Data Flow.
 *
 * For the OAuth2-specific configuration see {@link OAuthSecurityConfiguration}.
 *
 * @author Gunnar Hillert
 * @since 1.0
 *
 * @see OAuthSecurityConfiguration
 *
 */
@Configuration
@Conditional(OnSecurityEnabledAndOAuth2Disabled.class)
@EnableWebSecurity
public class BasicAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(BasicAuthSecurityConfiguration.class);

	public static final Pattern AUTHORIZATION_RULE;

	static {
		String methodsRegex = StringUtils.arrayToDelimitedString(HttpMethod.values(),
				"|");
		AUTHORIZATION_RULE = Pattern
				.compile("(" + methodsRegex + ")\\s+(.+)\\s+=>\\s+(.+)");
	}

	@Bean
	public SessionRepository<ExpiringSession> sessionRepository() {
		return new MapSessionRepository();
	}

	@Autowired
	private ContentNegotiationStrategy contentNegotiationStrategy;

	@Autowired
	private SecurityProperties securityProperties;

	@Autowired
	private AuthorizationConfig authorizationConfig;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		final RequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(
				contentNegotiationStrategy,
				MediaType.TEXT_HTML);

		final String loginPage = dashboard("/#/login");

		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(securityProperties.getBasic().getRealm());
		basicAuthenticationEntryPoint.afterPropertiesSet();

		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security = http
			.csrf()
			.disable()
			.authorizeRequests()
			.antMatchers("/")
			.authenticated()
			.antMatchers(
					dashboard("/**"),
					"/authenticate",
					"/security/info",
					"/dashboard",
					"/about",
					"/features",
					"/assets/**").permitAll();

		if (authorizationConfig.isEnabled()) {
			security = configureSimpleSecurity(security);
		}

		security.and()
			.formLogin().loginPage(loginPage)
			.loginProcessingUrl(dashboard("/login"))
			.defaultSuccessUrl(dashboard("/")).permitAll()
		.and()
			.logout().logoutUrl(dashboard("/logout"))
				.logoutSuccessUrl(dashboard("/logout-success.html"))
			.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()).permitAll()
		.and().httpBasic()
			.and().exceptionHandling()
			.defaultAuthenticationEntryPointFor(
					new LoginUrlAuthenticationEntryPoint(loginPage),
					textHtmlMatcher)
			.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint,
					AnyRequestMatcher.INSTANCE);

		if (authorizationConfig.isEnabled()) {
			security.anyRequest().denyAll();
		}
		else {
			security.anyRequest().authenticated();
		}

		final SessionRepositoryFilter<ExpiringSession> sessionRepositoryFilter = new SessionRepositoryFilter<ExpiringSession>(
				sessionRepository());
		sessionRepositoryFilter
				.setHttpSessionStrategy(new HeaderHttpSessionStrategy());

		http.addFilterBefore(sessionRepositoryFilter,
				ChannelProcessingFilter.class).csrf().disable();
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
	}

	/**
	 * Read the configuration for "simple" (that is, not ACL based) security and apply it.
	 */
	private ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry configureSimpleSecurity(
			ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security) {
		for (String rule : authorizationConfig.getRules()) {
			Matcher matcher = AUTHORIZATION_RULE.matcher(rule);
			Assert.isTrue(matcher.matches(),
					String.format(
							"Unable to parse security rule [%s], expected format is 'HTTP_METHOD ANT_PATTERN => SECURITY_ATTRIBUTE(S)'",
							rule));

			HttpMethod method = HttpMethod.valueOf(matcher.group(1).trim());
			String urlPattern = matcher.group(2).trim();
			String attribute = matcher.group(3).trim();

			logger.info("Authorization '{}' | '{}' | '{}'", method, attribute, urlPattern);
			security = security.antMatchers(method, urlPattern).access(attribute);
		}
		return security;
	}

	/**
	 * Holds configuration for the authorization aspects of security.
	 *
	 * @author Eric Bottard
	 * @author Gunnar Hillert
	 */
	@ConfigurationProperties(prefix = "spring.cloud.dataflow.security.authorization")
	public static class AuthorizationConfig {

		private boolean enabled = true;
		private List<String> rules = new ArrayList<>();

		public List<String> getRules() {
			return rules;
		}

		public void setRules(List<String> rules) {
			this.rules = rules;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}
}
