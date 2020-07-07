/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.cloudfoundry.security.support;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Cloud Foundry security service to handle REST calls to the cloud controller and UAA.
 *
 * @author Madhura Bhave
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 *
 */
public class CloudFoundrySecurityService {

	private static final Logger logger = LoggerFactory.getLogger(CloudFoundrySecurityService.class);

	private final OAuth2TokenUtilsService oauth2TokenUtilsService;
	private final RestTemplate restTemplate;

	private final String cloudControllerUrl;

	private final String applicationId;

	public CloudFoundrySecurityService(OAuth2TokenUtilsService oauth2TokenUtilsService,
			RestTemplate restTemplate, String cloudControllerUrl,
			String applicationId) {
		Assert.notNull(oauth2TokenUtilsService, "oauth2TokenUtilsService must not be null.");
		Assert.notNull(restTemplate, "restTemplate must not be null.");
		Assert.notNull(cloudControllerUrl, "CloudControllerUrl must not be null.");
		Assert.notNull(applicationId, "ApplicationId must not be null.");
		this.oauth2TokenUtilsService = oauth2TokenUtilsService;
		this.cloudControllerUrl = cloudControllerUrl;
		this.applicationId = applicationId;
		this.restTemplate = restTemplate;
	}

	/**
	 * Returns {@code true} if the user (using the access-token from the authenticated user)
	 * has full {@link AccessLevel#FULL} for the provided
	 * {@code applicationId}
	 *
	 * @return true of the user is a space developer in Cloud Foundry
	 */
	public boolean isSpaceDeveloper() {
		final String accessToken = this.oauth2TokenUtilsService.getAccessTokenOfAuthenticatedUser();
		return isSpaceDeveloper(accessToken);
	}

	public boolean isSpaceDeveloper(String accessToken) {
		Assert.hasText(accessToken, "The accessToken must not be null or empty.");
		final AccessLevel accessLevel = getAccessLevel(
				accessToken, applicationId);

		if (AccessLevel.FULL.equals(accessLevel)) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Return the access level that should be granted to the given token.
	 * @param token the token
	 * @param applicationId the cloud foundry application ID
	 * @return the access level that should be granted
	 * @throws CloudFoundryAuthorizationException if the token is not authorized
	 */
	public AccessLevel getAccessLevel(String token, String applicationId)
			throws CloudFoundryAuthorizationException {
		try {
			final URI permissionsUri = getPermissionsUri(applicationId);
			logger.info("Using PermissionsUri: " + permissionsUri);
			RequestEntity<?> request = RequestEntity.get(permissionsUri)
					.header("Authorization", "bearer " + token).build();
			Map<?, ?> body = this.restTemplate.exchange(request, Map.class).getBody();
			if (Boolean.TRUE.equals(body.get("read_sensitive_data"))) {
				return AccessLevel.FULL;
			}
			else {
				return AccessLevel.RESTRICTED;
			}
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
				return AccessLevel.NONE;
			}
			// TODO GH-2627 - a class of the same name is in boot actuator 2.1. check for differnces
			throw new CloudFoundryAuthorizationException(CloudFoundryAuthorizationException.Reason.INVALID_TOKEN,
					"Invalid token", ex);
		}
		catch (HttpServerErrorException ex) {
			throw new CloudFoundryAuthorizationException(CloudFoundryAuthorizationException.Reason.SERVICE_UNAVAILABLE,
					"Cloud controller not reachable");
		}
	}

	private URI getPermissionsUri(String applicationId) {
		try {
			return new URI(this.cloudControllerUrl + "/v2/apps/" + applicationId
					+ "/permissions");
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
