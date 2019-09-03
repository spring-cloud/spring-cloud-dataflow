/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * Holder class for an {@link OAuth2AccessToken}. To be used in conjunction with
 * the ManualOAuthAuthenticationProvider.
 *
 * @author Gunnar Hillert
 */
public class ManualOAuthAuthenticationDetails {

	private OAuth2AccessToken accessToken;

	public ManualOAuthAuthenticationDetails(OAuth2AccessToken accessToken) {
		this.accessToken = accessToken;
	}

	public OAuth2AccessToken getAccessToken() {
		return accessToken;
	}

}
