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

import org.springframework.cloud.common.security.ManualOAuthAuthenticationDetails;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

/**
 * Utility methods for retrieving access tokens.
 *
 * @author Gunnar Hillert
 */
public final class TokenUtils {

	private TokenUtils() {
		throw new AssertionError("This is a utility class.");
	}

	/**
	 * Retrieves the access token from the {@link Authentication} implementation.
	 * 
	 * @return May return null.
	 */
	public static final String getAccessToken() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		final String token;
		if (authentication != null && authentication instanceof OAuth2Authentication) {
			final OAuth2Authentication auth2Authentication = (OAuth2Authentication) authentication;

			if (auth2Authentication.getDetails() instanceof OAuth2AuthenticationDetails) {
				final OAuth2AuthenticationDetails auth2AuthenticationDetails =
					(OAuth2AuthenticationDetails) auth2Authentication.getDetails();
				token = auth2AuthenticationDetails.getTokenValue();
			}
			else if (auth2Authentication.getDetails() instanceof ManualOAuthAuthenticationDetails) {
				ManualOAuthAuthenticationDetails manualOAuthAuthenticationDetails =
					(ManualOAuthAuthenticationDetails) auth2Authentication.getDetails();
				token = manualOAuthAuthenticationDetails.getAccessToken().getValue();
			}
			else {
				token = null;
			}
		}
		else {
			token = null;
		}
		return token;
	}
}
