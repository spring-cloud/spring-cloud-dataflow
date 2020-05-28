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

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.validation.annotation.Validated;


/**
 * Properties used to define the behavior of tasks created by Spring Cloud Data Flow.
 *
 * @author Glenn Renfro
 * @author David Turanski
 */
@Validated
@ConfigurationProperties(prefix = TaskConfigurationProperties.COMPOSED_TASK_PREFIX)
public class TaskConfigurationProperties {

	public static final String COMPOSED_TASK_PREFIX = DataFlowPropertyKeys.PREFIX + "task";

	public static final String COMPOSED_TASK_RUNNER_NAME = "composed-task-runner";

	/**
	 * The task application uri to be used for the composed task runner.
	 */
	@NotBlank
	private String composedTaskRunnerUri = "composed-task-runner";

	/**
	 * Whether the server should auto create task definitions if one does not exist for a launch request and
	 * a registered task application with the same name does exist.
	 */
	private boolean autoCreateTaskDefinitions;

	/**
	 * The properties for showing deployer properties.
	 */
	private DeployerProperties deployerProperties = new DeployerProperties();

	public String getComposedTaskRunnerUri() {
		return composedTaskRunnerUri;
	}

	public void setComposedTaskRunnerUri(String composedTaskRunnerUri) {
		this.composedTaskRunnerUri = composedTaskRunnerUri;
	}

	public DeployerProperties getDeployerProperties() {
		return deployerProperties;
	}

	public void setDeployerProperties(DeployerProperties deployerProperties) {
		this.deployerProperties = deployerProperties;
	}

	public boolean isAutoCreateTaskDefinitions() {
		return autoCreateTaskDefinitions;
	}

	public void setAutoCreateTaskDefinitions(boolean autoCreateTaskDefinitions) {
		this.autoCreateTaskDefinitions = autoCreateTaskDefinitions;
	}

	public static class DeployerProperties {

		private String[] propertyIncludes = new String[0];
		private String[] groupIncludes = new String[0];
		private String[] propertyExcludes = new String[0];
		private String[] groupExcludes = new String[0];

		public String[] getPropertyIncludes() {
			return propertyIncludes;
		}

		public void setPropertyIncludes(String[] propertyIncludes) {
			this.propertyIncludes = propertyIncludes;
		}

		public String[] getGroupIncludes() {
			return groupIncludes;
		}

		public void setGroupIncludes(String[] groupIncludes) {
			this.groupIncludes = groupIncludes;
		}

		public String[] getPropertyExcludes() {
			return propertyExcludes;
		}

		public void setPropertyExcludes(String[] propertyExcludes) {
			this.propertyExcludes = propertyExcludes;
		}

		public String[] getGroupExcludes() {
			return groupExcludes;
		}

		public void setGroupExcludes(String[] groupExcludes) {
			this.groupExcludes = groupExcludes;
		}
	}
}
