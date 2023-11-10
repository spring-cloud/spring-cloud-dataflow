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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Extracts the {@link GrantedAuthority}s from scope attributes typically found
 * in a {@link Jwt}.
 *
 * @author Gunnar Hillert
 * @author Janne Valkealahti
 */
public final class MappingJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private final static Logger log = LoggerFactory.getLogger(MappingJwtGrantedAuthoritiesConverter.class);
	private static final String DEFAULT_AUTHORITY_PREFIX = "SCOPE_";

	private static final Collection<String> WELL_KNOWN_SCOPES_CLAIM_NAMES =
			Arrays.asList("scope", "scp");
	private static final Collection<String> WELL_KNOWN_GROUPS_CLAIM_NAMES =
			Arrays.asList("groups", "roles");

	private String authorityPrefix = DEFAULT_AUTHORITY_PREFIX;

	private String authoritiesClaimName;
	private String groupAuthoritiesClaimName;

	private Map<String, String> roleAuthoritiesMapping = new HashMap<>();
	private Map<String, String> groupAuthoritiesMapping = new HashMap<>();

	/**
	 * Extract {@link GrantedAuthority}s from the given {@link Jwt}.
	 *
	 * @param jwt The {@link Jwt} token
	 * @return The {@link GrantedAuthority authorities} read from the token scopes
	 */
	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		log.debug("JWT: {}", jwt.getTokenValue());
		Set<GrantedAuthority> collect = getAuthorities(jwt).stream()
			.flatMap(authority -> {
				if (roleAuthoritiesMapping.isEmpty() && groupAuthoritiesMapping.isEmpty()) {
					return Stream.of(authority);
				}
				Stream<String> s1 = roleAuthoritiesMapping.entrySet().stream()
					.filter(entry -> entry.getValue().equals(authority))
					.map(entry -> entry.getKey()).distinct();
				Stream<String> s2 = groupAuthoritiesMapping.entrySet().stream()
					.filter(entry -> entry.getValue().equals(authority))
					.map(entry -> entry.getKey()).distinct();
				return Stream.concat(s1, s2);
			})
			.distinct()
			.map(authority -> new SimpleGrantedAuthority(this.authorityPrefix + authority))
			.collect(Collectors.toSet());
		log.debug("JWT granted: {}", collect);
		return collect;
	}

	/**
	 * Sets the prefix to use for {@link GrantedAuthority authorities} mapped by this converter.
	 * Defaults to {@link JwtGrantedAuthoritiesConverter#DEFAULT_AUTHORITY_PREFIX}.
	 *
	 * @param authorityPrefix The authority prefix
	 */
	public void setAuthorityPrefix(String authorityPrefix) {
		Assert.notNull(authorityPrefix, "authorityPrefix cannot be null");
		this.authorityPrefix = authorityPrefix;
	}

	/**
	 * Sets the name of token claim to use for mapping {@link GrantedAuthority
	 * authorities} by this converter. Defaults to
	 * {@link JwtGrantedAuthoritiesConverter#WELL_KNOWN_SCOPES_CLAIM_NAMES}.
	 *
	 * @param authoritiesClaimName The token claim name to map authorities
	 */
	public void setAuthoritiesClaimName(String authoritiesClaimName) {
		Assert.hasText(authoritiesClaimName, "authoritiesClaimName cannot be empty");
		this.authoritiesClaimName = authoritiesClaimName;
	}

	/**
	 * Set the mapping from resolved authorities from jwt into granted authorities.
	 *
	 * @param authoritiesMapping the authoritiesMapping to set
	 */
	public void setAuthoritiesMapping(Map<String, String> authoritiesMapping) {
		Assert.notNull(authoritiesMapping, "authoritiesMapping cannot be null");
		this.roleAuthoritiesMapping = authoritiesMapping;
	}

	/**
	 * Sets the name of token claim to use for group mapping {@link GrantedAuthority
	 * authorities} by this converter. Defaults to
	 * {@link org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter#WELL_KNOWN_AUTHORITIES_CLAIM_NAMES}.
	 *
	 * @param groupAuthoritiesClaimName the token claim name to map group
	 *                                  authorities
	 */
	public void setGroupAuthoritiesClaimName(String groupAuthoritiesClaimName) {
		this.groupAuthoritiesClaimName = groupAuthoritiesClaimName;
	}

	/**
	 * Set the group mapping from resolved authorities from jwt into granted
	 * authorities.
	 *
	 * @param groupAuthoritiesMapping
	 */
	public void setGroupAuthoritiesMapping(Map<String, String> groupAuthoritiesMapping) {
		this.groupAuthoritiesMapping = groupAuthoritiesMapping;
	}

	private String getAuthoritiesClaimName(Jwt jwt) {
		if (this.authoritiesClaimName != null) {
			return this.authoritiesClaimName;
		}
		for (String claimName : WELL_KNOWN_SCOPES_CLAIM_NAMES) {
			if (jwt.hasClaim(claimName)) {
				return claimName;
			}
		}
		return null;
	}

	private String getGroupAuthoritiesClaimName(Jwt jwt) {
		if (this.groupAuthoritiesClaimName != null) {
			return this.groupAuthoritiesClaimName;
		}
		for (String claimName : WELL_KNOWN_GROUPS_CLAIM_NAMES) {
			if (jwt.hasClaim(claimName)) {
				return claimName;
			}
		}
		return null;
	}

	private Collection<String> getAuthorities(Jwt jwt) {
		String scopeClaimName = getAuthoritiesClaimName(jwt);
		String groupClaimName = getGroupAuthoritiesClaimName(jwt);

		List<String> claimAsStringList1 = null;
		List<String> claimAsStringList2 = null;

		// spring-sec does wrong conversion with arrays
		if (scopeClaimName != null && !ObjectUtils.isArray(jwt.getClaim(scopeClaimName))) {
			claimAsStringList1 = jwt.getClaimAsStringList(scopeClaimName);
		}
		if (groupClaimName != null && !ObjectUtils.isArray(jwt.getClaim(groupClaimName))) {
			claimAsStringList2 = jwt.getClaimAsStringList(groupClaimName);
		}

		List<String> claimAsStringList = new ArrayList<>();
		if (claimAsStringList1 != null) {
			List<String> collect = claimAsStringList1.stream()
				.flatMap(c -> Arrays.stream(c.split(" ")))
				.filter(c -> StringUtils.hasText(c))
				.collect(Collectors.toList());
			claimAsStringList.addAll(collect);
		}
		if (claimAsStringList2 != null) {
			claimAsStringList.addAll(claimAsStringList2);
		}
		return claimAsStringList;
	}
}
