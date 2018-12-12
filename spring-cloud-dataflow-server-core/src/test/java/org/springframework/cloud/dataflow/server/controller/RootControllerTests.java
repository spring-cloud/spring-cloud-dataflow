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

package org.springframework.cloud.dataflow.server.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.registry.DataFlowAppRegistryPopulator;
import org.springframework.cloud.dataflow.server.support.MockUtils;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestDependencies.class},
		properties = { "spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow",
		"spring.cloud.dataflow.features.analytics-enabled=false"})
@EnableConfigurationProperties({ CommonApplicationProperties.class })
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public class RootControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppRegistryService appRegistryService;

	@MockBean
	private SkipperClient skipperClient;

	@Autowired
	private DataFlowAppRegistryPopulator uriRegistryPopulator;

	@Before
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration : this.appRegistryService.findAll()) {
			this.appRegistryService.delete(appRegistration.getName(), appRegistration.getType(), appRegistration.getVersion());
		}
		this.uriRegistryPopulator.afterPropertiesSet();
		this.skipperClient = MockUtils.configureMock(this.skipperClient);
	}

	@Test
	public void testRegisterVersionedApp() throws Exception {
		String mvcResult = mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		Assert.assertEquals("{\"links\":[{\"rel\":\"dashboard\",\"href\":\"http://localhost/dashboard\"," +
				"\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null}," +
				"{\"rel\":\"audit-records\",\"href\":\"http://localhost/audit-records\",\"hreflang\":null,\"media\":null," +
				"\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/definitions\"," +
				"\"href\":\"http://localhost/streams/definitions\",\"hreflang\":null,\"media\":null,\"title\":null," +
				"\"type\":null,\"deprecation\":null},{\"rel\":\"streams/definitions/definition\"," +
				"\"href\":\"http://localhost/streams/definitions/{name}\",\"hreflang\":null,\"media\":null,\"title\":null," +
				"\"type\":null,\"deprecation\":null},{\"rel\":\"streams/validation\"," +
				"\"href\":\"http://localhost/streams/validation/{name}\",\"hreflang\":null,\"media\":null," +
				"\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"runtime/apps\"," +
				"\"href\":\"http://localhost/runtime/apps\",\"hreflang\":null,\"media\":null,\"title\":null," +
				"\"type\":null,\"deprecation\":null},{\"rel\":\"runtime/apps/app\",\"href\":\"http://localhost/runtime/apps/{appId}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"runtime/apps/instances\",\"href\":\"http://localhost/runtime/apps/{appId}/instances\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"metrics/streams\",\"href\":\"http://localhost/metrics/streams\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/deployments\",\"href\":\"http://localhost/streams/deployments\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/deployments/{name}\",\"href\":\"http://localhost/streams/deployments/{name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/deployments/history/{name}\",\"href\":\"http://localhost/streams/deployments/history/{name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/deployments/manifest/{name}/{version}\",\"href\":\"http://localhost/streams/deployments/manifest/{name}/{version}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/deployments/platform/list\",\"href\":\"http://localhost/streams/deployments/platform/list\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/deployments/rollback/{name}/{version}\",\"href\":\"http://localhost/streams/deployments/rollback/{name}/{version}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/deployments/update/{name}\",\"href\":\"http://localhost/streams/deployments/update/{name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"streams/deployments/deployment\",\"href\":\"http://localhost/streams/deployments/{name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tasks/definitions\",\"href\":\"http://localhost/tasks/definitions\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tasks/definitions/definition\",\"href\":\"http://localhost/tasks/definitions/{name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tasks/executions\",\"href\":\"http://localhost/tasks/executions\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tasks/executions/name\",\"href\":\"http://localhost/tasks/executions{?name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tasks/executions/current\",\"href\":\"http://localhost/tasks/executions/current\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tasks/executions/execution\",\"href\":\"http://localhost/tasks/executions/{id}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tasks/validation\",\"href\":\"http://localhost/tasks/validation/{name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/executions\",\"href\":\"http://localhost/jobs/executions\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/executions/name\",\"href\":\"http://localhost/jobs/executions{?name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/executions/execution\",\"href\":\"http://localhost/jobs/executions/{id}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/executions/execution/steps\",\"href\":\"http://localhost/jobs/executions/{jobExecutionId}/steps\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/executions/execution/steps/step\",\"href\":\"http://localhost/jobs/executions/{jobExecutionId}/steps/{stepId}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/executions/execution/steps/step/progress\",\"href\":\"http://localhost/jobs/executions/{jobExecutionId}/steps/{stepId}/progress\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/instances/name\",\"href\":\"http://localhost/jobs/instances{?name}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/instances/instance\",\"href\":\"http://localhost/jobs/instances/{id}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tools/parseTaskTextToGraph\",\"href\":\"http://localhost/tools\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"tools/convertTaskGraphToText\",\"href\":\"http://localhost/tools\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"jobs/thinexecutions\",\"href\":\"http://localhost/jobs/thinexecutions\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"apps\",\"href\":\"http://localhost/apps\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"about\",\"href\":\"http://localhost/about\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"completions/stream\",\"href\":\"http://localhost/completions/stream{?start,detailLevel}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null},{\"rel\":\"completions/task\",\"href\":\"http://localhost/completions/task{?start,detailLevel}\",\"hreflang\":null,\"media\":null,\"title\":null,\"type\":null,\"deprecation\":null}],\"api.revision\":14}", mvcResult);
	}
}
