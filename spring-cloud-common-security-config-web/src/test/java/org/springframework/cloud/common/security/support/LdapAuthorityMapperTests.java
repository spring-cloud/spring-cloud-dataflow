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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author Gunnar Hillert
 */
public class LdapAuthorityMapperTests {

	@Test
	public void testNullMapConstructor() throws Exception {
		try {
			new LdapAuthorityMapper(null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The provided roleMappings must neither be null nor empty.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testEmptyMapConstructor() throws Exception {
		try {
			new LdapAuthorityMapper(new HashMap<>());
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The provided roleMappings must neither be null nor empty.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testMapConstructorWithIncompleteRoleMappings() throws Exception {
		try {
			new LdapAuthorityMapper(Collections.singletonMap("ROLE_MANAGE", "foo-role-in-ldap"));
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The following 2 roles are not mapped: VIEW,CREATE.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testThat3AuthoritiesAreReturned() throws Exception {

		final Map<String, String> roleMappings = new HashMap<>();

		roleMappings.put("ROLE_MANAGE", "foo-manage");
		roleMappings.put("ROLE_VIEW", "bar-view");
		roleMappings.put("ROLE_CREATE", "blubba-create");

		final LdapAuthorityMapper authorityMapper = new LdapAuthorityMapper(roleMappings);

		final Collection<SimpleGrantedAuthority> ldapAuthoritiesToMap = new ArrayList<>(3);
		ldapAuthoritiesToMap.add(new SimpleGrantedAuthority("role_foo-manage"));
		ldapAuthoritiesToMap.add(new SimpleGrantedAuthority("ROLE_bar-view"));
		ldapAuthoritiesToMap.add(new SimpleGrantedAuthority("role_blubba-create"));

		final Collection<? extends GrantedAuthority> authorities = authorityMapper.mapAuthorities(ldapAuthoritiesToMap);
		assertThat(authorities, hasSize(3));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW"));
	}

	@Test
	public void testThat3AuthoritiesAreReturnedUppercase() throws Exception {

		final Map<String, String> roleMappings = new HashMap<>();

		roleMappings.put("ROLE_MANAGE", "FOO-MANAGE");
		roleMappings.put("ROLE_VIEW", "BAR-VIEW");
		roleMappings.put("ROLE_CREATE", "BLUBBA-CREATE");

		final LdapAuthorityMapper authorityMapper = new LdapAuthorityMapper(roleMappings);

		final Collection<SimpleGrantedAuthority> ldapAuthoritiesToMap = new ArrayList<>(3);
		ldapAuthoritiesToMap.add(new SimpleGrantedAuthority("role_foo-manage"));
		ldapAuthoritiesToMap.add(new SimpleGrantedAuthority("role_BAR-VIEW"));
		ldapAuthoritiesToMap.add(new SimpleGrantedAuthority("role_blubba-create"));

		final Collection<? extends GrantedAuthority> authorities = authorityMapper.mapAuthorities(ldapAuthoritiesToMap);
		assertThat(authorities, hasSize(3));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW"));
	}

	@Test
	public void testThat3AuthoritiesAreReturnedAndOneLdapRoleCoversMultipleServerRoles() throws Exception {

		final Map<String, String> roleMappings = new HashMap<>();

		roleMappings.put("ROLE_MANAGE", "FOO-MANAGE");
		roleMappings.put("ROLE_VIEW", "FOO-MANAGE");
		roleMappings.put("ROLE_CREATE", "BLUBBA-CREATE");

		final LdapAuthorityMapper authorityMapper = new LdapAuthorityMapper(roleMappings);

		final Collection<SimpleGrantedAuthority> ldapAuthoritiesToMap = new ArrayList<>(3);
		ldapAuthoritiesToMap.add(new SimpleGrantedAuthority("role_foo-manage"));
		ldapAuthoritiesToMap.add(new SimpleGrantedAuthority("role_blubba-create"));

		final Collection<? extends GrantedAuthority> authorities = authorityMapper.mapAuthorities(ldapAuthoritiesToMap);
		assertThat(authorities, hasSize(3));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW"));
	}

}
