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
package org.springframework.cloud.common.security.support;

import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility methods for retrieving access tokens.
 *
 * @author Gunnar Hillert
 */
public class DefaultOAuth2TokenUtilsService implements OAuth2TokenUtilsService {

	private final OAuth2AuthorizedClientService oauth2AuthorizedClientService;

	public DefaultOAuth2TokenUtilsService(OAuth2AuthorizedClientService oauth2AuthorizedClientService) {
		Assert.notNull(oauth2AuthorizedClientService, "oauth2AuthorizedClientService must not be null.");
		this.oauth2AuthorizedClientService = oauth2AuthorizedClientService;
	}

	/**
	 * Retrieves the access token from the {@link Authentication} implementation.
	 *
	 * @return May return null.
	 */
	@Override
	public String getAccessTokenOfAuthenticatedUser() {

		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {
			throw new IllegalStateException("Cannot retrieve the authentication object from the SecurityContext. Are you authenticated?");
		}

		final String accessTokenOfAuthenticatedUser;

		if (authentication instanceof BearerTokenAuthentication) {
			accessTokenOfAuthenticatedUser = ((BearerTokenAuthentication) authentication).getToken().getTokenValue();
		}
		else if (authentication instanceof OAuth2AuthenticationToken) {
			final OAuth2AuthenticationToken oauth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
			final OAuth2AuthorizedClient oauth2AuthorizedClient = this.getAuthorizedClient(oauth2AuthenticationToken);
			accessTokenOfAuthenticatedUser = oauth2AuthorizedClient.getAccessToken().getTokenValue();
		}
		else {
			throw new IllegalStateException("Authentication object is not of type OAuth2AuthenticationToken.");
		}

		return accessTokenOfAuthenticatedUser;
	}

	@Override
	public OAuth2AuthorizedClient getAuthorizedClient(OAuth2AuthenticationToken auth2AuthenticationToken) {

		final String principalName = auth2AuthenticationToken.getName();
		final String clientRegistrationId = auth2AuthenticationToken.getAuthorizedClientRegistrationId();

		if (StringUtils.isEmpty(principalName)) {
			throw new IllegalStateException("The retrieved principalName must not be null or empty.");
		}

		if (StringUtils.isEmpty(clientRegistrationId)) {
			throw new IllegalStateException("The retrieved clientRegistrationId must not be null or empty.");
		}

		final OAuth2AuthorizedClient oauth2AuthorizedClient = this.oauth2AuthorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName);

		if (oauth2AuthorizedClient == null) {
			throw new IllegalStateException(String.format(
				"No oauth2AuthorizedClient returned for clientRegistrationId '%s' and principalName '%s'.",
				clientRegistrationId, principalName));
		}
		return oauth2AuthorizedClient;
	}

	@Override
	public void removeAuthorizedClient(OAuth2AuthorizedClient auth2AuthorizedClient) {
		Assert.notNull(auth2AuthorizedClient, "The auth2AuthorizedClient must not be null.");
		this.oauth2AuthorizedClientService.removeAuthorizedClient(
			auth2AuthorizedClient.getClientRegistration().getRegistrationId(),
			auth2AuthorizedClient.getPrincipalName());
	}
}
