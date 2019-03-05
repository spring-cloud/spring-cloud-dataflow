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
package org.springframework.cloud.common.security.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.StringUtils;

/**
 * Spring Cloud {@link AuthoritiesExtractor} that looks up
 * {@link CoreSecurityRoles} from an external HTTP resource. Requests to the
 * external HTTP resource are authenticated by forwarding the user's access
 * token. The external resource's response body MUST be a JSON array
 * containing strings with values corresponding to
 * {@link CoreSecurityRoles#key} values. For example, a response containing
 * {@code ["VIEW", "CREATE"]} would grant the user
 * {@code ROLE_VIEW, ROLE_CREATE},
 *
 * @author Mike Heath
 */
public class ExternalOauth2ResourceAuthoritiesExtractor implements AuthoritiesExtractor {

	private static final Logger logger = LoggerFactory.getLogger(ExternalOauth2ResourceAuthoritiesExtractor.class);

	public static final GrantedAuthority CREATE = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.CREATE.getKey());
	public static final GrantedAuthority DEPLOY = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.DEPLOY.getKey());
	public static final GrantedAuthority DESTROY = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.DESTROY.getKey());
	public static final GrantedAuthority MANAGE = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.MANAGE.getKey());
	public static final GrantedAuthority MODIFY = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.MODIFY.getKey());
	public static final GrantedAuthority SCHEDULE = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.SCHEDULE.getKey());
	public static final GrantedAuthority VIEW = new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + CoreSecurityRoles.VIEW.getKey());

	private final OAuth2RestTemplate restTemplate;
	private final URI roleProviderUri;

	/**
	 *
	 * @param restTemplate used for acquiring the user's access token and
	 *                     requesting the user's security roles
	 * @param roleProviderUri a HTTP GET request is sent to this URI to fetch
	 *                        the user's security roles
	 */
	public ExternalOauth2ResourceAuthoritiesExtractor(OAuth2RestTemplate restTemplate, URI roleProviderUri) {
		this.restTemplate = restTemplate;
		this.roleProviderUri = roleProviderUri;
	}

	@Override
	public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
		logger.debug("Getting permissions from {}", roleProviderUri);
		final OAuth2AccessToken token = restTemplate.getAccessToken();
		RequestEntity<?> request = RequestEntity.get(roleProviderUri)
				.header("Authorization", "bearer " + token.getValue()).build();
		final ResponseEntity<String[]> entity = restTemplate.exchange(request, String[].class);

		final List<GrantedAuthority> authorities = new ArrayList<>();
		for (String permission : entity.getBody()) {
			if (StringUtils.isEmpty(permission)) {
				logger.warn("Received an empty permission from {}", roleProviderUri);
			} else {
				final CoreSecurityRoles securityRole = CoreSecurityRoles.fromKey(permission.toUpperCase());
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
		logger.info("Roles {} add for user {}", authorities, map);
		return authorities;
	}
}

