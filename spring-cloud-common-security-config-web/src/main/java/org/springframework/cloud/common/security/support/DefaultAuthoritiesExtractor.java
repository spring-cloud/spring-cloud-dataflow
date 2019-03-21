/*
 * Copyright 2017-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Default Spring Cloud {@link AuthoritiesExtractor}. Will assign ALL
 * {@link CoreSecurityRoles} to the authenticated OAuth2 user.
 *
 * @author Gunnar Hillert
 *
 */
public class DefaultAuthoritiesExtractor implements AuthoritiesExtractor {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DefaultAuthoritiesExtractor.class);

	private final boolean mapOauthScopesToAuthorities;
	private final Map<CoreSecurityRoles, String> roleMappings;
	private String rolePrefix = "ROLE_";
	private String oauthScopePrefix = "dataflow.";

	private final OAuth2RestOperations restTemplate;

	public DefaultAuthoritiesExtractor(boolean mapOauthScopesToAuthorities,
				Map<String, String> roleMappings,
				OAuth2RestOperations restTemplate) {
		super();
		this.mapOauthScopesToAuthorities = mapOauthScopesToAuthorities;
		this.roleMappings = prepareRoleMappings(roleMappings);
		this.restTemplate = restTemplate;
	}

	public DefaultAuthoritiesExtractor() {
		super();
		this.mapOauthScopesToAuthorities = false;
		this.roleMappings = prepareRoleMappings(null);
		this.restTemplate = null;
	}

	/**
	 * The returned {@link List} of {@link GrantedAuthority}s contains all roles from
	 * {@link CoreSecurityRoles}. The roles are prefixed with the value specified in
	 * {@link GrantedAuthorityDefaults}.
	 *
	 *
	 * @param  map Must not be null. Is only used for logging
	 */
	@Override
	public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
		Assert.notNull(map, "The map argument must not be null.");

		final List<String> rolesAsStrings = new ArrayList<>();

		final List<GrantedAuthority> grantedAuthorities;

		if (this.mapOauthScopesToAuthorities) {
			final Set<String> scopes = this.restTemplate.getAccessToken().getScope();
			grantedAuthorities = new ArrayList<>();

			if (scopes != null && !scopes.isEmpty()) {
				for (Map.Entry<CoreSecurityRoles, String> roleMappingEngtry : this.roleMappings.entrySet()) {
					final CoreSecurityRoles role = roleMappingEngtry.getKey();
					final String expectedOAuthScope = roleMappingEngtry.getValue();
					for (String scope : scopes) {
						if (scope.equalsIgnoreCase(expectedOAuthScope)) {
							final SimpleGrantedAuthority oauthRoleAuthority = new SimpleGrantedAuthority(this.rolePrefix + role.getKey());
							rolesAsStrings.add(oauthRoleAuthority.getAuthority());
							grantedAuthorities.add(oauthRoleAuthority);
						}
					}
				}
				logger.info("Adding roles {} to user {}", StringUtils.collectionToCommaDelimitedString(rolesAsStrings), map);
			}
		}
		else {
			grantedAuthorities =
					this.roleMappings.entrySet().stream().map(mapEntry -> {
						final String roleName = mapEntry.getKey().getKey();
						rolesAsStrings.add(roleName);
						return new SimpleGrantedAuthority(this.rolePrefix + mapEntry.getKey().getKey());
					}).collect(Collectors.toList());
			logger.info("Adding ALL roles {} to user {}", StringUtils.collectionToCommaDelimitedString(rolesAsStrings), map);
		}
		return grantedAuthorities;
	}

	/**
	 *
	 * @param rawRoleMappings
	 * @param mapOauthScopesToAuthorities
	 * @return
	 */
	private Map<CoreSecurityRoles, String> prepareRoleMappings(Map<String, String> rawRoleMappings) {

		final Map<CoreSecurityRoles, String> roleMappings = new HashMap<>(0);

		if (CollectionUtils.isEmpty(rawRoleMappings)) {
			for (CoreSecurityRoles roleEnum : CoreSecurityRoles.values()) {
				final String roleName = this.oauthScopePrefix + roleEnum.getKey();
				roleMappings.put(roleEnum, roleName);
			}
			return roleMappings;
		}

		final List<CoreSecurityRoles> unmappedRoles = new ArrayList<>(0);

		for (CoreSecurityRoles coreRole : CoreSecurityRoles.values()) {

			final String coreSecurityRoleName;
			if (this.rolePrefix.length() > 0 && !coreRole.getKey().startsWith(rolePrefix)) {
				coreSecurityRoleName = rolePrefix + coreRole.getKey();
			}
			else {
				coreSecurityRoleName = coreRole.getKey();
			}

			final String oauthScope = rawRoleMappings.get(coreSecurityRoleName);

			if (oauthScope == null) {
				unmappedRoles.add(coreRole);
			}
			else {
				roleMappings.put(coreRole, oauthScope);
			}
		}

		if (!unmappedRoles.isEmpty()) {
			throw new IllegalArgumentException(
				String.format("The following %s %s not mapped: %s.",
					unmappedRoles.size(),
					unmappedRoles.size() > 1 ? "roles are" : "role is",
					StringUtils.collectionToDelimitedString(unmappedRoles, ", ")));
		}

		return roleMappings;
	}

	/**
	 * Sets the prefix which should be added to the authority name (if it doesn't already
	 * exist)
	 *
	 */
	public void setRolePrefix(String rolePrefix) {
		Assert.notNull(rolePrefix, "rolePrefix cannot be null");
		this.rolePrefix = rolePrefix;
	}
}
