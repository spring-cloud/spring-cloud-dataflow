/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.single.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.MatcherAssert;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import org.springframework.cloud.dataflow.server.single.LocalDataflowResource;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.cloud.dataflow.server.single.security.SecurityTestUtils.basicAuthorizationHeader;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
public class LocalServerSecurityWithOAuth2BasicAuthAndExternalAuthoritiesTests {

	private final static OAuth2ServerResource oAuth2ServerResource = new OAuth2ServerResource();

	private final static MockWebServer externalAuthoritiesServer = new MockWebServer();

	static {
		System.setProperty("externalAuthoritiesUrl", externalAuthoritiesServer.url("/").toString());
	}

	private final static LocalDataflowResource localDataflowResource = new LocalDataflowResource(
			"classpath:org/springframework/cloud/dataflow/server/single/security/oauthConfigUsingExternalAuthorities.yml");

	@ClassRule
	public static TestRule springDataflowAndOAuth2Server = RuleChain.outerRule(oAuth2ServerResource).around(externalAuthoritiesServer)
			.around(localDataflowResource);

	@Test
	public void testDataflowCallingExternalAuthoritiesServer() throws Exception {
		final String[] roles = {"VIEW", "CREATE", "MANAGE"};

		final ObjectMapper objectMapper = new ObjectMapper();
		externalAuthoritiesServer.enqueue(new MockResponse()
				.setBody(objectMapper.writeValueAsString(roles))
				.addHeader("Content-Type", "application/json"));

		localDataflowResource.getMockMvc()
				.perform(get("/security/info").header("Authorization", basicAuthorizationHeader("user", "secret10"))).andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated", is(Boolean.TRUE)))
				.andExpect(jsonPath("$.authenticationEnabled", is(Boolean.TRUE)))
				.andExpect(jsonPath("$.roles", hasSize(3)));

		MatcherAssert.assertThat(externalAuthoritiesServer.getRequestCount(), is(1));
		final RecordedRequest recordedRequest = externalAuthoritiesServer.takeRequest();
		MatcherAssert.assertThat(recordedRequest.getHeader("Authorization"), is(not(emptyString())));
	}
}
