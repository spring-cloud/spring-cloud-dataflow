/*
 * Copyright 2019-2021 the original author or authors.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.common.security.ProviderRoleMapping;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default {@link AuthoritiesMapper}.
 *
 * @author Gunnar Hillert
 * @author Janne Valkealahti
 */
public class DefaultAuthoritiesMapper implements AuthoritiesMapper {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAuthoritiesMapper.class);
	private final Map<String, ProviderRoleMapping> providerRoleMappings;
	private final String defaultProviderId;

	public DefaultAuthoritiesMapper(Map<String, ProviderRoleMapping> providerRoleMappings, String defaultProviderId) {
		super();

		Assert.notNull(providerRoleMappings, "providerRoleMappings must not be null.");
		for (Entry<String, ProviderRoleMapping>  providerRoleMappingToValidate : providerRoleMappings.entrySet()) {
			providerRoleMappingToValidate.getValue().convertRoleMappingKeysToCoreSecurityRoles();
		}

		this.providerRoleMappings = providerRoleMappings;
		this.defaultProviderId = defaultProviderId;
	}

	/**
	 * Convenience constructor that will create a {@link DefaultAuthoritiesMapper} with a
	 * single {@link ProviderRoleMapping}.
	 *
	 * @param providerId Create a ProviderRoleMapping with the specified providerId
	 * @param mapOAuthScopes Shall OAuth scopes be considered?
	 * @param roleMappings Used to populate the ProviderRoleMapping
	 */
	public DefaultAuthoritiesMapper(String providerId, boolean mapOAuthScopes, Map<String, String> roleMappings) {
		Assert.hasText(providerId, "The providerId must not be null or empty.");
		final ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping(mapOAuthScopes, roleMappings);
		this.providerRoleMappings = new HashMap<String, ProviderRoleMapping>(1);
		this.providerRoleMappings.put(providerId, providerRoleMapping);
		for (ProviderRoleMapping providerRoleMappingToValidate : providerRoleMappings.values()) {
			providerRoleMappingToValidate.convertRoleMappingKeysToCoreSecurityRoles();
		}
		this.defaultProviderId = providerId;
	}

	/**
	 * Convenience constructor that will create a {@link DefaultAuthoritiesMapper} with a
	 * single {@link ProviderRoleMapping}.
	 *
	 * @param providerId The provider id for the ProviderRoleMapping
	 * @param mapOAuthScopes Consider scopes?
	 */
	public DefaultAuthoritiesMapper(String providerId, boolean mapOAuthScopes) {
		Assert.hasText(providerId, "The providerId must not be null or empty.");
		final ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping(mapOAuthScopes);
		this.providerRoleMappings = new HashMap<String, ProviderRoleMapping>(1);
		this.providerRoleMappings.put(providerId, providerRoleMapping);
		for (ProviderRoleMapping providerRoleMappingToValidate : providerRoleMappings.values()) {
			providerRoleMappingToValidate.convertRoleMappingKeysToCoreSecurityRoles();
		}
		this.defaultProviderId = providerId;
	}

	/**
	 * Convenience constructor that will create a {@link DefaultAuthoritiesMapper} with a
	 * single {@link ProviderRoleMapping}.
	 *
	 * @param providerId The provider id for the ProviderRoleMapping
	 * @param providerRoleMapping The role mappings to add to the {@link ProviderRoleMapping}
	 */
	public DefaultAuthoritiesMapper(String providerId, ProviderRoleMapping providerRoleMapping) {
		this.providerRoleMappings = new HashMap<String, ProviderRoleMapping>(1);
		this.providerRoleMappings.put(providerId, providerRoleMapping);
		for (ProviderRoleMapping providerRoleMappingToValidate : providerRoleMappings.values()) {
			providerRoleMappingToValidate.convertRoleMappingKeysToCoreSecurityRoles();
		}
		this.defaultProviderId = providerId;
	}

	/**
	 * The returned {@link List} of {@link GrantedAuthority}s contains all roles from
	 * {@link CoreSecurityRoles}. The roles are prefixed with the value specified in
	 * {@link GrantedAuthorityDefaults}.
	 *
	 * @param clientIdParam If null, the default defaultProviderId is used
	 * @param scopes Must not be null
	 * @param token Ignored in this implementation
	 */
	@Override
	public Set<GrantedAuthority> mapScopesToAuthorities(String clientIdParam, Set<String> scopes, String token) {
		logger.debug("Mapping scopes to authorities");
		final String clientId;
		if (clientIdParam == null) {
			clientId = this.defaultProviderId;
		}
		else {
			clientId = clientIdParam;
		}
		Assert.notNull(scopes, "The scopes argument must not be null.");

		final ProviderRoleMapping roleMapping = this.providerRoleMappings.get(clientId);

		if (roleMapping == null) {
			throw new IllegalArgumentException("No role mapping found for clientId " + clientId);
		}

		final List<String> rolesAsStrings = new ArrayList<>();

		Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

		if (roleMapping.isMapOauthScopes()) {
			if (!scopes.isEmpty()) {
				for (Map.Entry<CoreSecurityRoles, String> roleMappingEngtry : roleMapping.convertRoleMappingKeysToCoreSecurityRoles().entrySet()) {
					final CoreSecurityRoles role = roleMappingEngtry.getKey();
					final String expectedOAuthScope = roleMappingEngtry.getValue();
					for (String scope : pathParts(scopes)) {
						if (scope.equalsIgnoreCase(expectedOAuthScope)) {
							final SimpleGrantedAuthority oauthRoleAuthority = new SimpleGrantedAuthority(roleMapping.getRolePrefix() + role.getKey());
							rolesAsStrings.add(oauthRoleAuthority.getAuthority());
							grantedAuthorities.add(oauthRoleAuthority);
						}
					}
				}
				logger.info("Adding roles: {}.", StringUtils.collectionToCommaDelimitedString(rolesAsStrings));
			}
		}
		else if (!roleMapping.isMapGroupClaims()) {
			grantedAuthorities =
					roleMapping.convertRoleMappingKeysToCoreSecurityRoles().entrySet().stream().map(mapEntry -> {
						final CoreSecurityRoles role = mapEntry.getKey();
						rolesAsStrings.add(role.getKey());
						return new SimpleGrantedAuthority(roleMapping.getRolePrefix() + mapEntry.getKey());
					}).collect(Collectors.toSet());
			logger.info("Adding ALL roles: {}.", StringUtils.collectionToCommaDelimitedString(rolesAsStrings));
		}
		return grantedAuthorities;
	}

	@Override
	public Set<GrantedAuthority> mapClaimsToAuthorities(String clientIdParam, List<String> claims) {
		logger.debug("Mapping claims to authorities");
		final String clientId;
		if (clientIdParam == null) {
			clientId = this.defaultProviderId;
		}
		else {
			clientId = clientIdParam;
		}

		final ProviderRoleMapping groupMapping = this.providerRoleMappings.get(clientId);
		if (groupMapping == null) {
			throw new IllegalArgumentException("No role mapping found for clientId " + clientId);
		}

		final List<String> rolesAsStrings = new ArrayList<>();
		final Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

		if (groupMapping.isMapGroupClaims()) {
			if (!claims.isEmpty()) {
				for (Map.Entry<CoreSecurityRoles, String> roleMappingEngtry : groupMapping.convertGroupMappingKeysToCoreSecurityRoles().entrySet()) {
					final CoreSecurityRoles role = roleMappingEngtry.getKey();
					final String expectedOAuthScope = roleMappingEngtry.getValue();
					logger.debug("Checking group mapping {} {}", role, expectedOAuthScope);
					for (String claim : claims) {
						logger.debug("Checking against claim {} {}", claim, expectedOAuthScope);
						if (claim.equalsIgnoreCase(expectedOAuthScope)) {
							final SimpleGrantedAuthority oauthRoleAuthority = new SimpleGrantedAuthority(groupMapping.getRolePrefix() + role.getKey());
							rolesAsStrings.add(oauthRoleAuthority.getAuthority());
							grantedAuthorities.add(oauthRoleAuthority);
							logger.debug("Adding to granted authorities {}", oauthRoleAuthority);
						}
					}
				}
				logger.info("Adding groups: {}.", StringUtils.collectionToCommaDelimitedString(rolesAsStrings));
			}
		}

		return grantedAuthorities;
	}

	private Set<String> pathParts(Set<String> scopes) {
		// String away leading part if scope is something like
		// api://dataflow-server/dataflow.create resulting dataflow.create
		return scopes.stream().map(scope -> {
			try {
				URI uri = URI.create(scope);
				String path = uri.getPath();
				if (StringUtils.hasText(path) && path.charAt(0) == '/') {
					return path.substring(1);
				}
			} catch (Exception e) {
			}
			return scope;
		})
		.collect(Collectors.toSet());
	}
}
