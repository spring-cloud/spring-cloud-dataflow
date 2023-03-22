/*
 * Copyright 2019-2021 the original author or authors.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;

/**
 *
 * @author Gunnar Hillert
 * @author Janne Valkealahti
 */
public class CustomOAuth2OidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	private final static Logger log = LoggerFactory.getLogger(CustomOAuth2OidcUserService.class);
	final OidcUserService delegate = new OidcUserService();
	final AuthoritiesMapper authorityMapper;

	public CustomOAuth2OidcUserService(AuthoritiesMapper authorityMapper) {
		this.authorityMapper = authorityMapper;
	}

	@Override
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		log.debug("Load user");
		final OidcUser oidcUser = delegate.loadUser(userRequest);
		final OAuth2AccessToken accessToken = userRequest.getAccessToken();
		final Set<GrantedAuthority> mappedAuthorities1 = this.authorityMapper.mapScopesToAuthorities(
				userRequest.getClientRegistration().getRegistrationId(), accessToken.getScopes(),
				accessToken.getTokenValue());

		List<String> roleClaims = oidcUser.getClaimAsStringList("groups");
		if (roleClaims == null) {
			roleClaims = oidcUser.getClaimAsStringList("roles");
		}
		if (roleClaims == null) {
			roleClaims = new ArrayList<>();
		}
		log.debug("roleClaims: {}", roleClaims);
		Set<GrantedAuthority> mappedAuthorities2 = this.authorityMapper
				.mapClaimsToAuthorities(userRequest.getClientRegistration().getRegistrationId(), roleClaims);

		final String userNameAttributeName = userRequest.getClientRegistration()
				.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

		log.debug("AccessToken: {}", accessToken.getTokenValue());

		HashSet<GrantedAuthority> mappedAuthorities = new HashSet<>(mappedAuthorities1);
		mappedAuthorities.addAll(mappedAuthorities2);

		final OidcUser oidcUserToReturn;
		// OidcUser oidcUserToReturn;

		if (StringUtils.hasText(userNameAttributeName)) {
			oidcUserToReturn = new DefaultOidcUser(mappedAuthorities, userRequest.getIdToken(), oidcUser.getUserInfo(),
					userNameAttributeName);
		} else {
			oidcUserToReturn = new DefaultOidcUser(mappedAuthorities, userRequest.getIdToken(), oidcUser.getUserInfo());
		}
		return oidcUserToReturn;
	}
}
