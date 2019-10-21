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
package org.springframework.cloud.common.security.support;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.cloud.common.security.ProviderRoleMapping;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Gunnar Hillert
 */
public class DefaultAuthoritiesMapperTests {

	@Test
	public void testNullConstructor() throws Exception {
		try {
			new DefaultAuthoritiesMapper(null, "");
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("providerRoleMappings must not be null.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testMapScopesToAuthoritiesWithNullParameters() throws Exception {
		final DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper(Collections.emptyMap(), "");
		try {
			authoritiesMapper.mapScopesToAuthorities(null, null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The clientId argument must not be empty or null.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testMapScopesToAuthoritiesWithNullParameters2() throws Exception {
		final DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper(Collections.emptyMap(), "");
		try {
			authoritiesMapper.mapScopesToAuthorities("myClientId", null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The scopes argument must not be null.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalStateException to be thrown.");
	}


	@Test
	public void testThat7AuthoritiesAreReturned() throws Exception {
		final DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper("uaa", false);

		final Set<GrantedAuthority> authorities = authoritiesMapper.mapScopesToAuthorities("uaa", Collections.emptySet());
		assertThat(authorities, hasSize(7));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW", "ROLE_DEPLOY", "ROLE_MODIFY", "ROLE_SCHEDULE", "ROLE_DESTROY"));
	}

	@Test
	public void testEmptyMapConstructor() throws Exception {
		final Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		final DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper("uaa", true);

		final Collection<? extends GrantedAuthority> authorities = authoritiesMapper.mapScopesToAuthorities("uaa", scopes);
		assertThat(authorities, hasSize(3));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW"));
	}

	@Test
	public void testMapConstructorWithIncompleteRoleMappings() throws Exception {
		final ProviderRoleMapping roleMapping = new ProviderRoleMapping();
		roleMapping.setMapOauthScopes(true);
		roleMapping.addRoleMapping("ROLE_MANAGE", "foo-scope-in-oauth");

		try {
			new DefaultAuthoritiesMapper("uaa", roleMapping);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The following 6 roles are not mapped: CREATE, DEPLOY, DESTROY, MODIFY, SCHEDULE, VIEW.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testThat7MappedAuthoritiesAreReturned() throws Exception {

		final Map<String, String> roleMappings = new HashMap<>();

		roleMappings.put("ROLE_MANAGE", "foo-manage");
		roleMappings.put("ROLE_VIEW", "bar-view");
		roleMappings.put("ROLE_CREATE", "blubba-create");

		roleMappings.put("ROLE_MODIFY", "foo-modify");
		roleMappings.put("ROLE_DEPLOY", "foo-deploy");
		roleMappings.put("ROLE_DESTROY", "foo-destroy");
		roleMappings.put("ROLE_SCHEDULE", "foo-schedule");

		final ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);
		providerRoleMapping.getRoleMappings().putAll(roleMappings);

		final Set<String> scopes = new HashSet<>();
		scopes.add("foo-manage");
		scopes.add("bar-view");
		scopes.add("blubba-create");
		scopes.add("foo-modify");
		scopes.add("foo-deploy");
		scopes.add("foo-destroy");
		scopes.add("foo-schedule");

		final DefaultAuthoritiesMapper defaultAuthoritiesMapper = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesMapper.mapScopesToAuthorities("uaa", scopes);

		assertThat(authorities, hasSize(7));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY", "ROLE_SCHEDULE", "ROLE_VIEW"));
	}

	@Test
	public void testThat3MappedAuthoritiesAreReturnedForDefaultMapping() throws Exception {

		final ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);

		final Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		final DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa", scopes);

		assertThat(authorities, hasSize(3));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW"));
	}

	@Test
	public void testThat7MappedAuthoritiesAreReturnedForDefaultMappingWithoutMappingScopes() throws Exception {

		final ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(false);

		final Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		final DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", false);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa", scopes);

		assertThat(authorities, hasSize(7));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY", "ROLE_SCHEDULE", "ROLE_VIEW"));
	}

	@Test
	public void testThat2MappedAuthoritiesAreReturnedForDefaultMapping() throws Exception {

		final Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		final DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", true);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa", scopes);

		assertThat(authorities, hasSize(2));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_CREATE", "ROLE_VIEW"));
	}

	@Test
	public void testThat7AuthoritiesAreReturnedAndOneOAuthScopeCoversMultipleServerRoles() throws Exception {
		final Map<String, String> roleMappings = new HashMap<>();

		roleMappings.put("ROLE_MANAGE", "foo-manage");
		roleMappings.put("ROLE_VIEW", "foo-manage");
		roleMappings.put("ROLE_DEPLOY", "foo-manage");
		roleMappings.put("ROLE_DESTROY", "foo-manage");
		roleMappings.put("ROLE_MODIFY", "foo-manage");
		roleMappings.put("ROLE_SCHEDULE", "foo-manage");
		roleMappings.put("ROLE_CREATE", "blubba-create");

		final Set<String> scopes = new HashSet<>();
		scopes.add("foo-manage");
		scopes.add("blubba-create");

		final DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", true, roleMappings);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa", scopes);

		assertThat(authorities, hasSize(7));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY", "ROLE_SCHEDULE", "ROLE_VIEW"));
	}

}
