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
 */package org.springframework.cloud.common.security.support;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * @author Mike Heath
 * @author Gunnar Hillert
 */
public class ExternalOauth2ResourceAuthoritiesMapperTests {

	public static MockWebServer mockBackEnd;

	@BeforeClass
	public static void setUp() throws IOException {
		mockBackEnd = new MockWebServer();
		mockBackEnd.start();
	}

	@AfterClass
	public static void tearDown() throws IOException {
		mockBackEnd.shutdown();
	}

	@Test
	public void testExtractAuthorities() throws Exception {
		assertAuthorities2(mockBackEnd.url("/authorities").uri(), "VIEW");
		assertAuthorities2(mockBackEnd.url("/authorities").uri(), "VIEW", "CREATE", "MANAGE");
		assertAuthorities2(mockBackEnd.url("/").uri(), "MANAGE");
		assertAuthorities2(mockBackEnd.url("/").uri(), "DEPLOY", "DESTROY", "MODIFY", "SCHEDULE");
		assertThat(mockBackEnd.getRequestCount(), is(4));
	}

	private void assertAuthorities2(URI uri, String... roles) throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		mockBackEnd.enqueue(new MockResponse()
				.setBody(objectMapper.writeValueAsString(roles))
				.addHeader("Content-Type", "application/json"));

		final ExternalOauth2ResourceAuthoritiesMapper authoritiesExtractor =
				new ExternalOauth2ResourceAuthoritiesMapper(uri);
		final Set<GrantedAuthority> grantedAuthorities = authoritiesExtractor.mapScopesToAuthorities(null, new HashSet<>(), "1234567");
		for (String role : roles) {
			assertThat(grantedAuthorities, hasItem(new SimpleGrantedAuthority(SecurityConfigUtils.ROLE_PREFIX + role)));
		}
		assertThat(mockBackEnd.takeRequest().getHeader("Authorization"), is("Bearer 1234567"));
	}
}
