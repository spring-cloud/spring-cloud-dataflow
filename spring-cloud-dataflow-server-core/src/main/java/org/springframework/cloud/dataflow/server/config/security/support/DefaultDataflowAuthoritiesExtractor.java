/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.security.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default Spring Cloud Data Flow {@link AuthoritiesExtractor}. Will assign ALL
 * {@link CoreSecurityRoles} to the authenticated OAuth2 user.
 *
 * @author Gunnar Hillert
 *
 */
public class DefaultDataflowAuthoritiesExtractor implements AuthoritiesExtractor {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DefaultDataflowAuthoritiesExtractor.class);

	/**
	 * The returned {@link List} of {@link GrantedAuthority}s contains all roles from
	 * {@link CoreSecurityRoles}. The roles are prefixed with the value specified in
	 * {@link GrantedAuthorityDefaults}.
	 *
	 *
	 * @param Must not be null. Is only used for logging
	 */
	@Override
	public List<GrantedAuthority> extractAuthorities(Map<String, Object> map) {
		Assert.notNull(map, "The map argument must not be null.");

		final List<String> rolesAsStrings = new ArrayList<>();
		final List<GrantedAuthority> grantedAuthorities =
			Stream.of(CoreSecurityRoles.values())
				.map(roleEnum -> {
					final String roleName = SecurityConfigUtils.ROLE_PREFIX + roleEnum.getKey();
					rolesAsStrings.add(roleName);
					return new SimpleGrantedAuthority(roleName);
				})
				.collect(Collectors.toList());
		logger.info("Adding ALL roles {} to user {}", StringUtils.collectionToCommaDelimitedString(rolesAsStrings), map);
		return grantedAuthorities;
	}
}
