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

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.util.Assert;

/**
 * Customized {@link SimpleUrlLogoutSuccessHandler} that will remove the previously authenticated user's
 * {@link OAuth2AuthorizedClient} from the underlying {@link OAuth2AuthorizedClientService}.
 *
 * @author Gunnar Hillert
 * @since 1.3.0
 */
public class AccessTokenClearingLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(AccessTokenClearingLogoutSuccessHandler.class);

	final OAuth2TokenUtilsService oauth2TokenUtilsService;

	public AccessTokenClearingLogoutSuccessHandler(OAuth2TokenUtilsService oauth2TokenUtilsService) {
		Assert.notNull(oauth2TokenUtilsService, "oauth2TokenUtilsService must not be null.");
		this.oauth2TokenUtilsService = oauth2TokenUtilsService;
	}

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException, ServletException {

		if (authentication instanceof OAuth2AuthenticationToken) {
			final OAuth2AuthenticationToken oauth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
			final OAuth2AuthorizedClient oauth2AuthorizedClient = oauth2TokenUtilsService.getAuthorizedClient(oauth2AuthenticationToken);
			oauth2TokenUtilsService.removeAuthorizedClient(oauth2AuthorizedClient);
			logger.info("Removed OAuth2AuthorizedClient.");
		}

		super.handle(request, response, authentication);
	}

}
