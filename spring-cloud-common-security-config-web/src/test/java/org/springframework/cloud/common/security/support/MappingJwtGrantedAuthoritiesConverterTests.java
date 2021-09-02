/*
 * Copyright 2020-2021 the original author or authors.
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappingJwtGrantedAuthoritiesConverter}
 *
 */
public class MappingJwtGrantedAuthoritiesConverterTests {

	public static Jwt.Builder jwt() {
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.audience(Arrays.asList("https://audience.example.org"))
				.expiresAt(Instant.MAX)
				.issuedAt(Instant.MIN)
				.issuer("https://issuer.example.org")
				.jti("jti")
				.notBefore(Instant.MIN)
				.subject("mock-test-subject");
	}

	public static Jwt user() {
		return jwt()
				.claim("sub", "mock-test-subject")
				.build();
	}

	@Test
	public void convertWhenTokenHasScopeAttributeThenTranslatedToAuthorities() {
		Jwt jwt = jwt().claim("scope", "message:read message:write").build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).containsExactlyInAnyOrder(
				new SimpleGrantedAuthority("SCOPE_message:read"),
				new SimpleGrantedAuthority("SCOPE_message:write"));
	}

	@Test
	public void convertWithCustomAuthorityPrefixWhenTokenHasScopeAttributeThenTranslatedToAuthoritiesViaMapping() {
		Jwt jwt = jwt().claim("scope", "message:read message:write").build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
		Map<String, String> authoritiesMapping = new HashMap<>();
		authoritiesMapping.put("READ", "message:read");
		authoritiesMapping.put("WRITE", "message:write");
		jwtGrantedAuthoritiesConverter.setAuthoritiesMapping(authoritiesMapping);
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).containsExactly(
				new SimpleGrantedAuthority("ROLE_READ"),
				new SimpleGrantedAuthority("ROLE_WRITE"));
	}

	@Test
	public void convertWithCustomAuthorityWhenTokenHasScopeAttributeThenTranslatedToAuthoritiesViaMapping() {
		Jwt jwt = jwt().claim("scope", "message:read message:write").build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
		Map<String, String> authoritiesMapping = new HashMap<>();
		authoritiesMapping.put("ROLE_READ", "message:read");
		authoritiesMapping.put("ROLE_WRITE", "message:write");
		jwtGrantedAuthoritiesConverter.setAuthoritiesMapping(authoritiesMapping);
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).containsExactly(
				new SimpleGrantedAuthority("ROLE_READ"),
				new SimpleGrantedAuthority("ROLE_WRITE"));
	}

	@Test
	public void convertWithCustomAuthorityPrefixWhenTokenHasScopeAttributeThenTranslatedToAuthorities() {
		Jwt jwt = jwt().claim("scope", "message:read message:write").build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).containsExactlyInAnyOrder(
				new SimpleGrantedAuthority("ROLE_message:read"),
				new SimpleGrantedAuthority("ROLE_message:write"));
	}

	@Test
	public void convertWhenTokenHasEmptyScopeAttributeThenTranslatedToNoAuthorities() {
		Jwt jwt = jwt().claim("scope", "").build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).isEmpty();
	}

	@Test
	public void convertWhenTokenHasScpAttributeThenTranslatedToAuthorities() {
		Jwt jwt = jwt().claim("scp", Arrays.asList("message:read", "message:write")).build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).containsExactlyInAnyOrder(
				new SimpleGrantedAuthority("SCOPE_message:read"),
				new SimpleGrantedAuthority("SCOPE_message:write"));
	}

	@Test
	public void convertWithCustomAuthorityPrefixWhenTokenHasScpAttributeThenTranslatedToAuthorities() {
		Jwt jwt = jwt().claim("scp", Arrays.asList("message:read", "message:write")).build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).containsExactlyInAnyOrder(
				new SimpleGrantedAuthority("ROLE_message:read"),
				new SimpleGrantedAuthority("ROLE_message:write"));
	}

	@Test
	public void convertWhenTokenHasEmptyScpAttributeThenTranslatedToNoAuthorities() {
		Jwt jwt = jwt().claim("scp", Collections.emptyList()).build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).isEmpty();
	}

	@Test
	public void convertWhenTokenHasBothScopeAndScpThenScopeAttributeIsTranslatedToAuthorities() {
		Jwt jwt = jwt()
			.claim("scp", Arrays.asList("message:read", "message:write"))
			.claim("scope", "missive:read missive:write")
			.build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).containsExactly(
				new SimpleGrantedAuthority("SCOPE_missive:read"),
				new SimpleGrantedAuthority("SCOPE_missive:write"));
	}

	@Test
	public void convertWhenTokenHasEmptyScopeAndNonEmptyScpThenScopeAttributeIsTranslatedToNoAuthorities() {
		Jwt jwt = jwt()
			.claim("scp", Arrays.asList("message:read", "message:write"))
			.claim("scope", "")
			.build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).isEmpty();
	}

	@Test
	public void convertWhenTokenHasEmptyScopeAndEmptyScpAttributeThenTranslatesToNoAuthorities() {
		Jwt jwt = jwt()
			.claim("scp", Collections.emptyList())
			.claim("scope", Collections.emptyList())
			.build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).isEmpty();
	}

	@Test
	public void convertWhenTokenHasNoScopeAndNoScpAttributeThenTranslatesToNoAuthorities() {
		Jwt jwt = jwt().claim("roles", Arrays.asList("message:read", "message:write")).build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).isEmpty();
	}

	@Test
	public void convertWhenTokenHasUnsupportedTypeForScopeThenTranslatesToNoAuthorities() {
		Jwt jwt = jwt().claim("scope", new String[] {"message:read", "message:write"}).build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).isEmpty();
	}

	@Test
	public void convertWhenTokenHasCustomClaimNameThenCustomClaimNameAttributeIsTranslatedToAuthorities() {
		Jwt jwt = jwt()
				.claim("roles", Arrays.asList("message:read", "message:write"))
				.claim("scope", "missive:read missive:write")
				.build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).containsExactlyInAnyOrder(
				new SimpleGrantedAuthority("SCOPE_message:read"),
				new SimpleGrantedAuthority("SCOPE_message:write"));
	}

	@Test
	public void convertWhenTokenHasEmptyCustomClaimNameThenCustomClaimNameAttributeIsTranslatedToNoAuthorities() {
		Jwt jwt = jwt()
				.claim("roles", Collections.emptyList())
				.claim("scope", "missive:read missive:write")
				.build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).isEmpty();
	}

	@Test
	public void convertWhenTokenHasNoCustomClaimNameThenCustomClaimNameAttributeIsTranslatedToNoAuthorities() {
		Jwt jwt = jwt().claim("scope", "missive:read missive:write").build();

		MappingJwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new MappingJwtGrantedAuthoritiesConverter();
		jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);

		assertThat(authorities).isEmpty();
	}
}
