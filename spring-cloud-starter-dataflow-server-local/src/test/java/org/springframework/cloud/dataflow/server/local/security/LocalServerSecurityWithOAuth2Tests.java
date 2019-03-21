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

package org.springframework.cloud.dataflow.server.local.security;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import org.springframework.cloud.dataflow.server.local.LocalDataflowResource;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.dataflow.server.local.security.SecurityTestUtils.basicAuthorizationHeader;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
public class LocalServerSecurityWithOAuth2Tests {

	private final static OAuth2ServerResource oAuth2ServerResource = new OAuth2ServerResource();

	private final static LocalDataflowResource localDataflowResource = new LocalDataflowResource(
			"classpath:org/springframework/cloud/dataflow/server/local/security/oauthConfig" + ".yml");

	@ClassRule
	public static TestRule springDataflowAndLdapServer = RuleChain.outerRule(oAuth2ServerResource)
			.around(localDataflowResource);

	@Test
	public void testAccessRootUrlWithoutCredentials() throws Exception {
		localDataflowResource.getMockMvc().perform(get("/")).andDo(print()).andExpect(status().isUnauthorized());
	}

	@Test
	public void testAccessRootUrlWithBasicAuthCredentials() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/").header("Authorization", basicAuthorizationHeader("user", "secret10"))).andDo(print())
				.andExpect(status().isOk());
	}

	@Test
	public void testAccessRootUrlWithBasicAuthCredentialsWrongPassword() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/").header("Authorization", basicAuthorizationHeader("user", "wrong-password")))
				.andDo(print()).andExpect(status().isUnauthorized());
	}

	@Test
	public void testThatAccessToActuatorEndpointPromptsSecurity() throws Exception {
		localDataflowResource.getMockMvc().perform(get("/management/env")).andDo(print())
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testAccessToActuatorEndpointWithBasicAuthCredentialsWrongPassword() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/management/env").header("Authorization",
						basicAuthorizationHeader("user", "wrong-password")))
				.andDo(print()).andExpect(status().isUnauthorized());
	}

	@Test
	public void testThatAccessToActuatorEndpointWithBasicAuthCredentialsSucceeds() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/management/env").header("Authorization", basicAuthorizationHeader("user", "secret10")))
				.andDo(print()).andExpect(status().isOk());
	}

	@Test
	public void testAccessRootUrlWithOAuth2AccessToken() throws Exception {

		final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
		resourceDetails.setClientId("myclient");
		resourceDetails.setClientSecret("mysecret");
		resourceDetails.setGrantType("client_credentials");
		resourceDetails
				.setAccessTokenUri("http://localhost:" + oAuth2ServerResource.getOauth2ServerPort() + "/oauth/token");

		final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
		final OAuth2AccessToken accessToken = oAuth2RestTemplate.getAccessToken();

		final String accessTokenAsString = accessToken.getValue();

		localDataflowResource.getMockMvc().perform(get("/").header("Authorization", "bearer " + accessTokenAsString))
				.andDo(print()).andExpect(status().isOk());
	}

	@Test
	public void testAccessSecurityInfoUrlWithOAuth2AccessToken() throws Exception {

		final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
		resourceDetails.setClientId("myclient");
		resourceDetails.setClientSecret("mysecret");
		resourceDetails.setGrantType("client_credentials");
		resourceDetails
				.setAccessTokenUri("http://localhost:" + oAuth2ServerResource.getOauth2ServerPort() + "/oauth/token");

		final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(resourceDetails);
		final OAuth2AccessToken accessToken = oAuth2RestTemplate.getAccessToken();

		final String accessTokenAsString = accessToken.getValue();

		localDataflowResource.getMockMvc()
				.perform(get("/security/info").header("Authorization", "bearer " + accessTokenAsString)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("$.authorizationEnabled", is(Boolean.TRUE)))
				.andExpect(jsonPath("$.authenticated", is(Boolean.TRUE)))
				.andExpect(jsonPath("$.authenticationEnabled", is(Boolean.TRUE)))
				.andExpect(jsonPath("$.roles", hasSize(3)));
	}

	@Test
	public void testAccessRootUrlWithWrongOAuth2AccessToken() throws Exception {
		localDataflowResource.getMockMvc().perform(get("/").header("Authorization", "bearer 123456")).andDo(print())
				.andExpect(status().isUnauthorized());
	}

}
