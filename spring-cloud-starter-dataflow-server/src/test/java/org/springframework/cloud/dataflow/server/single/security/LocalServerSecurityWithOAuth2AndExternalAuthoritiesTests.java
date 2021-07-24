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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import org.springframework.cloud.common.security.support.AuthoritiesMapper;
import org.springframework.cloud.common.security.support.ExternalOauth2ResourceAuthoritiesMapper;
import org.springframework.cloud.dataflow.server.single.LocalDataflowResource;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
public class LocalServerSecurityWithOAuth2AndExternalAuthoritiesTests {

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
	public void testAuthoritiesMapperBean() throws Exception {
		final AuthoritiesMapper authoritiesMapper = localDataflowResource.getWebApplicationContext().getBean(AuthoritiesMapper.class);
		Assertions.assertTrue(authoritiesMapper instanceof ExternalOauth2ResourceAuthoritiesMapper);
	}

	@Test
	public void testDataflowCallingExternalAuthoritiesServer() throws Exception {
		final String[] roles = {"VIEW", "CREATE", "MANAGE"};

		final ObjectMapper objectMapper = new ObjectMapper();
		externalAuthoritiesServer.enqueue(new MockResponse()
				.setBody(objectMapper.writeValueAsString(roles))
				.addHeader("Content-Type", "application/json"));

		final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
		resourceDetails.setClientId("myclient");
		resourceDetails.setClientSecret("mysecret");
		resourceDetails.setGrantType("client_credentials");
		resourceDetails
				.setAccessTokenUri("http://localhost:" + oAuth2ServerResource.getOauth2ServerPort() + "/oauth/token");

		Assertions.assertEquals(0, externalAuthoritiesServer.getRequestCount());

		final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
		final OAuth2AccessToken accessToken = oAuth2RestTemplate.getAccessToken();

		final String accessTokenAsString = accessToken.getValue();

		localDataflowResource.getMockMvc()
				.perform(get("/security/info").header("Authorization", "bearer " + accessTokenAsString)).andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated", is(Boolean.TRUE)))
				.andExpect(jsonPath("$.authenticationEnabled", is(Boolean.TRUE)))
				.andExpect(jsonPath("$.roles", hasSize(3)));

		MatcherAssert.assertThat(externalAuthoritiesServer.getRequestCount(), is(1));
		final RecordedRequest recordedRequest = externalAuthoritiesServer.takeRequest();
		MatcherAssert.assertThat(recordedRequest.getHeader("Authorization"), is("Bearer " + accessTokenAsString));
	}
}
