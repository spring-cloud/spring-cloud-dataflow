/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.single.security.support.oauth2testserver;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.builders.ClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;


/**
 *
 * @author Gunnar Hillert
 *
 */
@Configuration
@EnableAuthorizationServer
public class AuthServerConfig extends AuthorizationServerConfigurerAdapter {

	@Autowired
	private BaseClientDetails details;

	private AuthenticationManager authenticationManagerBean;

	@Autowired
	public void setAuthenticationManagerBean(AuthenticationManager authenticationManagerBean) {
		this.authenticationManagerBean = authenticationManagerBean;
	}

	@Override
	public void configure(final AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
		oauthServer
			.tokenKeyAccess("permitAll()")
			.checkTokenAccess("isAuthenticated()");
	}

	@Override
	public void configure(final ClientDetailsServiceConfigurer clients) throws Exception {
		ClientDetailsServiceBuilder<InMemoryClientDetailsServiceBuilder>.ClientBuilder builder = clients
				.inMemory().withClient(this.details.getClientId());
		builder.secret(this.details.getClientSecret())
				.resourceIds(this.details.getResourceIds().toArray(new String[0]))
				.authorizedGrantTypes(
						this.details.getAuthorizedGrantTypes().toArray(new String[0]))
				.authorities(AuthorityUtils
						.authorityListToSet(this.details.getAuthorities())
						.toArray(new String[0]))
				.scopes(this.details.getScope().toArray(new String[0]));

		if (this.details.getAutoApproveScopes() != null) {
			builder.autoApprove(
					this.details.getAutoApproveScopes().toArray(new String[0]));
		}
		if (this.details.getAccessTokenValiditySeconds() != null) {
			builder.accessTokenValiditySeconds(
					this.details.getAccessTokenValiditySeconds());
		}
		if (this.details.getRefreshTokenValiditySeconds() != null) {
			builder.refreshTokenValiditySeconds(
					this.details.getRefreshTokenValiditySeconds());
		}
		if (this.details.getRegisteredRedirectUri() != null) {
			builder.redirectUris(
					this.details.getRegisteredRedirectUri().toArray(new String[0]));
		}
	}

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		super.configure(endpoints);
		endpoints.authenticationManager(authenticationManagerBean);
		endpoints.tokenStore(tokenStore());
		endpoints.tokenEnhancer(new TokenEnhancer() {

			@Override
			public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
				if (authentication.getPrincipal() instanceof User) {
					final User user = (User) authentication.getPrincipal();

					final Set<String> scopes = new HashSet<String>();
					for (GrantedAuthority authority : user.getAuthorities()) {
						final String role = authority.getAuthority();

						if (role.startsWith("ROLE_")) {
							scopes.add(role.substring(5).toLowerCase());
						}
						else {
							scopes.add(role.toLowerCase());
						}
					}
					((DefaultOAuth2AccessToken) accessToken).setScope(scopes);

				}
				return accessToken;
			}
		});
	}

	@Bean
	public TokenStore tokenStore() {
		return new InMemoryTokenStore();
	}

	@Bean
	@ConfigurationProperties(prefix = "security.oauth2.client")
	public BaseClientDetails oauth2ClientDetails() {
		return new BaseClientDetails();
	}
}
