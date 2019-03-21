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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.Filter;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.cloud.dataflow.server.config.security.support.DefaultDataflowAuthoritiesExtractor;
import org.springframework.cloud.dataflow.server.config.security.support.OnSecurityEnabledAndOAuth2Enabled;
import org.springframework.cloud.dataflow.server.config.security.support.SecurityConfigUtils;
import org.springframework.cloud.dataflow.server.config.security.support.SecurityStateBean;
import org.springframework.cloud.dataflow.server.service.impl.ManualOAuthAuthenticationProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2AuthenticationFailureEvent;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.NativeWebRequest;

import static org.springframework.cloud.dataflow.server.controller.UiController.dashboard;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@EnableOAuth2Client
@EnableWebSecurity
@Configuration
@Conditional(OnSecurityEnabledAndOAuth2Enabled.class)
public class OAuthSecurityConfiguration extends WebSecurityConfigurerAdapter {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OAuthSecurityConfiguration.class);

	@Autowired
	private SecurityStateBean securityStateBean;

	@Autowired
	private SecurityProperties securityProperties;

	@Autowired
	private OAuth2ClientContext oauth2ClientContext;

	@Autowired
	private AuthorizationCodeResourceDetails authorizationCodeResourceDetails;

	@Autowired
	private ResourceServerProperties resourceServerProperties;

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private AuthorizationConfig authorizationConfig;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		final RequestMatcher textHtmlMatcher = new MediaTypeRequestMatcher(
				new BrowserDetectingContentNegotiationStrategy(),
				MediaType.TEXT_HTML);

		final BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
		basicAuthenticationEntryPoint.setRealmName(securityProperties.getBasic().getRealm());
		basicAuthenticationEntryPoint.afterPropertiesSet();

		final Filter oauthFilter = oauthFilter();
		BasicAuthenticationFilter basicAuthenticationFilter = new BasicAuthenticationFilter(
				providerManager(), basicAuthenticationEntryPoint);

		http.addFilterAfter(oauthFilter, basicAuthenticationFilter.getClass());
		http.addFilterBefore(basicAuthenticationFilter, oauthFilter.getClass());
		http.addFilterBefore(oAuth2AuthenticationProcessingFilter(), basicAuthenticationFilter.getClass());

		ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry security =

				http.authorizeRequests()
						.antMatchers(
								"/favicon.ico",
								"/security/info**", "/login**", dashboard("/logout-success-oauth.html"),
								dashboard("/styles/**"), dashboard("/images/**"), "/assets/**", dashboard("/fonts/**"),
								dashboard("/lib/**"))
						.permitAll()
						.antMatchers("/", dashboard("/**"), "/dashboard", "/features").authenticated();

		if (authorizationConfig.isEnabled()) {
			security = SecurityConfigUtils.configureSimpleSecurity(security, authorizationConfig);
			security.anyRequest().denyAll();
			securityStateBean.setAuthorizationEnabled(true);
		}
		else {
			security.anyRequest().authenticated();
			securityStateBean.setAuthorizationEnabled(false);
		}

		http.httpBasic().and()
				.logout()
				.logoutSuccessUrl(dashboard("/logout-success-oauth.html"))
				.and().csrf().disable()
				.exceptionHandling()
				.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), textHtmlMatcher)
				.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, AnyRequestMatcher.INSTANCE);

		securityStateBean.setAuthenticationEnabled(true);
	}

	@Bean
	public UserInfoTokenServices tokenServices() {
		final UserInfoTokenServices tokenServices = new UserInfoTokenServices(resourceServerProperties.getUserInfoUri(),
				authorizationCodeResourceDetails.getClientId());
		tokenServices.setRestTemplate(oAuth2RestTemplate());
		tokenServices.setAuthoritiesExtractor(new DefaultDataflowAuthoritiesExtractor());
		return tokenServices;
	}

	@Bean
	public OAuth2RestTemplate oAuth2RestTemplate() {
		final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(authorizationCodeResourceDetails,
				oauth2ClientContext);
		return oAuth2RestTemplate;
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		return new ManualOAuthAuthenticationProvider();
	}

	@Bean
	public ProviderManager providerManager() {
		List<AuthenticationProvider> providers = new ArrayList<>();
		providers.add(this.authenticationProvider());
		ProviderManager providerManager = new ProviderManager(providers);
		return providerManager;
	}

	private Filter oauthFilter() {
		final OAuth2ClientAuthenticationProcessingFilter oauthFilter = new OAuth2ClientAuthenticationProcessingFilter(
				"/login");
		oauthFilter.setRestTemplate(oAuth2RestTemplate());
		oauthFilter.setTokenServices(tokenServices());
		oauthFilter.setApplicationEventPublisher(this.applicationEventPublisher);
		return oauthFilter;
	}

	private OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter() {
		final OAuth2AuthenticationProcessingFilter oAuth2AuthenticationProcessingFilter = new OAuth2AuthenticationProcessingFilter();
		oAuth2AuthenticationProcessingFilter.setAuthenticationManager(oauthAuthenticationManager());
		oAuth2AuthenticationProcessingFilter.setStateless(false);
		return oAuth2AuthenticationProcessingFilter;
	}

	@Bean
	public AuthenticationManager oauthAuthenticationManager() {
		final OAuth2AuthenticationManager oauthAuthenticationManager = new OAuth2AuthenticationManager();
		oauthAuthenticationManager.setTokenServices(tokenServices());
		return oauthAuthenticationManager;
	}

	@EventListener
	public void handleOAuth2AuthenticationFailureEvent(
			OAuth2AuthenticationFailureEvent oAuth2AuthenticationFailureEvent) {
		final int throwableIdexForResourceAccessException = ExceptionUtils
				.indexOfThrowable(oAuth2AuthenticationFailureEvent.getException(), ResourceAccessException.class);

		if (throwableIdexForResourceAccessException > -1) {
			logger.error("An error ocurred while accessing an authentication REST resource.",
					oAuth2AuthenticationFailureEvent.getException());
		}

	}

	private static class BrowserDetectingContentNegotiationStrategy extends HeaderContentNegotiationStrategy {

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
}
