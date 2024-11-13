/*
 * Copyright 2016-2019 the original author or authors.
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
package org.springframework.cloud.common.security;

import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2PasswordGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.web.client.ResourceAccessException;

/**
 * Provides a custom {@link AuthenticationProvider} that allows for authentication
 * (username and password) against an OAuth Server using a {@code password grant}.
 *
 * @author Gunnar Hillert
 */
public class ManualOAuthAuthenticationProvider implements AuthenticationProvider {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ManualOAuthAuthenticationProvider.class);

	private final OAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> oAuth2PasswordTokenResponseClient;
	private final ClientRegistrationRepository clientRegistrationRepository;
	private final AuthenticationProvider authenticationProvider;
	private final String providerId;

	public ManualOAuthAuthenticationProvider(
			OAuth2AccessTokenResponseClient<OAuth2PasswordGrantRequest> oAuth2PasswordTokenResponseClient,
			ClientRegistrationRepository clientRegistrationRepository,
			OpaqueTokenIntrospector opaqueTokenIntrospector,
			String providerId) {

		this.oAuth2PasswordTokenResponseClient = oAuth2PasswordTokenResponseClient;
		this.clientRegistrationRepository = clientRegistrationRepository;
		this.authenticationProvider =
				new OpaqueTokenAuthenticationProvider(opaqueTokenIntrospector);
		this.providerId = providerId;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		final String username = authentication.getName();
		final String password = authentication.getCredentials().toString();

		final ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(providerId);
		final ClientRegistration clientRegistrationPassword = ClientRegistration.withClientRegistration(clientRegistration).authorizationGrantType(AuthorizationGrantType.PASSWORD).build();

		final OAuth2PasswordGrantRequest grantRequest = new OAuth2PasswordGrantRequest(clientRegistrationPassword, username, password);
		final OAuth2AccessTokenResponse accessTokenResponse;
		final String accessTokenUri = clientRegistration.getProviderDetails().getTokenUri();

		try {
			accessTokenResponse = oAuth2PasswordTokenResponseClient.getTokenResponse(grantRequest);
			logger.warn("Authenticating user '{}' using accessTokenUri '{}'.", username, accessTokenUri);
		}
		catch (OAuth2AuthorizationException e) {
			if (e.getCause() instanceof ResourceAccessException) {
					final String errorMessage = String.format(
						"While authenticating user '%s': " + "Unable to access accessTokenUri '%s'.", username,
						accessTokenUri);
				logger.error(errorMessage + " Error message: {}.", e.getCause().getMessage());
				throw new AuthenticationServiceException(errorMessage, e);
			}
			else {
				throw new BadCredentialsException(String.format("Access denied for user '%s'.", username), e);
			}

		}

		final BearerTokenAuthenticationToken authenticationRequest = new BearerTokenAuthenticationToken(accessTokenResponse.getAccessToken().getTokenValue());

		Authentication newAuthentication = null;
		try {
			newAuthentication = this.authenticationProvider.authenticate(authenticationRequest);
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(newAuthentication);
			SecurityContextHolder.setContext(context);
		} catch (AuthenticationException failed) {
			SecurityContextHolder.clearContext();
			logger.warn("Authentication request for failed!", failed);
			//this.authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
		}

		return newAuthentication;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}
}
