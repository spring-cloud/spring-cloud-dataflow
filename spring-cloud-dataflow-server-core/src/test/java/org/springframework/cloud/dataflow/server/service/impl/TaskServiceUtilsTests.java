/*
 * Copyright 2018-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
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

/**
 * Verifies the behavior of the methods in the utility.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
public class TaskServiceUtilsTests {
	public static final String BASE_GRAPH = "AAA && BBB";


	@Test
	void testCreateComposedTaskDefinition() {
		assertThat(TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH)).isEqualTo("composed-task-runner --graph=\"AAA && BBB\"");
	}

	@Test
	void createComposeTaskDefinitionNullNameCheck() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH);
			TaskServiceUtils.createComposedTaskDefinition(null);
		});
	}

	@Test
	void createComposeTaskDefinitionNullProperties() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			TaskServiceUtils.createComposedTaskDefinition(BASE_GRAPH, null);
		});
	}

	@Test
	void ctrPropertyReplacement() {
		TaskNode node = parse("AAA && BBB");
		Map<String, String> taskDeploymentProperties = new HashMap<>();
		taskDeploymentProperties.put("app.test.BBB.timestamp.format", "aformat");
		taskDeploymentProperties.put("deployer.test.BBB.foo", "bar");
		taskDeploymentProperties = TaskServiceUtils.establishComposedTaskProperties(
				taskDeploymentProperties,
				node);
		assertThat(taskDeploymentProperties).hasSize(1);
		assertThat(taskDeploymentProperties).containsEntry("app.composed-task-runner.composed-task-properties", "app.test-BBB.app.BBB.timestamp.format=aformat, deployer.test-BBB.deployer.BBB.foo=bar");
	}

	@Test
	void databasePropUpdate() {
		TaskDefinition taskDefinition = new TaskDefinition("testTask", "testApp");
		DataSourceProperties dataSourceProperties = getDataSourceProperties();
		TaskDefinition definition = TaskServiceUtils.updateTaskProperties(
				taskDefinition,
				dataSourceProperties, true);
		assertThat(definition.getProperties()).hasSize(5);
		assertThat(definition.getProperties()).containsEntry("spring.datasource.url", "myUrl");
		assertThat(definition.getProperties()).containsEntry("spring.datasource.driverClassName", "myDriver");
		assertThat(definition.getProperties()).containsEntry("spring.datasource.username", "myUser");
		assertThat(definition.getProperties()).containsEntry("spring.datasource.password", "myPassword");

		definition = TaskServiceUtils.updateTaskProperties(
				taskDefinition,
				dataSourceProperties, false);
		assertThat(definition.getProperties()).hasSize(3);
		assertThat(definition.getProperties()).containsEntry("spring.datasource.url", "myUrl");
		assertThat(definition.getProperties()).containsEntry("spring.datasource.driverClassName", "myDriver");
	}

	@Test
	void databasePropUpdateWithPlatform() {
		TaskDefinition taskDefinition = new TaskDefinition("testTask", "testApp");
		DataSourceProperties dataSourceProperties = getDataSourceProperties();
		TaskDefinition definition = TaskServiceUtils.updateTaskProperties(
				taskDefinition,
				dataSourceProperties, false);

		validateProperties(definition, 3);
		assertThat(definition.getProperties()).containsEntry("spring.datasource.driverClassName", "myDriver");
	}

	@Test
	void databasePropUpdateWithPlatformForUserDriverClassName() {
		TaskDefinition definition = createUpdatedDefinitionForProperty("spring.datasource.driverClassName", "foobar");
		validateProperties(definition, 2);
		assertThat(definition.getProperties()).containsEntry("spring.datasource.driverClassName", "foobar");

		definition = createUpdatedDefinitionForProperty("spring.datasource.driver-class-name", "feebar");
		validateProperties(definition, 2);
		assertThat(definition.getProperties()).containsEntry("spring.datasource.driver-class-name", "feebar");

		definition = createUpdatedDefinitionForProperty(null, null);
		validateProperties(definition, 2);
		assertThat(definition.getProperties()).containsEntry("spring.datasource.driverClassName", "myDriver");
	}

	@Test
	void databasePropUpdateWithPlatformForUrl() {
		TaskDefinition definition = createUpdatedDefinitionForProperty("spring.datasource.url", "newurl");
		assertThat(definition.getProperties()).containsEntry("spring.datasource.url", "newurl");

		definition = createUpdatedDefinitionForProperty(null, null);
		assertThat(definition.getProperties()).containsEntry("spring.datasource.url", "myUrl");
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
		assertThat(definition.getProperties()).hasSize(size);
		assertThat(definition.getProperties()).containsEntry("spring.datasource.url", "myUrl");
		assertThat(definition.getProperties().get("spring.datasource.username")).isNull();
		assertThat(definition.getProperties().get("spring.datasource.password")).isNull();
	}

	@Test
	void testExtractAppProperties() {
		Map<String, String> taskDeploymentProperties = new HashMap<>();
		taskDeploymentProperties.put("app.test.foo", "bar");
		taskDeploymentProperties.put("test.none", "boo");
		taskDeploymentProperties.put("app.test.test", "baz");
		taskDeploymentProperties.put("app.none.test", "boo");
		Map<String, String> result = TaskServiceUtils.extractAppProperties("test",
				taskDeploymentProperties);

		assertThat(result).hasSize(2);
		assertThat(result).containsEntry("foo", "bar");
		assertThat(result).containsEntry("test", "baz");
	}

	@Test
	void extractAppLabelProperties() {
		Map<String, String> taskDeploymentProperties = new HashMap<>();
		taskDeploymentProperties.put("app.myapplabel.foo", "bar");
		taskDeploymentProperties.put("myappname.none", "boo");
		taskDeploymentProperties.put("app.myappname.foo", "boo");
		taskDeploymentProperties.put("app.myapplabel.myprop", "baz");
		taskDeploymentProperties.put("app.none.test", "boo");
		Map<String, String> result = TaskServiceUtils.extractAppProperties("myappname", "myapplabel",
				taskDeploymentProperties);

		assertThat(result).hasSize(2);
		assertThat(result).containsEntry("foo", "bar");
		assertThat(result).containsEntry("myprop", "baz");
	}

	@Test
	void testMergeAndExpandAppProperties() {
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
		assertThat(appDefinition.getProperties()).hasSize(2);
		assertThat(appDefinition.getProperties()).containsEntry("propA", "valA");
		assertThat(appDefinition.getProperties()).containsEntry("propB", "valB");
	}

	@Test
	void dataFlowUriProperty() throws Exception {
		final String DATA_FLOW_SERVICE_URI = "https://myserver:9191";
		List<String> cmdLineArgs = new ArrayList<>();
		Map<String, String> appDeploymentProperties = new HashMap<>();
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertThat(appDeploymentProperties).containsKey("dataflowServerUri");
		assertThat(appDeploymentProperties.get("dataflowServerUri")).as("dataflowServerUri is expected to be in the app deployment properties").isEqualTo("https://myserver:9191");
		appDeploymentProperties.clear();
		appDeploymentProperties.put("dataflow-server-uri", "http://localhost:8080");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertThat(appDeploymentProperties.containsKey("dataflowServerUri")).isFalse();
		assertThat(appDeploymentProperties.get("dataflow-server-uri")).as("dataflowServerUri is incorrect").isEqualTo("http://localhost:8080");
		appDeploymentProperties.clear();
		appDeploymentProperties.put("dataflowServerUri", "http://localhost:8191");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertThat(appDeploymentProperties).containsKey("dataflowServerUri");
		assertThat(appDeploymentProperties.get("dataflowServerUri")).as("dataflowServerUri is incorrect").isEqualTo("http://localhost:8191");
		appDeploymentProperties.clear();
		appDeploymentProperties.put("DATAFLOW_SERVER_URI", "http://localhost:9000");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertThat(appDeploymentProperties.containsKey("dataflowServerUri")).isFalse();
		assertThat(appDeploymentProperties.get("DATAFLOW_SERVER_URI")).as("dataflowServerUri is incorrect").isEqualTo("http://localhost:9000");
		appDeploymentProperties.clear();
		cmdLineArgs.add("--dataflowServerUri=http://localhost:8383");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertThat(appDeploymentProperties.containsKey("dataflowServerUri")).isFalse();
		cmdLineArgs.clear();
		cmdLineArgs.add("DATAFLOW_SERVER_URI=http://localhost:8383");
		TaskServiceUtils.updateDataFlowUriIfNeeded(DATA_FLOW_SERVICE_URI, appDeploymentProperties, cmdLineArgs);
		assertThat(appDeploymentProperties.containsKey("dataflowServerUri")).isFalse();
		assertThat(appDeploymentProperties.containsKey("DATAFLOW-SERVER-URI")).isFalse();
	}

	@Test
	void addProvidedImagePullSecret() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setImagePullSecret("regcred");

		Map<String, String> taskDeploymentProperties = new HashMap<>();

		TaskServiceUtils.addImagePullSecretProperty(taskDeploymentProperties, composedTaskRunnerConfigurationProperties);

		String imagePullSecretPropertyKey = "deployer.composed-task-runner.kubernetes.imagePullSecret";

		assertThat(taskDeploymentProperties.containsKey(imagePullSecretPropertyKey)).as("Task deployment properties are missing composed task runner imagePullSecret").isTrue();

		assertThat(taskDeploymentProperties.get(imagePullSecretPropertyKey)).as("Invalid imagePullSecret").isEqualTo("regcred");
	}

	@Test
	void composedTaskRunnerUriFromTaskProps() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(composedTaskRunnerConfigurationProperties);
		taskConfigurationProperties.setComposedTaskRunnerUri("docker://something");

		String uri = TaskServiceUtils.getComposedTaskLauncherUri(taskConfigurationProperties,
				composedTaskRunnerConfigurationProperties);

		assertThat(uri).as("Invalid task runner URI string").isEqualTo("docker://something");
	}

	@Test
	void composedTaskRunnerUriFromCTRProps() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUri("docker://something");

		String uri = TaskServiceUtils.getComposedTaskLauncherUri(new TaskConfigurationProperties(),
				composedTaskRunnerConfigurationProperties);

		assertThat(uri).as("Invalid task runner URI string").isEqualTo("docker://something");
	}

	@Test
	void composedTaskRunnerUriFromCTRPropsOverridesTaskProps() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUri("gcr.io://something");

		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(composedTaskRunnerConfigurationProperties);
		taskConfigurationProperties.setComposedTaskRunnerUri("docker://something");

		String uri = TaskServiceUtils.getComposedTaskLauncherUri(taskConfigurationProperties,
				composedTaskRunnerConfigurationProperties);

		assertThat(uri).as("Invalid task runner URI string").isEqualTo("gcr.io://something");
	}

	@Test
	void imagePullSecretNullCTRProperties() {
		Map<String, String> taskDeploymentProperties = new HashMap<>();
		TaskServiceUtils.addImagePullSecretProperty(taskDeploymentProperties, null);
		assertThat(taskDeploymentProperties.containsKey("deployer.composed-task-runner.kubernetes.imagePullSecret")).as("Task deployment properties should not contain imagePullSecret").isFalse();
	}

	@Test
	void useUserAccessTokenFromCTRPropsEnabled() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUseUserAccessToken(true);

		boolean result = TaskServiceUtils.isUseUserAccessToken(null, composedTaskRunnerConfigurationProperties);

		assertThat(result).as("Use user access token should be true").isTrue();
	}

	@Test
	void useUserAccessTokenFromCTRPropsDisabled() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
				new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUseUserAccessToken(false);

		boolean result = TaskServiceUtils.isUseUserAccessToken(null, composedTaskRunnerConfigurationProperties);

		assertThat(result).as("Use user access token should be false").isFalse();
	}

	@Test
	void useUserAccessTokenFromNullCTRProps() {
		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(new ComposedTaskRunnerConfigurationProperties());

		boolean result = TaskServiceUtils.isUseUserAccessToken(taskConfigurationProperties, null);

		assertThat(result).as("Use user access token should be false").isFalse();
	}

	@Test
	void useUserAccessTokenFromTaskProps() {
		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(new ComposedTaskRunnerConfigurationProperties());
		taskConfigurationProperties.setUseUserAccessToken(true);

		boolean result = TaskServiceUtils.isUseUserAccessToken(taskConfigurationProperties, null);

		assertThat(result).as("Use user access token should be true").isTrue();
	}

	@Test
	void useUserAccessTokenFromTaskPropsDefault() {
		TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
		taskConfigurationProperties.setComposedTaskRunnerConfigurationProperties(new ComposedTaskRunnerConfigurationProperties());

		boolean result = TaskServiceUtils.isUseUserAccessToken(taskConfigurationProperties, null);

		assertThat(result).as("Use user access token should be false").isFalse();
	}

	@Test
	void convertCommandLineArgsToCTRFormat() {
		validateSingleCTRArgs("app.a.0=foo=bar", "--composed-task-app-arguments.base64_YXBwLmEuMA=foo=bar");
		validateSingleCTRArgs("app.a.0=foo", "--composed-task-app-arguments.base64_YXBwLmEuMA=foo");
		validateSingleCTRArgs("app.foo.bar", "--composed-task-app-arguments.app.foo.bar");
		validateSingleCTRArgs("foo=bar", "foo=bar");
	}

	@Test
	void convertCommandLineArgsToCTRFormatWithNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						TaskServiceUtils.convertCommandLineArgsToCTRFormat(Collections.singletonList(null)))
				.withMessage("Command line Arguments for ComposedTaskRunner contain a null entry.");
	}

	@Test
	void convertMultipleCommandLineArgsToCTRFormat() {
		List<String> originalList = new ArrayList<>();
		originalList.add("app.a.0=foo=bar");
		originalList.add("app.b.0=baz=boo");
		originalList.add("app.c.0=val=buz");
		List<String> expectedList = new ArrayList<>();
		expectedList.add("--composed-task-app-arguments.base64_YXBwLmEuMA=foo=bar");
		expectedList.add("--composed-task-app-arguments.base64_YXBwLmIuMA=baz=boo");
		expectedList.add("--composed-task-app-arguments.base64_YXBwLmMuMA=val=buz");
		validateCTRArgs(expectedList, TaskServiceUtils.convertCommandLineArgsToCTRFormat(originalList));
	}

	private void validateSingleCTRArgs(String original, String expected) {
		List<String> testArgs = Collections.singletonList(original);
		validateCTRArgs(Collections.singletonList(expected),
				TaskServiceUtils.convertCommandLineArgsToCTRFormat(testArgs));

	}

	private void validateCTRArgs(List<String> expectedList, List<String> resultList) {
		assertThat(resultList).containsExactlyInAnyOrderElementsOf(expectedList);
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
