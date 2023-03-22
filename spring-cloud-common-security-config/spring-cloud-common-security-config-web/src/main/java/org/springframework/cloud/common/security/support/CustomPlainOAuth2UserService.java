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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 *
 * @author Gunnar Hillert
 * @author Janne Valkealahti
 */
public class CustomPlainOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

	private final static Logger log = LoggerFactory.getLogger(CustomPlainOAuth2UserService.class);
	final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
	final AuthoritiesMapper authorityMapper;

	public CustomPlainOAuth2UserService(AuthoritiesMapper authorityMapper) {
		this.authorityMapper = authorityMapper;
	}

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		log.debug("Load user");
		final OAuth2User oauth2User = delegate.loadUser(userRequest);
		final OAuth2AccessToken accessToken = userRequest.getAccessToken();
		log.debug("AccessToken: {}", accessToken.getTokenValue());

		final Set<GrantedAuthority> mappedAuthorities = this.authorityMapper.mapScopesToAuthorities(
				userRequest.getClientRegistration().getRegistrationId(), accessToken.getScopes(),
				accessToken.getTokenValue());
		final String userNameAttributeName = userRequest.getClientRegistration()
				.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
		final OAuth2User oauth2UserToReturn = new DefaultOAuth2User(mappedAuthorities, oauth2User.getAttributes(),
				userNameAttributeName);
		return oauth2UserToReturn;
	}
}
