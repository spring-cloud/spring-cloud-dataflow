/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.config;

import java.io.IOException;

import org.springframework.cloud.common.security.ManualOAuthAuthenticationDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

/**
 * This implementation of a {@link ClientHttpRequestInterceptor} will retrieve, if available, the OAuth2 Access Token
 * and add it to the {@code Authorization} HTTP header.
 *
 * @author Gunnar Hillert
 */
public class OAuth2AccessTokenProvidingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication instanceof OAuth2Authentication) {
			final OAuth2Authentication auth2Authentication = (OAuth2Authentication) authentication;

			final String token;
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

			if (token != null) {
				request.getHeaders().add(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.BEARER_TYPE + " " + token);
			}
		}
		return execution.execute(request, body);
	}
}
