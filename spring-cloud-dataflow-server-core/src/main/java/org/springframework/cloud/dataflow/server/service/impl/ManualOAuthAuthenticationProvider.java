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
package org.springframework.cloud.dataflow.server.service.impl;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.client.ResourceAccessException;

/**
 * Provides a custom {@link AuthenticationProvider} that allows for authentication
 * (username and password) against an OAuth Server using a {@code password grant}.
 *
 * @author Gunnar Hillert
 */
public class ManualOAuthAuthenticationProvider implements AuthenticationProvider {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ManualOAuthAuthenticationProvider.class);

	@Autowired
	private OAuth2ClientProperties oAuth2ClientProperties;

	@Value("${security.oauth2.client.access-token-uri}")
	private String accessTokenUri;

	@Autowired
	private UserInfoTokenServices userInfoTokenServices;

	public AccessTokenProvider userAccessTokenProvider() {
		ResourceOwnerPasswordAccessTokenProvider accessTokenProvider = new ResourceOwnerPasswordAccessTokenProvider();
		return accessTokenProvider;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		final String username = authentication.getName();
		final String password = authentication.getCredentials().toString();

		final ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();

		resource.setUsername(username);
		resource.setPassword(password);

		resource.setAccessTokenUri(accessTokenUri);
		resource.setClientId(oAuth2ClientProperties.getClientId());
		resource.setClientSecret(oAuth2ClientProperties.getClientSecret());
		resource.setGrantType("password");

		final OAuth2RestTemplate template = new OAuth2RestTemplate(resource,
				new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest()));
		template.setAccessTokenProvider(userAccessTokenProvider());

		final OAuth2AccessToken accessToken;
		try {
			logger.warn("Authenticating user '{}' using accessTokenUri '{}'.", username, accessTokenUri);
			accessToken = template.getAccessToken();
		}
		catch (OAuth2AccessDeniedException e) {
			if (e.getCause() instanceof ResourceAccessException) {
				final String errorMessage = String.format(
						"While authenticating user '%s': " + "Unable to access accessTokenUri '%s'.", username,
						accessTokenUri);
				logger.error(errorMessage + " Error message: {}.", e.getCause().getMessage());
				throw new AuthenticationServiceException(errorMessage, e);
			}
			throw new BadCredentialsException(String.format("Access denied for user '%s'.", username), e);
		}
		catch (OAuth2Exception e) {
			throw new AuthenticationServiceException(
					String.format("Unable to perform OAuth authentication for user '%s'.", username), e);
		}

		final OAuth2Authentication auth2Authentication = userInfoTokenServices.loadAuthentication(accessToken.getValue());
		return auth2Authentication;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
}
