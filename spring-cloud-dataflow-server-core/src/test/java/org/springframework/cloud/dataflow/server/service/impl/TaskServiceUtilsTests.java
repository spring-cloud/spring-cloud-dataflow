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
 */

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.server.controller.WhitelistProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * Verifies the behavior of the methods in the utility.
 *
 * @author Glenn Renfro
 */
public class TaskServiceUtilsTests {
	public static final String BASE_GRAPH = "AAA && BBB";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateComposedTaskDefinition() {
		TaskConfigurationProperties props = new TaskConfigurationProperties();
		props.setComposedTaskRunnerName("FOO");
		TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH, props);
		assertThat(TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH, props)).isEqualTo("FOO --graph=\"AAA && BBB\"");
	}

	@Test
	public void testCreateComposeTaskDefinitionNullNameCheck() {
		this.expectedException.expect(IllegalArgumentException.class);
		TaskConfigurationProperties props = new TaskConfigurationProperties();
		TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH, props);
		TaskServiceUtils.createComposedTaskDefinition(null, props);
	}

	@Test
	public void testCreateComposeTaskDefinitionNullProperties() {
		this.expectedException.expect(IllegalArgumentException.class);
		TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH, null);
	}

	@Test
	public void testCTRPropertyReplacement() {
		TaskNode node = parse("AAA && BBB");
		Map<String, String> taskDeploymentProperties = new HashMap<>();
		taskDeploymentProperties.put("app.test.BBB.timestamp.format", "aformat");
		taskDeploymentProperties.put("deployer.test.BBB.foo", "bar");
		taskDeploymentProperties = TaskServiceUtils.establishComposedTaskProperties(
				taskDeploymentProperties,
				node);
		assertThat(taskDeploymentProperties.size()).isEqualTo(1);
		assertThat(taskDeploymentProperties.get(
				"app.composed-task-runner.composed-task-properties"))
				.isEqualTo("app.test-BBB.app.BBB.timestamp.format=aformat, deployer.test-BBB.deployer.BBB.foo=bar");
	}

	@Test
	public void testDatabasePropUpdate() {
		TaskDefinition taskDefinition = new TaskDefinition("testTask", "testApp");
		DataSourceProperties dataSourceProperties = new DataSourceProperties();
		dataSourceProperties.setUsername("myUser");
		dataSourceProperties.setDriverClassName("myDriver");
		dataSourceProperties.setPassword("myPassword");
		dataSourceProperties.setUrl("myUrl");
		TaskDefinition definition = TaskServiceUtils.updateTaskProperties(
				taskDefinition,
				dataSourceProperties);

		assertThat(definition.getProperties().size()).isEqualTo(5);
		assertThat(definition.getProperties().get("spring.datasource.url")).isEqualTo("myUrl");
		assertThat(definition.getProperties().get("spring.datasource.driverClassName")).isEqualTo("myDriver");
		assertThat(definition.getProperties().get("spring.datasource.username")).isEqualTo("myUser");
		assertThat(definition.getProperties().get("spring.datasource.password")).isEqualTo("myPassword");
	}

	@Test
	public void testExtractAppProperties() {
		Map<String, String> taskDeploymentProperties = new HashMap<>();
		taskDeploymentProperties.put("app.test.foo", "bar");
		taskDeploymentProperties.put("test.none", "boo");
		taskDeploymentProperties.put("app.test.test", "baz");
		taskDeploymentProperties.put("app.none.test", "boo");
		Map<String, String> result = TaskServiceUtils.extractAppProperties("test",
				taskDeploymentProperties);

		assertThat(result.size()).isEqualTo(2);
		assertThat(result.get("foo")).isEqualTo("bar");
		assertThat(result.get("test")).isEqualTo("baz");
	}

	@Test
	public void testMergeAndExpandAppProperties() {
		TaskDefinition taskDefinition = new TaskDefinition("testTask", "testApp");
		Map<String, String> appDeploymentProperties = new HashMap<>();
		appDeploymentProperties.put("propA", "valA");
		appDeploymentProperties.put("propB", "valB");
		WhitelistProperties whitelistProperties = mock(WhitelistProperties.class);
		org.mockito.BDDMockito.given(whitelistProperties
				.qualifyProperties(any(), any()))
				.willReturn(appDeploymentProperties);
		AppDefinition appDefinition = TaskServiceUtils.mergeAndExpandAppProperties(
				taskDefinition,
				mock(Resource.class),
				appDeploymentProperties,
				whitelistProperties);
		assertThat(appDefinition.getProperties().size()).isEqualTo(2);
		assertThat(appDefinition.getProperties().get("propA")).isEqualTo("valA");
		assertThat(appDefinition.getProperties().get("propB")).isEqualTo("valB");
	}

	@Test
	public void testDataFlowUriProperty() throws Exception {
		final String DATA_FLOW_SERVICE_URI = "https://myserver:9191";
		List<String> cmdLineArgs = new ArrayList<>();
		Map<String, String> appDeploymentProperties = new HashMap<>();
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue("dataflowServerUri is expected to be in the app deployment properties",
				appDeploymentProperties.get("dataflowServerUri").equals("https://myserver:9191"));
		appDeploymentProperties.clear();
		appDeploymentProperties.put("dataflow-server-uri", "http://localhost:8080");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue("dataflowServerUri is incorrect",
				appDeploymentProperties.get("dataflow-server-uri").equals("http://localhost:8080"));
		appDeploymentProperties.clear();
		appDeploymentProperties.put("dataflowServerUri", "http://localhost:8191");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue("dataflowServerUri is incorrect",
				appDeploymentProperties.get("dataflowServerUri").equals("http://localhost:8191"));
		appDeploymentProperties.clear();
		appDeploymentProperties.put("DATAFLOW_SERVER_URI", "http://localhost:9000");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue("dataflowServerUri is incorrect",
				appDeploymentProperties.get("DATAFLOW_SERVER_URI").equals("http://localhost:9000"));
		appDeploymentProperties.clear();
		cmdLineArgs.add("--dataflowServerUri=http://localhost:8383");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		cmdLineArgs.clear();
		cmdLineArgs.add("DATAFLOW_SERVER_URI=http://localhost:8383");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue(!appDeploymentProperties.containsKey("DATAFLOW-SERVER-URI"));
	}


	private TaskNode parse(String dsltext) {
		TaskNode ctn = new TaskParser("test", dsltext, true, true).parse();
		return ctn;
	}
}
