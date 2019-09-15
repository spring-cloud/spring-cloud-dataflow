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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.cloud.common.security.ManualOAuthAuthenticationDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * @author Gunnar Hillert
 */
public class TokenStoreClearingLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(TokenStoreClearingLogoutSuccessHandler.class);

	private TokenStore tokenStore;
	private OAuth2ClientProperties oAuth2ClientProperties;

	public TokenStoreClearingLogoutSuccessHandler(TokenStore tokenStore, OAuth2ClientProperties oAuth2ClientProperties) {
		this.tokenStore=tokenStore;
		this.oAuth2ClientProperties = oAuth2ClientProperties;
	}

	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {

		final int numberOfTokensBeforeRemoval = tokenStore.findTokensByClientId(oAuth2ClientProperties.getClientId()).size();
		if (authentication instanceof OAuth2Authentication) {
			final OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) authentication;

			if (oAuth2Authentication.getDetails() instanceof ManualOAuthAuthenticationDetails) {
				ManualOAuthAuthenticationDetails authenticationDetails = (ManualOAuthAuthenticationDetails) oAuth2Authentication.getDetails();
				this.tokenStore.removeAccessToken(authenticationDetails.getAccessToken());
			}
			else if (oAuth2Authentication.getDetails() instanceof OAuth2AuthenticationDetails) {
				OAuth2AuthenticationDetails authenticationDetails = (OAuth2AuthenticationDetails) oAuth2Authentication.getDetails();
				this.tokenStore.removeAccessToken(this.tokenStore.readAccessToken(authenticationDetails.getTokenValue()));
			}
		}

		final int numberOfTokensAfterRemoval = tokenStore.findTokensByClientId(oAuth2ClientProperties.getClientId()).size();
		final int numberOfRemovedTokens = numberOfTokensBeforeRemoval - numberOfTokensAfterRemoval;

		if (numberOfRemovedTokens > 0) {
			logger.error("Number of removed tokens: {}. Total number of tokens in store: {}",
					numberOfRemovedTokens, numberOfTokensAfterRemoval);
		}

		super.handle(request, response, authentication);
	}

}
