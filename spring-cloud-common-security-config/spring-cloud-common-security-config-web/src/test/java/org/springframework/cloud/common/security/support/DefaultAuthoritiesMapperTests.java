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
class DefaultAuthoritiesMapperTests {

	@Test
	void nullConstructor() throws Exception {
		assertThatThrownBy(() -> {
			new DefaultAuthoritiesMapper(null, "");
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("providerRoleMappings must not be null.");
	}

	@Test
	void mapScopesToAuthoritiesWithNullParameters() throws Exception {
		DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper(Collections.emptyMap(), "");

		assertThatThrownBy(() -> {
			authoritiesMapper.mapScopesToAuthorities(null, null, null);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The scopes argument must not be null.");
		assertThatThrownBy(() -> {
			authoritiesMapper.mapScopesToAuthorities("myClientId", null, null);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The scopes argument must not be null.");
	}

	@Test
	void that7AuthoritiesAreReturned() throws Exception {
		DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper("uaa", false);
		Set<GrantedAuthority> authorities = authoritiesMapper.mapScopesToAuthorities("uaa", Collections.emptySet(), null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW", "ROLE_DEPLOY", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_DESTROY");
	}

	@Test
	void emptyMapConstructor() throws Exception {
		Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		DefaultAuthoritiesMapper authoritiesMapper = new DefaultAuthoritiesMapper("uaa", true);
		Collection<? extends GrantedAuthority> authorities = authoritiesMapper.mapScopesToAuthorities("uaa", scopes, null);

		assertThat(authorities).hasSize(3);
		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW");
	}

	@Test
	void mapConstructorWithIncompleteRoleMappings() throws Exception {
		ProviderRoleMapping roleMapping = new ProviderRoleMapping();
		roleMapping.setMapOauthScopes(true);
		roleMapping.addRoleMapping("ROLE_MANAGE", "foo-scope-in-oauth");
		assertThatThrownBy(() -> {
			new DefaultAuthoritiesMapper("uaa", roleMapping);
		}).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
				"The following 6 roles are not mapped: CREATE, DEPLOY, DESTROY, MODIFY, SCHEDULE, VIEW.");
	}

	@Test
	void that3MappedAuthoritiesAreReturned() throws Exception {
		Map<String, String> roleMappings = Map.of(
			"ROLE_MANAGE", "dataflow_manage",
			"ROLE_VIEW", "dataflow_view",
			"ROLE_CREATE", "dataflow_create",
			"ROLE_MODIFY", "dataflow_modify",
			"ROLE_DEPLOY", "dataflow_deploy",
			"ROLE_DESTROY", "dataflow_destroy",
			"ROLE_SCHEDULE", "dataflow_schedule"
		);

		ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);
		providerRoleMapping.getRoleMappings().putAll(roleMappings);

		Set<String> roles = Set.of("dataflow_manage", "dataflow_view", "dataflow_deploy");

		DefaultAuthoritiesMapper defaultAuthoritiesMapper = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesMapper.mapScopesToAuthorities("uaa",
				roles, null);

		assertThat(authorities).hasSize(3);
		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_DEPLOY", "ROLE_MANAGE", "ROLE_VIEW");
	}
	@Test
	void that7MappedAuthoritiesAreReturned() throws Exception {
		Map<String, String> roleMappings = Map.of(
		"ROLE_MANAGE", "foo-manage",
		"ROLE_VIEW", "bar-view",
		"ROLE_CREATE", "blubba-create",
		"ROLE_MODIFY", "foo-modify",
		"ROLE_DEPLOY", "foo-deploy",
		"ROLE_DESTROY", "foo-destroy",
		"ROLE_SCHEDULE", "foo-schedule"
		);

		ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);
		providerRoleMapping.getRoleMappings().putAll(roleMappings);

		Set<String> scopes = Set.of(
			"foo-manage",
			"bar-view",
			"blubba-create",
			"foo-modify",
			"foo-deploy",
			"foo-destroy",
			"foo-schedule"
		);

		DefaultAuthoritiesMapper defaultAuthoritiesMapper = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesMapper.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}

	@Test
	void that3MappedAuthoritiesAreReturnedForDefaultMapping() throws Exception {
		ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);

		Set<String> scopes = Set.of(
			"dataflow.manage",
			"dataflow.view",
			"dataflow.create"
		);

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(3);
		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW");
	}

	@Test
	void that7MappedAuthoritiesAreReturnedForDefaultMappingWithoutMappingScopes() throws Exception {
		Set<String> scopes = Set.of(
			"dataflow.manage",
			"dataflow.view",
			"dataflow.create"
		);

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", false);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}

	@Test
	void that2MappedAuthoritiesAreReturnedForDefaultMapping() throws Exception {
		Set<String> scopes = Set.of(
			"dataflow.view",
			"dataflow.create"
		);

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", true);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(2);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_VIEW");
	}

	@Test
	void that7AuthoritiesAreReturnedAndOneOAuthScopeCoversMultipleServerRoles() throws Exception {
		Map<String, String> roleMappings = Map.of(
			"ROLE_MANAGE", "foo-manage",
			"ROLE_VIEW", "foo-manage",
			"ROLE_DEPLOY", "foo-manage",
			"ROLE_DESTROY", "foo-manage",
			"ROLE_MODIFY", "foo-manage",
			"ROLE_SCHEDULE", "foo-manage",
			"ROLE_CREATE", "blubba-create"
		);

		Set<String> scopes = Set.of("foo-manage", "blubba-create");

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", true, roleMappings);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()))
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}

	@Test
	void thatUriStyleScopeRemovesLeadingPart() throws Exception {
		Map<String, String> roleMappings = Map.of(
			"ROLE_MANAGE", "foo-manage",
			"ROLE_VIEW", "foo-manage",
			"ROLE_DEPLOY", "foo-manage",
			"ROLE_DESTROY", "foo-manage",
			"ROLE_MODIFY", "foo-manage",
			"ROLE_SCHEDULE", "foo-manage",
			"ROLE_CREATE", "blubba-create"
		);

		Set<String> scopes = Set.of("api://foobar/foo-manage", "blubba-create");

		DefaultAuthoritiesMapper defaultAuthoritiesExtractor = new DefaultAuthoritiesMapper("uaa", true, roleMappings);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}

	@Test
	void thatUriStyleScopeParsingCanBeDisabled() throws Exception {
		Map<String, String> roleMappings = Map.of(
			"ROLE_MANAGE", "/ROLE/2000803042",
			"ROLE_VIEW", "/ROLE/2000803036",
			"ROLE_DEPLOY", "/ROLE/2000803039",
			"ROLE_DESTROY", "/ROLE/20008030340",
			"ROLE_MODIFY", "/ROLE/2000803037",
			"ROLE_SCHEDULE", "/ROLE/2000803038",
			"ROLE_CREATE", "/ROLE/2000803041"
		);

		ProviderRoleMapping providerRoleMapping = new ProviderRoleMapping();
		providerRoleMapping.setMapOauthScopes(true);
		providerRoleMapping.setParseOauthScopePathParts(false);
		providerRoleMapping.getRoleMappings().putAll(roleMappings);

		Set<String> scopes = Set.of(
			"/ROLE/2000803042",
			"/ROLE/2000803036",
			"/ROLE/2000803039",
			"/ROLE/20008030340",
			"/ROLE/2000803037",
			"/ROLE/2000803038",
			"/ROLE/2000803041"
		);

		DefaultAuthoritiesMapper defaultAuthoritiesMapper = new DefaultAuthoritiesMapper("uaa", providerRoleMapping);
		Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesMapper.mapScopesToAuthorities("uaa",
				scopes, null);

		assertThat(authorities).hasSize(7);
		assertThat(authorities)
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY",
						"ROLE_SCHEDULE", "ROLE_VIEW");
	}
}
