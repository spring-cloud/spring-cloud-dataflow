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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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
 * @since 1.3.0
 */
public class DefaultAuthoritiesMapper implements AuthoritiesMapper {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DefaultAuthoritiesMapper.class);

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
	 * @param clientId Must not be empty or null
	 * @param scopes Must not be null
	 */
	@Override
	public Set<GrantedAuthority> mapScopesToAuthorities(String clientId, Set<String> scopes) {
		Assert.hasText(clientId, "The clientId argument must not be empty or null.");
		Assert.notNull(scopes, "The scopes argument must not be null.");

		final ProviderRoleMapping roleMapping = this.providerRoleMappings.get(clientId);

		if (roleMapping == null) {
			throw new IllegalArgumentException("No role mapping found for clientId " + clientId);
		}

		final List<String> rolesAsStrings = new ArrayList<>();

		final Set<GrantedAuthority> grantedAuthorities;

		if (roleMapping.isMapOauthScopes()) {
			grantedAuthorities = new HashSet<>();

			if (!scopes.isEmpty()) {
				for (Map.Entry<CoreSecurityRoles, String> roleMappingEngtry : roleMapping.convertRoleMappingKeysToCoreSecurityRoles().entrySet()) {
					final CoreSecurityRoles role = roleMappingEngtry.getKey();
					final String expectedOAuthScope = roleMappingEngtry.getValue();
					for (String scope : scopes) {
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
		else {
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
	public Set<GrantedAuthority> mapScopesToAuthorities(Set<String> scopes) {
		return this.mapScopesToAuthorities(this.defaultProviderId, scopes);
	}
}
