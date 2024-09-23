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

package org.springframework.cloud.skipper.server.config.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.cloud.common.security.AuthorizationProperties;
import org.springframework.cloud.common.security.OAuthClientConfiguration;
import org.springframework.cloud.common.security.ProviderRoleMapping;
import org.springframework.cloud.common.security.support.MappingJwtGrantedAuthoritiesConverter;
import org.springframework.cloud.common.security.support.OnOAuth2SecurityEnabled;
import org.springframework.cloud.common.security.support.SecurityConfigUtils;
import org.springframework.cloud.common.security.support.SecurityStateBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

/**
 * Setup Spring Security OAuth for the Rest Endpoints of Spring Cloud Data Flow.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@Configuration(proxyBeanMethods = false)
@Conditional(OnOAuth2SecurityEnabled.class)
@Import({ OAuthClientConfiguration.class })
@EnableWebSecurity
public class SkipperOAuthSecurityConfiguration {

	private final OpaqueTokenIntrospector opaqueTokenIntrospector;
	private final AuthenticationManager authenticationManager;
	private final AuthorizationProperties authorizationProperties;
	private final OAuth2ResourceServerProperties oAuth2ResourceServerProperties;
	private final OAuth2ClientProperties oauth2ClientProperties;
	private final SecurityStateBean securityStateBean;

	public SkipperOAuthSecurityConfiguration(ObjectProvider<OpaqueTokenIntrospector> opaqueTokenIntrospector,
			ObjectProvider<AuthenticationManager> authenticationManager,
			ObjectProvider<AuthorizationProperties> authorizationProperties,
			ObjectProvider<OAuth2ResourceServerProperties> oAuth2ResourceServerProperties,
			ObjectProvider<OAuth2ClientProperties> oauth2ClientProperties,
			ObjectProvider<SecurityStateBean> securityStateBean
	){
		this.opaqueTokenIntrospector = opaqueTokenIntrospector.getIfAvailable();
		this.authenticationManager = authenticationManager.getIfAvailable();
		this.authorizationProperties = authorizationProperties.getIfAvailable();
		this.oAuth2ResourceServerProperties = oAuth2ResourceServerProperties.getIfAvailable();
		this.oauth2ClientProperties = oauth2ClientProperties.getIfAvailable();
		this.securityStateBean = securityStateBean.getIfAvailable();
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

		http.authorizeHttpRequests(auth -> {
			auth.requestMatchers(authorizationProperties.getPermitAllPaths().toArray(String[]::new)).permitAll();
			auth.requestMatchers(authorizationProperties.getAuthenticatedPaths().toArray(String[]::new)).authenticated();
			SecurityConfigUtils.configureSimpleSecurity(auth, authorizationProperties);
		});


		http.httpBasic(Customizer.withDefaults());
		http.csrf(AbstractHttpConfigurer::disable);

		http.exceptionHandling(auth -> {
			auth.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, new AntPathRequestMatcher("/api/**"));
			auth.defaultAuthenticationEntryPointFor(basicAuthenticationEntryPoint, new AntPathRequestMatcher("/actuator/**"));
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
