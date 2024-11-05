/*
 * Copyright 2018-2021 the original author or authors.
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

import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * {@link AuthoritiesMapper} that looks up
 * {@link CoreSecurityRoles} from an external HTTP resource. Requests to the
 * external HTTP resource are authenticated by forwarding the user's access
 * token. The external resource's response body MUST be a JSON array
 * containing strings with values corresponding to
 * {@link CoreSecurityRoles#key} values. For example, a response containing
 * {@code ["VIEW", "CREATE"]} would grant the user
 * {@code ROLE_VIEW, ROLE_CREATE},
 *
 * @author Mike Heath
 * @author Gunnar Hillert
 */
public class ExternalOauth2ResourceAuthoritiesMapper implements AuthoritiesMapper {

	private static final Logger logger = LoggerFactory.getLogger(ExternalOauth2ResourceAuthoritiesMapper.class);

	public static final GrantedAuthority CREATE = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.CREATE.getKey());
	public static final GrantedAuthority DEPLOY = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.DEPLOY.getKey());
	public static final GrantedAuthority DESTROY = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.DESTROY.getKey());
	public static final GrantedAuthority MANAGE = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.MANAGE.getKey());
	public static final GrantedAuthority MODIFY = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.MODIFY.getKey());
	public static final GrantedAuthority SCHEDULE = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.SCHEDULE.getKey());
	public static final GrantedAuthority VIEW = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.VIEW.getKey());

	private final URI roleProviderUri;
	private final RestOperations restOperations;

	/**
	 *
	 * @param roleProviderUri a HTTP GET request is sent to this URI to fetch
	 *                        the user's security roles
	 */
	public ExternalOauth2ResourceAuthoritiesMapper(
		URI roleProviderUri) {
		Assert.notNull(roleProviderUri, "The provided roleProviderUri must not be null.");
		this.roleProviderUri = roleProviderUri;

		final RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
		this.restOperations = restTemplate;
	}


	@Override
	public Set<GrantedAuthority> mapScopesToAuthorities(String providerId, Set<String> scopes, String token) {
		logger.debug("Getting permissions from {}", roleProviderUri);

		final HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.AUTHORIZATION, OAuth2AccessToken.TokenType.BEARER.getValue() + " " + token);

		final HttpEntity<String> entity = new HttpEntity<>(null, headers);
		final ResponseEntity<String[]> response = restOperations.exchange(roleProviderUri, HttpMethod.GET, entity, String[].class);

		final Set<GrantedAuthority> authorities = new HashSet<>();
		for (String permission : response.getBody()) {
			if (!StringUtils.hasText(permission)) {
				logger.warn("Received an empty permission from {}", roleProviderUri);
			} else {
				final CoreSecurityRoles securityRole = CoreSecurityRoles.fromKey(permission.toUpperCase(Locale.ROOT));
				if (securityRole == null) {
					logger.warn("Invalid role {} provided by {}", permission, roleProviderUri);
				} else {
					switch (securityRole) {
						case CREATE:
							authorities.add(CREATE);
							break;
						case DEPLOY:
							authorities.add(DEPLOY);
							break;
						case DESTROY:
							authorities.add(DESTROY);
							break;
						case MANAGE:
							authorities.add(MANAGE);
							break;
						case MODIFY:
							authorities.add(MODIFY);
							break;
						case SCHEDULE:
							authorities.add(SCHEDULE);
							break;
						case VIEW:
							authorities.add(VIEW);
							break;
					}
				}
			}
		}
		logger.info("Roles added for user: {}.", authorities);
		return authorities;
	}
}

