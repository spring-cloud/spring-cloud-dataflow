/*
 * Copyright 2018-2020 the original author or authors.
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
package org.springframework.cloud.common.security.core.support;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 *
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
class OAuth2AccessTokenProvidingClientHttpRequestInterceptorTests {

	@Test
	void testOAuth2AccessTokenProvidingClientHttpRequestInterceptorWithEmptyConstructior() {
		assertThatThrownBy(() -> new OAuth2AccessTokenProvidingClientHttpRequestInterceptor(""))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("staticOauthAccessToken must not be null or empty.");
	}

	@Test
	void testOAuth2AccessTokenProvidingClientHttpRequestInterceptorWithStaticTokenConstructor() {
		final OAuth2AccessTokenProvidingClientHttpRequestInterceptor interceptor =
				new OAuth2AccessTokenProvidingClientHttpRequestInterceptor("foobar");

		final String accessToken = (String) ReflectionTestUtils.getField(interceptor, "staticOauthAccessToken");
		assertThat(accessToken).isEqualTo("foobar");
	}

	@Test
	void testInterceptWithStaticToken() throws IOException {
		final OAuth2AccessTokenProvidingClientHttpRequestInterceptor interceptor =
				new OAuth2AccessTokenProvidingClientHttpRequestInterceptor("foobar");
		final HttpHeaders headers = setupTest(interceptor);

		assertThat(headers).hasSize(1);
		assertThat(headers).contains(entry("Authorization", Collections.singletonList("Bearer foobar")));
	}

	@Test
	void testInterceptWithAuthentication() throws IOException {
		final OAuth2TokenUtilsService oauth2TokenUtilsService = mock(OAuth2TokenUtilsService.class);
		when(oauth2TokenUtilsService.getAccessTokenOfAuthenticatedUser()).thenReturn("foo-bar-123-token");

		final OAuth2AccessTokenProvidingClientHttpRequestInterceptor interceptor =
			new OAuth2AccessTokenProvidingClientHttpRequestInterceptor(oauth2TokenUtilsService);
		final HttpHeaders headers = setupTest(interceptor);

		assertThat(headers).hasSize(1);
		assertThat(headers).contains(entry("Authorization", Collections.singletonList("Bearer foo-bar-123-token")));
	}

	@Test
	void testInterceptWithAuthenticationAndStaticToken() throws IOException {
		final OAuth2TokenUtilsService oauth2TokenUtilsService = mock(OAuth2TokenUtilsService.class);
		when(oauth2TokenUtilsService.getAccessTokenOfAuthenticatedUser()).thenReturn("foo-bar-123-token");

		final OAuth2AccessTokenProvidingClientHttpRequestInterceptor interceptor =
				new OAuth2AccessTokenProvidingClientHttpRequestInterceptor("foobar");
		final HttpHeaders headers = setupTest(interceptor);

		assertThat(headers).hasSize(1);
		assertThat(headers).contains(entry("Authorization", Collections.singletonList("Bearer foobar")));
	}

	private HttpHeaders setupTest( OAuth2AccessTokenProvidingClientHttpRequestInterceptor interceptor) throws IOException {
		final HttpRequest request = Mockito.mock(HttpRequest.class);
		final ClientHttpRequestExecution clientHttpRequestExecution = Mockito.mock(ClientHttpRequestExecution.class);
		final HttpHeaders headers = new HttpHeaders();

		when(request.getHeaders()).thenReturn(headers);
		interceptor.intercept(request, null, clientHttpRequestExecution);
		verify(clientHttpRequestExecution, Mockito.times(1)).execute(request, null);
		return headers;
	}
}
