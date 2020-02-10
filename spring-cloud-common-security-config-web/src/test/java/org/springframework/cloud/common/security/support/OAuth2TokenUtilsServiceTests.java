/*
 * Copyright 2019-2020 the original author or authors.
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

import java.time.Instant;

import org.junit.Test;

import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class OAuth2TokenUtilsServiceTests {

	@Test
	public void testGetAccessTokenOfAuthenticatedUserWithNoAuthentication() {
		SecurityContextHolder.getContext().setAuthentication(null);

		final OAuth2AuthorizedClientService oauth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		OAuth2TokenUtilsService oAuth2TokenUtilsService = new DefaultOAuth2TokenUtilsService(oauth2AuthorizedClientService);

		try {
			oAuth2TokenUtilsService.getAccessTokenOfAuthenticatedUser();
		}
		catch (IllegalStateException e) {
			assertEquals(
				"Cannot retrieve the authentication object from the SecurityContext. Are you authenticated?",
				e.getMessage());
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testGetAccessTokenOfAuthenticatedUserWithWrongAuthentication() {
		final Authentication authentication = mock(Authentication.class);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		final OAuth2AuthorizedClientService oauth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		OAuth2TokenUtilsService oAuth2TokenUtilsService = new DefaultOAuth2TokenUtilsService(oauth2AuthorizedClientService);

		try {
			oAuth2TokenUtilsService.getAccessTokenOfAuthenticatedUser();
		}
		catch (IllegalStateException e) {
			assertEquals(
				"Authentication object is not of type OAuth2AuthenticationToken.",
				e.getMessage());
			SecurityContextHolder.getContext().setAuthentication(null);
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	public void testGetAccessTokenOfAuthenticatedUserWithEmptyPrincipalName() {
		final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
		when(authentication.getName()).thenReturn("");
		when(authentication.getAuthorizedClientRegistrationId()).thenReturn("uaa");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		final OAuth2AuthorizedClientService oauth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		OAuth2TokenUtilsService oAuth2TokenUtilsService = new DefaultOAuth2TokenUtilsService(oauth2AuthorizedClientService);

		try {
			oAuth2TokenUtilsService.getAccessTokenOfAuthenticatedUser();
		}
		catch (IllegalStateException e) {
			assertEquals(
				"The retrieved principalName must not be null or empty.",
				e.getMessage());
			SecurityContextHolder.getContext().setAuthentication(null);
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	public void testGetAccessTokenOfAuthenticatedUserWithEmptyClientRegistrationId() {
		final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
		when(authentication.getName()).thenReturn("FOO");
		when(authentication.getAuthorizedClientRegistrationId()).thenReturn("");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		final OAuth2AuthorizedClientService oauth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		OAuth2TokenUtilsService oAuth2TokenUtilsService = new DefaultOAuth2TokenUtilsService(oauth2AuthorizedClientService);

		try {
			oAuth2TokenUtilsService.getAccessTokenOfAuthenticatedUser();
		}
		catch (IllegalStateException e) {
			assertEquals(
				"The retrieved clientRegistrationId must not be null or empty.",
				e.getMessage());
			SecurityContextHolder.getContext().setAuthentication(null);
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	public void testGetAccessTokenOfAuthenticatedUserWithWrongClientRegistrationId() {
		final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
		when(authentication.getName()).thenReturn("my-username");
		when(authentication.getAuthorizedClientRegistrationId()).thenReturn("CID");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		final OAuth2AuthorizedClientService oauth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		when(oauth2AuthorizedClientService.loadAuthorizedClient("uaa", "my-username")).thenReturn(getOAuth2AuthorizedClient());
		final OAuth2TokenUtilsService oauth2TokenUtilsService = new DefaultOAuth2TokenUtilsService(oauth2AuthorizedClientService);

		try {
			oauth2TokenUtilsService.getAccessTokenOfAuthenticatedUser();
		}
		catch (IllegalStateException e) {
			assertEquals(
				"No oauth2AuthorizedClient returned for clientRegistrationId 'CID' and principalName 'my-username'.",
				e.getMessage());
			SecurityContextHolder.getContext().setAuthentication(null);
			return;
		}
		fail("Expected an IllegalStateException to be thrown.");
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	public void testGetAccessTokenOfAuthenticatedUserWithAuthentication() {
		final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
		when(authentication.getName()).thenReturn("my-username");
		when(authentication.getAuthorizedClientRegistrationId()).thenReturn("uaa");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		final OAuth2AuthorizedClientService oauth2AuthorizedClientService = mock(OAuth2AuthorizedClientService.class);
		when(oauth2AuthorizedClientService.loadAuthorizedClient("uaa", "my-username")).thenReturn(getOAuth2AuthorizedClient());
		final OAuth2TokenUtilsService oauth2TokenUtilsService = new DefaultOAuth2TokenUtilsService(oauth2AuthorizedClientService);

		assertEquals("foo-bar-123-token", oauth2TokenUtilsService.getAccessTokenOfAuthenticatedUser());
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	private OAuth2AuthorizedClient getOAuth2AuthorizedClient() {
		final ClientRegistration clientRegistration = ClientRegistration
				.withRegistrationId("uaa")
				.clientId("clientId")
				.clientSecret("clientSecret")
				.redirectUriTemplate("blubba")
				.authorizationUri("blubba")
				.tokenUri("blubba")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.build();
			final OAuth2AccessToken accessToken = new OAuth2AccessToken(TokenType.BEARER, "foo-bar-123-token", Instant.now(), Instant.now().plusMillis(100000));
			final OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(clientRegistration, "my-username", accessToken);
		return authorizedClient;
	}

}
