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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.TaskNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.server.controller.VisibleProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * Verifies the behavior of the methods in the utility.
 *
 * @author Glenn Renfro
 */
public class TaskServiceUtilsTests {
	public static final String BASE_GRAPH = "AAA && BBB";

	@Test
	public void testCreateComposedTaskDefinition() {
		assertThat(TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH)).isEqualTo("composed-task-runner --graph=\"AAA && BBB\"");
	}

	@Test
	public void testCreateComposeTaskDefinitionNullNameCheck() {
		assertThrows(IllegalArgumentException.class, () -> {
			TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH);
			TaskServiceUtils.createComposedTaskDefinition(null);
		});
	}

	@Test
	public void testCreateComposeTaskDefinitionNullProperties() {
		assertThrows(IllegalArgumentException.class, () -> {
			TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH, null);
		});
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
		DataSourceProperties dataSourceProperties = getDataSourceProperties();
		TaskDefinition definition = TaskServiceUtils.updateTaskProperties(
				taskDefinition,
				dataSourceProperties, true);
		assertThat(definition.getProperties().size()).isEqualTo(5);
		assertThat(definition.getProperties().get("spring.datasource.url")).isEqualTo("myUrl");
		assertThat(definition.getProperties().get("spring.datasource.driverClassName")).isEqualTo("myDriver");
		assertThat(definition.getProperties().get("spring.datasource.username")).isEqualTo("myUser");
		assertThat(definition.getProperties().get("spring.datasource.password")).isEqualTo("myPassword");

		definition = TaskServiceUtils.updateTaskProperties(
				taskDefinition,
				dataSourceProperties, false);
		assertThat(definition.getProperties().size()).isEqualTo(3);
		assertThat(definition.getProperties().get("spring.datasource.url")).isEqualTo("myUrl");
		assertThat(definition.getProperties().get("spring.datasource.driverClassName")).isEqualTo("myDriver");
	}

	@Test
	public void testDatabasePropUpdateWithPlatform() {
		TaskDefinition taskDefinition = new TaskDefinition("testTask", "testApp");
		DataSourceProperties dataSourceProperties = getDataSourceProperties();
		TaskDefinition definition = TaskServiceUtils.updateTaskProperties(
				taskDefinition,
				dataSourceProperties, false);

		validateProperties(definition, 3);
		assertThat(definition.getProperties().get("spring.datasource.driverClassName")).isEqualTo("myDriver");
	}

	@Test
	public void testDatabasePropUpdateWithPlatformForUserDriverClassName() {
		TaskDefinition definition = createUpdatedDefinitionForProperty("spring.datasource.driverClassName", "foobar");
		validateProperties(definition, 2);
		assertThat(definition.getProperties().get("spring.datasource.driverClassName")).isEqualTo("foobar");

		definition = createUpdatedDefinitionForProperty("spring.datasource.driver-class-name", "feebar");
		validateProperties(definition, 2);
		assertThat(definition.getProperties().get("spring.datasource.driver-class-name")).isEqualTo("feebar");

		definition = createUpdatedDefinitionForProperty(null, null);
		validateProperties(definition, 2);
		assertThat(definition.getProperties().get("spring.datasource.driverClassName")).isEqualTo("myDriver");
	}

	@Test
	public void testDatabasePropUpdateWithPlatformForUrl() {
		TaskDefinition definition = createUpdatedDefinitionForProperty("spring.datasource.url", "newurl");
		assertThat(definition.getProperties().get("spring.datasource.url")).isEqualTo("newurl");

		definition = createUpdatedDefinitionForProperty(null, null);
		assertThat(definition.getProperties().get("spring.datasource.url")).isEqualTo("myUrl");
	}

	private TaskDefinition createUpdatedDefinitionForProperty(String key, String value) {
		Map<String, String> props = new HashMap<>();
		if(StringUtils.hasText(key) && StringUtils.hasText(value)) {
			props.put(key, value);
		}
		TaskDefinition taskDefinition = (new TaskDefinition.TaskDefinitionBuilder()).
				addProperties(props).
				setTaskName("testTask").
				setRegisteredAppName("testApp").
				build();
		DataSourceProperties dataSourceProperties = getDataSourceProperties();
		return TaskServiceUtils.updateTaskProperties(
				taskDefinition,
				dataSourceProperties, false);
	}

	private void validateProperties(TaskDefinition definition, int size) {
		assertThat(definition.getProperties().size()).isEqualTo(size);
		assertThat(definition.getProperties().get("spring.datasource.url")).isEqualTo("myUrl");
		assertThat(definition.getProperties().get("spring.datasource.username")).isNull();
		assertThat(definition.getProperties().get("spring.datasource.password")).isNull();
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
		VisibleProperties visibleProperties = mock(VisibleProperties.class);
		org.mockito.BDDMockito.given(visibleProperties
				.qualifyProperties(any(), any()))
				.willReturn(appDeploymentProperties);
		AppDefinition appDefinition = TaskServiceUtils.mergeAndExpandAppProperties(
				taskDefinition,
				mock(Resource.class),
				appDeploymentProperties,
				visibleProperties);
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
		assertTrue(appDeploymentProperties.get("dataflowServerUri").equals("https://myserver:9191"),
				"dataflowServerUri is expected to be in the app deployment properties");
		appDeploymentProperties.clear();
		appDeploymentProperties.put("dataflow-server-uri", "http://localhost:8080");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue(appDeploymentProperties.get("dataflow-server-uri").equals("http://localhost:8080"),
				"dataflowServerUri is incorrect");
		appDeploymentProperties.clear();
		appDeploymentProperties.put("dataflowServerUri", "http://localhost:8191");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue(appDeploymentProperties.get("dataflowServerUri").equals("http://localhost:8191"),
				"dataflowServerUri is incorrect");
		appDeploymentProperties.clear();
		appDeploymentProperties.put("DATAFLOW_SERVER_URI", "http://localhost:9000");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue(appDeploymentProperties.get("DATAFLOW_SERVER_URI").equals("http://localhost:9000"),
				"dataflowServerUri is incorrect");
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

	@Test
	public void testAddProvidedImagePullSecret() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setImagePullSecret("regcred");

		Map<String, String> taskDeploymentProperties = new HashMap<>();

		TaskServiceUtils.addImagePullSecretProperty(taskDeploymentProperties, composedTaskRunnerConfigurationProperties);

		String imagePullSecretPropertyKey = "deployer.composed-task-runner.kubernetes.imagePullSecret";

		assertTrue(taskDeploymentProperties.containsKey(imagePullSecretPropertyKey),
				"Task deployment properties are missing composed task runner imagePullSecret");

		assertEquals("regcred", taskDeploymentProperties.get(imagePullSecretPropertyKey), "Invalid imagePullSecret");
	}

	@Test
	public void testComposedTaskRunnerUriFromTaskProps() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(composedTaskRunnerConfigurationProperties);
		taskConfigurationProperties.setComposedTaskRunnerUri("docker://something");

		String uri = TaskServiceUtils.getComposedTaskLauncherUri(taskConfigurationProperties,
				composedTaskRunnerConfigurationProperties);

		assertEquals("docker://something", uri, "Invalid task runner URI string");
	}

	@Test
	public void testComposedTaskRunnerUriFromCTRProps() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUri("docker://something");

		String uri = TaskServiceUtils.getComposedTaskLauncherUri(new TaskConfigurationProperties(),
				composedTaskRunnerConfigurationProperties);

		assertEquals("docker://something", uri, "Invalid task runner URI string");
	}

	@Test
	public void testComposedTaskRunnerUriFromCTRPropsOverridesTaskProps() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUri("gcr.io://something");

		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(composedTaskRunnerConfigurationProperties);
		taskConfigurationProperties.setComposedTaskRunnerUri("docker://something");

		String uri = TaskServiceUtils.getComposedTaskLauncherUri(taskConfigurationProperties,
				composedTaskRunnerConfigurationProperties);

		assertEquals("gcr.io://something", uri, "Invalid task runner URI string");
	}

	@Test
	public void testImagePullSecretNullCTRProperties() {
		Map<String, String> taskDeploymentProperties = new HashMap<>();
		TaskServiceUtils.addImagePullSecretProperty(taskDeploymentProperties, null);
		assertFalse(taskDeploymentProperties.containsKey("deployer.composed-task-runner.kubernetes.imagePullSecret"),
				"Task deployment properties should not contain imagePullSecret");
	}

	@Test
	public void testUseUserAccessTokenFromCTRPropsEnabled() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUseUserAccessToken(true);

		boolean result = TaskServiceUtils.isUseUserAccessToken(null, composedTaskRunnerConfigurationProperties);

		assertTrue(result, "Use user access token should be true");
	}

	@Test
	public void testUseUserAccessTokenFromCTRPropsDisabled() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUseUserAccessToken(false);

		boolean result = TaskServiceUtils.isUseUserAccessToken(null, composedTaskRunnerConfigurationProperties);

		assertFalse(result, "Use user access token should be false");
	}

	@Test
	public void testUseUserAccessTokenFromNullCTRProps() {
		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(new ComposedTaskRunnerConfigurationProperties());

		boolean result = TaskServiceUtils.isUseUserAccessToken(taskConfigurationProperties, null);

		assertFalse(result, "Use user access token should be false");
	}

	@Test
	public void testUseUserAccessTokenFromTaskProps() {
		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(new ComposedTaskRunnerConfigurationProperties());
		taskConfigurationProperties.setUseUserAccessToken(true);

		boolean result = TaskServiceUtils.isUseUserAccessToken(taskConfigurationProperties, null);

		assertTrue(result, "Use user access token should be true");
	}

	@Test
	public void testUseUserAccessTokenFromTaskPropsDefault() {
		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(new ComposedTaskRunnerConfigurationProperties());

		boolean result = TaskServiceUtils.isUseUserAccessToken(taskConfigurationProperties, null);

		assertFalse(result, "Use user access token should be false");
	}

	private TaskNode parse(String dsltext) {
		TaskNode ctn = new TaskParser("test", dsltext, true, true).parse();
		return ctn;
	}

	private DataSourceProperties getDataSourceProperties() {
		DataSourceProperties dataSourceProperties = new DataSourceProperties();
		dataSourceProperties.setUsername("myUser");
		dataSourceProperties.setDriverClassName("myDriver");
		dataSourceProperties.setPassword("myPassword");
		dataSourceProperties.setUrl("myUrl");
		return dataSourceProperties;
	}
}
