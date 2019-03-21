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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * @author Gunnar Hillert
 */
public class DefaultAuthoritiesExtractorTests {

	@Test
	public void testNullMapParameter() throws Exception {
		final DefaultAuthoritiesExtractor authoritiesExtractor = new DefaultAuthoritiesExtractor();
		try {
			authoritiesExtractor.extractAuthorities(null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("The map argument must not be null.", e.getMessage());
			return;
		}
		Assert.fail("Expected an IllegalStateException to be thrown.");
	}

	@Test
	public void testThat7AuthoritiesAreReturned() throws Exception {
		final DefaultAuthoritiesExtractor authoritiesExtractor = new DefaultAuthoritiesExtractor();

		final List<GrantedAuthority> authorities = authoritiesExtractor.extractAuthorities(new HashMap<>());
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

		final OAuth2RestTemplate template = mock(OAuth2RestTemplate.class);
		final OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);
		when(template.getAccessToken()).thenReturn(accessToken);
		when(accessToken.getScope()).thenReturn(scopes);

		final Collection<? extends GrantedAuthority> authorities = new DefaultAuthoritiesExtractor(true, new HashMap<>(), template).extractAuthorities(new HashMap<>());
		assertThat(authorities, hasSize(3));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW"));
	}

	@Test
	public void testMapConstructorWithIncompleteRoleMappings() throws Exception {
		try {
			new DefaultAuthoritiesExtractor(true, Collections.singletonMap("ROLE_MANAGE", "foo-scope-in-oauth"), mock(OAuth2RestTemplate.class));
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

		final OAuth2RestTemplate template = mock(OAuth2RestTemplate.class);
		final OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);

		final Set<String> scopes = new HashSet<>();
		scopes.add("foo-manage");
		scopes.add("bar-view");
		scopes.add("blubba-create");
		scopes.add("foo-modify");
		scopes.add("foo-deploy");
		scopes.add("foo-destroy");
		scopes.add("foo-schedule");

		when(template.getAccessToken()).thenReturn(accessToken);
		when(accessToken.getScope()).thenReturn(scopes);

		final DefaultAuthoritiesExtractor defaultAuthoritiesExtractor = new DefaultAuthoritiesExtractor(true, roleMappings, template);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.extractAuthorities(new HashMap<>());

		assertThat(authorities, hasSize(7));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY", "ROLE_SCHEDULE", "ROLE_VIEW"));
	}

	@Test
	public void testThat3MappedAuthoritiesAreReturnedForDefaultMapping() throws Exception {

		final OAuth2RestTemplate template = mock(OAuth2RestTemplate.class);
		final OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);

		final Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		when(template.getAccessToken()).thenReturn(accessToken);
		when(accessToken.getScope()).thenReturn(scopes);

		final DefaultAuthoritiesExtractor defaultAuthoritiesExtractor = new DefaultAuthoritiesExtractor(true, null, template);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.extractAuthorities(new HashMap<>());

		assertThat(authorities, hasSize(3));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_MANAGE", "ROLE_CREATE", "ROLE_VIEW"));
	}

	@Test
	public void testThat7MappedAuthoritiesAreReturnedForDefaultMappingWithoutMappingScopes() throws Exception {

		final OAuth2RestTemplate template = mock(OAuth2RestTemplate.class);
		final OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);

		final Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.manage");
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		when(template.getAccessToken()).thenReturn(accessToken);
		when(accessToken.getScope()).thenReturn(scopes);

		final DefaultAuthoritiesExtractor defaultAuthoritiesExtractor = new DefaultAuthoritiesExtractor(false, null, template);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.extractAuthorities(new HashMap<>());

		assertThat(authorities, hasSize(7));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY", "ROLE_SCHEDULE", "ROLE_VIEW"));
	}

	@Test
	public void testThat2MappedAuthoritiesAreReturnedForDefaultMapping() throws Exception {

		final OAuth2RestTemplate template = mock(OAuth2RestTemplate.class);
		final OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);

		final Set<String> scopes = new HashSet<>();
		scopes.add("dataflow.view");
		scopes.add("dataflow.create");

		when(template.getAccessToken()).thenReturn(accessToken);
		when(accessToken.getScope()).thenReturn(scopes);

		final DefaultAuthoritiesExtractor defaultAuthoritiesExtractor = new DefaultAuthoritiesExtractor(true, null, template);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.extractAuthorities(new HashMap<>());

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

		final OAuth2RestTemplate template = mock(OAuth2RestTemplate.class);
		final OAuth2AccessToken accessToken = mock(OAuth2AccessToken.class);

		final Set<String> scopes = new HashSet<>();
		scopes.add("foo-manage");
		scopes.add("blubba-create");

		when(template.getAccessToken()).thenReturn(accessToken);
		when(accessToken.getScope()).thenReturn(scopes);

		final DefaultAuthoritiesExtractor defaultAuthoritiesExtractor = new DefaultAuthoritiesExtractor(true, roleMappings, template);

		final Collection<? extends GrantedAuthority> authorities = defaultAuthoritiesExtractor.extractAuthorities(new HashMap<>());

		assertThat(authorities, hasSize(7));

		assertThat(authorities.stream().map(authority -> authority.getAuthority()).collect(Collectors.toList()),
			containsInAnyOrder("ROLE_CREATE", "ROLE_DEPLOY", "ROLE_DESTROY", "ROLE_MANAGE", "ROLE_MODIFY", "ROLE_SCHEDULE", "ROLE_VIEW"));
	}
}
