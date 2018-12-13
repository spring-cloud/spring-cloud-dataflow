/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.local.security;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import org.springframework.cloud.dataflow.server.local.LocalDataflowResource;

import static org.springframework.cloud.dataflow.server.local.security.SecurityTestUtils.basicAuthorizationHeader;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
@Ignore("Ignored until proper UAA integration exists") //FIXME see https://github.com/spring-cloud/spring-cloud-dataflow/issues/2580
public class LocalServerSecurityWithLdapSimpleBindGroupMappingTests {

	private final static LocalDataflowResource localDataflowResource = new LocalDataflowResource(
			"classpath:org/springframework/cloud/dataflow/server/local/security/ldapSimpleBindGroupMapping.yml");

	@ClassRule
	public static TestRule springDataflowAndLdapServer = RuleChain.outerRule(
		new LdapServerResource("testUsersWithCustomRoles.ldif")).around(localDataflowResource);

	@Test
	public void testWrongUsernameFails() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/apps").header("Authorization", basicAuthorizationHeader("joe", "wrongspassword")))
				.andDo(print()).andExpect(status().isUnauthorized());
	}

	@Test
	public void testDefaultSpringBootConfigurationFails() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/apps").header("Authorization", basicAuthorizationHeader("admin", "whosThere")))
				.andDo(print()).andExpect(status().isUnauthorized());
	}

	@Test
	public void testWrongPasswordFails() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/apps").header("Authorization", basicAuthorizationHeader("bob", "bobpassword999")))
				.andDo(print()).andExpect(status().isUnauthorized());
	}

	@Test
	public void testUnauthenticatedAccessToAppsEndpointFails() throws Exception {
		localDataflowResource.getMockMvc().perform(get("/apps")).andExpect(status().isUnauthorized());
	}

	@Test
	public void testUnauthenticatedAccessToManagementEndpointFails() throws Exception {
		localDataflowResource.getMockMvc().perform(get("/management/metrics")).andExpect(status().isUnauthorized());
	}

	@Test
	public void testAuthenticatedAccessToAppsEndpointSucceeds() throws Exception {
		localDataflowResource.getMockMvc()
				.perform(get("/apps").header("Authorization", basicAuthorizationHeader("joe", "joespassword")))
				.andDo(print()).andExpect(status().isOk());
	}

	// TODO: BOOT2, handle this test when we have something from micrometrics
	//	@Test
	//	public void testAuthenticatedAccessToManagementEndpointSucceeds() throws Exception {
	//		localDataflowResource.getMockMvc().perform(
	//				get("/management/metrics").header("Authorization", basicAuthorizationHeader("joe", "joespassword")))
	//				.andDo(print()).andExpect(status().isOk());
	//	}

}
