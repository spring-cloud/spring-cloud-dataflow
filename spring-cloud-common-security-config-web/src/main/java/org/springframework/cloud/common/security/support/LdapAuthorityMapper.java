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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of a {@link GrantedAuthoritiesMapper} that maps Ldap roles
 * to the {@link CoreSecurityRoles}. When mapping roles, the case of the role name is ignored. One Ldap role
 * can be mapped to multiple Spring Cloud application roles.
 *
 * @author Gunnar Hillert
 *
 */
public class LdapAuthorityMapper implements GrantedAuthoritiesMapper{

	private final Map<CoreSecurityRoles, SimpleGrantedAuthority> roleMappings = new HashMap<>(0);
	private String rolePrefix = "ROLE_";

	/**
	 * Constructor of the LdapAuthorityMapper.
	 *
	 * @param roleMappings Must not be null or empty.
	 * @throws IllegalArgumentException if not all CoreSecurityRoles are mapped.
	 */
	public LdapAuthorityMapper(Map<String, String> roleMappings) {
		super();
		Assert.notEmpty(roleMappings, "The provided roleMappings must neither be null nor empty.");

		final List<CoreSecurityRoles> unmappedRoles = new ArrayList<>(0);

		for (CoreSecurityRoles coreRole : CoreSecurityRoles.values()) {

			final String coreSecurityRoleName;
			if (this.rolePrefix.length() > 0 && !coreRole.getKey().startsWith(rolePrefix)) {
				coreSecurityRoleName = rolePrefix + coreRole.getKey();
			}
			else {
				coreSecurityRoleName = coreRole.getKey();
			}

			final String ldapRole = roleMappings.get(coreSecurityRoleName);

			if (ldapRole == null) {
				unmappedRoles.add(coreRole);
			}
			else {
				final SimpleGrantedAuthority ldapRoleAuthority = new SimpleGrantedAuthority(this.rolePrefix + ldapRole);
				this.roleMappings.put(coreRole, ldapRoleAuthority);
			}
		}

		if (!unmappedRoles.isEmpty()) {
			throw new IllegalArgumentException(
				String.format("The following %s %s not mapped: %s.",
					unmappedRoles.size(),
					unmappedRoles.size() > 1 ? "roles are" : "role is",
					StringUtils.collectionToCommaDelimitedString(unmappedRoles)));
		}

	}

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		final Set<GrantedAuthority> authoritiesToReturn = authorities.stream()
				.map(authority -> mapAuthority(authority))
				.flatMap(Collection::stream)
				.filter(authority -> authority != null)
				.collect(Collectors.toSet());
		return authoritiesToReturn;
	}

	private Set<SimpleGrantedAuthority> mapAuthority(final GrantedAuthority ldapRoleAuthority) {
		final Set<SimpleGrantedAuthority> authorities = roleMappings.entrySet().stream()
			.filter(roleMapEntry -> ldapRoleAuthority.getAuthority().equalsIgnoreCase(roleMapEntry.getValue().getAuthority()))
			.map(roleMapEntry -> new SimpleGrantedAuthority(this.rolePrefix + roleMapEntry.getKey().getKey())).collect(Collectors.toSet());
		return authorities;
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
