/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.common.security.core.support;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.Assert;

/**
 * This implementation of a {@link ClientHttpRequestInterceptor} will retrieve, if available, the OAuth2 Access Token
 * and add it to the {@code Authorization} HTTP header.
 *
 * @author Gunnar Hillert
 */
public class OAuth2AccessTokenProvidingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private final String staticOauthAccessToken;

	private final OAuth2TokenUtilsService oauth2TokenUtilsService;

	public OAuth2AccessTokenProvidingClientHttpRequestInterceptor(String staticOauthAccessToken) {
		super();
		Assert.hasText(staticOauthAccessToken, "staticOauthAccessToken must not be null or empty.");
		this.staticOauthAccessToken = staticOauthAccessToken;
		this.oauth2TokenUtilsService = null;
	}

	public OAuth2AccessTokenProvidingClientHttpRequestInterceptor(OAuth2TokenUtilsService oauth2TokenUtilsService) {
		super();
		this.oauth2TokenUtilsService = oauth2TokenUtilsService;
		this.staticOauthAccessToken = null;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {

		final String tokenToUse;

		if (this.staticOauthAccessToken != null) {
			tokenToUse = this.staticOauthAccessToken;
		}
		else if (this.oauth2TokenUtilsService != null){
			tokenToUse = this.oauth2TokenUtilsService.getAccessTokenOfAuthenticatedUser();
		}
		else {
			tokenToUse = null;
		}

		if (tokenToUse != null) {
			request.getHeaders().add(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.TokenType.BEARER.getValue() + " " + tokenToUse);
		}
		return execution.execute(request, body);
	}
}
