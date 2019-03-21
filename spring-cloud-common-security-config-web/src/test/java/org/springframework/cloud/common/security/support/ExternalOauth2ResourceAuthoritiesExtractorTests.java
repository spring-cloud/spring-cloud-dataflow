/*
 * Copyright 2018 the original author or authors.
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
 */package org.springframework.cloud.common.security.support;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mike Heath
 */
public class ExternalOauth2ResourceAuthoritiesExtractorTests {

	@Test
	public void testExtractAuthorities() {
		assertAuthorities(URI.create("http://test/authorities"), "VIEW");
		assertAuthorities(URI.create("https://the.authorities.server/authorities"), "VIEW", "CREATE", "MANAGE");
		assertAuthorities(URI.create("https://server/"), "MANAGE");
		assertAuthorities(URI.create("https://scdf2/"), "DEPLOY", "DESTROY", "MODIFY", "SCHEDULE");
	}

	private void assertAuthorities(URI uri, String... roles) {
		final OAuth2RestTemplate mockRestTemplate = mock(OAuth2RestTemplate.class);
		final String accessToken = UUID.randomUUID().toString();
		final OAuth2AccessToken oAuth2AccessToken = new DefaultOAuth2AccessToken(accessToken);
		final ArgumentCaptor<RequestEntity> requestArgumentCaptor = ArgumentCaptor.forClass(RequestEntity.class);
		when(mockRestTemplate.getAccessToken()).thenReturn(oAuth2AccessToken);
		when(mockRestTemplate.exchange(requestArgumentCaptor.capture(), (Class<String[]>)any())).thenReturn(new ResponseEntity<>(roles, HttpStatus.OK));

		final ExternalOauth2ResourceAuthoritiesExtractor authoritiesExtractor =
				new ExternalOauth2ResourceAuthoritiesExtractor(mockRestTemplate, uri);
		final List<GrantedAuthority> grantedAuthorities = authoritiesExtractor.extractAuthorities(new HashMap<>());
		for (String role : roles) {
			assertThat(grantedAuthorities, hasItem(new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + role)));
		}

		verify(mockRestTemplate).exchange(requestArgumentCaptor.capture(), (Class<String[]>)any());
		final RequestEntity requestEntity = requestArgumentCaptor.getValue();
		assertThat(requestEntity.getUrl(), equalTo(uri));
		assertThat(requestEntity.getMethod(), equalTo(HttpMethod.GET));
		assertThat(requestEntity.getHeaders().get("Authorization"), contains("bearer " + accessToken));

	}
}
