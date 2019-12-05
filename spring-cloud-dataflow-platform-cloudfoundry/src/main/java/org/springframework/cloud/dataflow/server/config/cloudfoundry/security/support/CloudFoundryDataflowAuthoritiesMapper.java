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
package org.springframework.cloud.dataflow.server.config.cloudfoundry.security.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.common.security.support.AuthoritiesMapper;
import org.springframework.cloud.common.security.support.CoreSecurityRoles;
import org.springframework.cloud.common.security.support.SecurityConfigUtils;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

/**
 * This Spring Cloud Data Flow {@link AuthoritiesMapper} will assign all
 * {@link CoreSecurityRoles} to the authenticated OAuth2 user IF the user is a "Space
 * Developer" in Cloud Foundry.
 *
 * @author Gunnar Hillert
 *
 */
public class CloudFoundryDataflowAuthoritiesMapper implements AuthoritiesMapper {

	private static final Logger logger = LoggerFactory
			.getLogger(CloudFoundryDataflowAuthoritiesMapper.class);

	private final CloudFoundrySecurityService cloudFoundrySecurityService;

	public CloudFoundryDataflowAuthoritiesMapper(CloudFoundrySecurityService cloudFoundrySecurityService) {
		this.cloudFoundrySecurityService = cloudFoundrySecurityService;
	}

	/**
	 * The returned {@link List} of {@link GrantedAuthority}s contains all roles from
	 * {@link CoreSecurityRoles}. The roles are prefixed with the value specified in
	 * {@link GrantedAuthorityDefaults}.
	 *
	 * @param providerId Not used
	 * @param scopes Not used
	 * @param token not used
	 */
	@Override
	public Set<GrantedAuthority> mapScopesToAuthorities(String providerId, Set<String> scopes, String token) {
		if (cloudFoundrySecurityService.isSpaceDeveloper()) {
			final List<String> rolesAsStrings = new ArrayList<>();
			final Set<GrantedAuthority> grantedAuthorities = Stream.of(CoreSecurityRoles.values())
					.map(roleEnum -> {
						final String roleName = SecurityConfigUtils.ROLE_PREFIX + roleEnum.getKey();
						rolesAsStrings.add(roleName);
						return new SimpleGrantedAuthority(roleName);
					})
					.collect(Collectors.toSet());
			logger.info("Adding ALL roles {} to Cloud Foundry Space Developer user.",
					StringUtils.collectionToCommaDelimitedString(rolesAsStrings));
			return grantedAuthorities;
		}
		else {
			return Collections.emptySet();
		}
	}
}
