/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
		"info.app.name=spring-cloud-dataflow-server-local", "info.app.version=1.2.3.RELEASE",
		"spring_cloud_dataflow_version-info_core_url={repository}/org/springframework/cloud/spring-cloud-dataflow-parent/{version}/spring-cloud-dataflow-parent-{version}.pom.klsj",
		"spring_cloud_dataflow_version-info_dashboard_url=https://repo.spring.io/libs-milestone/org/springframework/cloud/spring-cloud-dataflow-ui/1.3.0.M4/spring-cloud-dataflow-ui-1.3.0.M4.jsdfasdf",
		"spring_cloud_dataflow_version-info_shell_url=https://repo.spring.io/libs-milestone/org/springframework/cloud/spring-cloud-dataflow-shell/1.3.0.BUILD-SNAPSHOT/spring-cloud-dataflow-shell-1.3.0.BUILD-SNAPSHOT.jsdfasdf"
})
public class AboutControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Before
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@Test
	public void testListApplications() throws Exception {
		ResultActions result = mockMvc.perform(get("/about").accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());
				result.andExpect(jsonPath("$.featureInfo.analyticsEnabled", is(false)))
				.andExpect(jsonPath("$.versionInfo.implementationDependency.name", is("spring-cloud-dataflow-server-local")))
				.andExpect(jsonPath("$.versionInfo.implementationDependency.version", is("1.2.3.RELEASE")))
				.andExpect(jsonPath("$.versionInfo.implementationDependency.checksumSha1", is("2dd0de58ebd59ac0d74106971dbc2fd03bec1bcd")))
				.andExpect(jsonPath("$.versionInfo.implementationDependency.checksumSha256").doesNotExist())
				.andExpect(jsonPath("$.versionInfo.coreDependency.name", is("Spring Cloud Data Flow Core")))
				.andExpect(jsonPath("$.versionInfo.coreDependency.checksumSha1").doesNotExist())
				.andExpect(jsonPath("$.versionInfo.coreDependency.checksumSha256").doesNotExist())
				.andExpect(jsonPath("$.versionInfo.dashboardDependency.name", is("Spring Cloud Dataflow UI")))
				.andExpect(jsonPath("$.versionInfo.dashboardDependency.checksumSha1").doesNotExist())
				.andExpect(jsonPath("$.versionInfo.shellDependency.name", is("Spring Cloud Data Flow Shell")))
				.andExpect(jsonPath("$.versionInfo.shellDependency.checksumSha1").doesNotExist())
				.andExpect(jsonPath("$.versionInfo.shellDependency.checksumSha256").doesNotExist())
				.andExpect(jsonPath("$.securityInfo.authenticationEnabled", is(false)))
				.andExpect(jsonPath("$.securityInfo.authorizationEnabled", is(false)))
				.andExpect(jsonPath("$.securityInfo.formLogin", is(false)))
				.andExpect(jsonPath("$.securityInfo.authenticated", is(false)))
				.andExpect(jsonPath("$.securityInfo.username", isEmptyOrNullString()));
	}


}
