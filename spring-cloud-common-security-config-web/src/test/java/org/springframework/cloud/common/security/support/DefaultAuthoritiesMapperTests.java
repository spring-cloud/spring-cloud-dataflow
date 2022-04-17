/*
 * Copyright 2017-2021 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.common.security.ProviderRoleMapping;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Gunnar Hillert
 */
public class DefaultAuthoritiesMapperTests {

	@Test
	public void testNullConstructor() throws Exception {
		assertThatThrownBy(() -> {
			new DefaultAuthoritiesMapper(null, "");
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("providerRoleMappings must not be null.");
	}

	@Test
	public void testMapScopesToAuthoritiesWithNullParameters() throws Exception {
		DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper(Collections.emptyMap(), "");

		assertThatThrownBy(() -> {
			authoritiesMapper.mapScopesToAuthorities(null, null, null);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The scopes argument must not be null.");
		assertThatThrownBy(() -> {
			authoritiesMapper.mapScopesToAuthorities("myClientId", null, null);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The scopes argument must not be null.");
	}

	@Test
	public void testThat7AuthoritiesAreReturned() throws Exception {
		DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper("uaa", false);
		Set<GrantedAuthority> authorities = authoritiesMapper.mapScopesToAuthorities("uaa", Collections.emptySet(), null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW", "ROLE_DEPLOY", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_DESTROY");
	}

	@Test
	public void testEmptyMapConstructor() throws Exception {
		Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper("uaa", true);
		Collection<? extends GrantedAuthority> authorities = authoritiesMapper.mapScopesToAuthorities("uaa", scopes, null);

		assertThat(authorities).hasSize(3);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW");
	}

	@Test
	public void testMapConstructorWithIncompleteRoleMappings() throws Exception {
		ProviderRoleMapping roleMapping = new ProviderRoleMapping();
		roleMapping.setMapOauthScopes(true);
		roleMapping.addRoleMapping("ROLE_MANAGE", "foo-scope-in-oauth");
		assertThatThrownBy(() -> {
			new DefaultAuthoritiesMapper("uaa", roleMapping);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
				"The following 6 roles are not mapped: CREATE, DEPLOY, DESTROY, MODIFY, SCHEDULE, VIEW.");
	}

	@Test
	public void testThat7MappedAuthoritiesAreReturned() throws Exception {
		Map<String, String> roleMappings = new HashMap<>();
		roleMappings.put("ROLE_MANAGE", "foo-manage");
		roleMappings.put("ROLE_VIEW", "bar-view");
		roleMappings.put("ROLE_CREATE", "blubba-create");
		roleMappings.put("ROLE_MODIFY", "foo-modify");
		roleMappings.put("ROLE_DEPLOY", "foo-deploy");
		roleMappings.put("ROLE_DESTROY", "foo-destroy");
		roleMappings.put("ROLE_SCHEDULE", "foo-schedule");

		ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);
		providerRoleMapping.getRoleMappings().putAll(roleMappings);

		Set<String> scopes = new HashSet<>();
		scopes.add("foo-manage");
		scopes.add("bar-view");
		scopes.add("blubba-create");
		scopes.add("foo-modify");
		scopes.add("foo-deploy");
		scopes.add("foo-destroy");
		scopes.add("foo-schedule");

		DefaultAuthoritiesMapper defaultAuthoritiesMapper = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesMapper.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}

	@Test
	public void testThat3MappedAuthoritiesAreReturnedForDefaultMapping() throws Exception {
		ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);

		Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(3);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW");
	}

	@Test
	public void testThat7MappedAuthoritiesAreReturnedForDefaultMappingWithoutMappingScopes() throws Exception {
		Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", false);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}

	@Test
	public void testThat2MappedAuthoritiesAreReturnedForDefaultMapping() throws Exception {
		Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", true);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(2);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_VIEW");
	}

	@Test
	public void testThat7AuthoritiesAreReturnedAndOneOAuthScopeCoversMultipleServerRoles() throws Exception {
		Map<String, String> roleMappings = new HashMap<>();
		roleMappings.put("ROLE_MANAGE", "foo-manage");
		roleMappings.put("ROLE_VIEW", "foo-manage");
		roleMappings.put("ROLE_DEPLOY", "foo-manage");
		roleMappings.put("ROLE_DESTROY", "foo-manage");
		roleMappings.put("ROLE_MODIFY", "foo-manage");
		roleMappings.put("ROLE_SCHEDULE", "foo-manage");
		roleMappings.put("ROLE_CREATE", "blubba-create");

		Set<String> scopes = new HashSet<>();
		scopes.add("foo-manage");
		scopes.add("blubba-create");

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", true, roleMappings);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}

	@Test
	public void testThatUriStyleScopeRemovesLeadingPart() throws Exception {
		Map<String, String> roleMappings = new HashMap<>();
		roleMappings.put("ROLE_MANAGE", "foo-manage");
		roleMappings.put("ROLE_VIEW", "foo-manage");
		roleMappings.put("ROLE_DEPLOY", "foo-manage");
		roleMappings.put("ROLE_DESTROY", "foo-manage");
		roleMappings.put("ROLE_MODIFY", "foo-manage");
		roleMappings.put("ROLE_SCHEDULE", "foo-manage");
		roleMappings.put("ROLE_CREATE", "blubba-create");

		Set<String> scopes = new HashSet<>();
		scopes.add("api://foobar/foo-manage");
		scopes.add("blubba-create");

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", true, roleMappings);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}

	@Test
	public void testThatUriStyleScopeParsingCanBeDisabled() throws Exception {
		Map<String, String> roleMappings = new HashMap<>();
		roleMappings.put("ROLE_MANAGE", "/ROLE/2000803042");
		roleMappings.put("ROLE_VIEW", "/ROLE/2000803036");
		roleMappings.put("ROLE_DEPLOY", "/ROLE/2000803039");
		roleMappings.put("ROLE_DESTROY", "/ROLE/20008030340");
		roleMappings.put("ROLE_MODIFY", "/ROLE/2000803037");
		roleMappings.put("ROLE_SCHEDULE", "/ROLE/2000803038");
		roleMappings.put("ROLE_CREATE", "/ROLE/2000803041");

		ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);
		providerRoleMapping.setParseOauthScopePathParts(false);
		providerRoleMapping.getRoleMappings().putAll(roleMappings);

		Set<String> scopes = new HashSet<>();
		scopes.add("/ROLE/2000803042");
		scopes.add("/ROLE/2000803036");
		scopes.add("/ROLE/2000803039");
		scopes.add("/ROLE/20008030340");
		scopes.add("/ROLE/2000803037");
		scopes.add("/ROLE/2000803038");
		scopes.add("/ROLE/2000803041");

		DefaultAuthoritiesMapper defaultAuthoritiesMapper = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesMapper.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}
}
