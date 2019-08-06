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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.cloud.common.security.ManualOAuthAuthenticationDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class TokenUtilsTests {

	@Test
	public void testGetAccessTokenWithNoAuthentication() {
		SecurityContextHolder.getContext().setAuthentication(null);
		assertNull("Without authentication, null should be returned.", TokenUtils.getAccessToken());
	}

	@Test
	public void testGetAccessTokenWithAuthentication() {
		final OAuth2Authentication oAuth2Authentication = mock(OAuth2Authentication.class);
		final OAuth2AuthenticationDetails oAuth2AuthenticationDetails = mock(OAuth2AuthenticationDetails.class);
		when(oAuth2AuthenticationDetails.getTokenValue()).thenReturn("foo-bar-123-token");
		when(oAuth2Authentication.getDetails()).thenReturn(oAuth2AuthenticationDetails);
		SecurityContextHolder.getContext().setAuthentication(oAuth2Authentication);

		assertEquals("foo-bar-123-token", TokenUtils.getAccessToken());
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@Test
	public void testGetAccessTokenWithManualOAuthAuthenticationDetails() {
		final OAuth2Authentication oAuth2Authentication = mock(OAuth2Authentication.class);
		final ManualOAuthAuthenticationDetails manualOAuthAuthenticationDetails = mock(ManualOAuthAuthenticationDetails.class);
		when(manualOAuthAuthenticationDetails.getAccessToken()).thenReturn(new DefaultOAuth2AccessToken("foo-bar-123-token"));
		when(oAuth2Authentication.getDetails()).thenReturn(manualOAuthAuthenticationDetails);
		SecurityContextHolder.getContext().setAuthentication(oAuth2Authentication);

		assertEquals("foo-bar-123-token", TokenUtils.getAccessToken());
		SecurityContextHolder.getContext().setAuthentication(null);
	}
}
