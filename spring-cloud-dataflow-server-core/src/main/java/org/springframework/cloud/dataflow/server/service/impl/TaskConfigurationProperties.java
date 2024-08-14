/*
 * Copyright 2018-2024 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Properties used to define the behavior of tasks created by Spring Cloud Data Flow.
 *
 * @author Glenn Renfro
 * @author David Turanski
 * @author Chris Schaefer
 */
@Validated
@ConfigurationProperties(prefix = TaskConfigurationProperties.TASK_PREFIX)
public class TaskConfigurationProperties {
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskConfigurationProperties.class);

	public static final String TASK_PREFIX = DataFlowPropertyKeys.PREFIX + "task";

	@Deprecated
	public static final String COMPOSED_TASK_RUNNER_NAME = "composed-task-runner";

	@Autowired
	private ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	/**
	 * Whether the server should auto create task definitions if one does not exist for a launch request and
	 * a registered task application with the same name does exist.
	 */
	private boolean autoCreateTaskDefinitions;

	/**
	 * The number of task executions that will be deleted in a chunk when executing bulk deletes.
	 * Default is zero which means no chunking.
	 */
	private int executionDeleteChunkSize = 0;

	/**
	 * The properties for showing deployer properties.
	 */
	private DeployerProperties deployerProperties = new DeployerProperties();

	/**
	 * When using the kubernetes platform obtain database username and password
	 * from secrets vs having dataflow pass them via properties.
	 */
	private boolean useKubernetesSecretsForDbCredentials;

	/**
	 * Controls the style that Dataflow reconstitutes job parameters when re-running a
	 * failed batch job. The style will be taken from Spring Batch's
	 * DefaultJobParametersConverter when set to false or JsonJobParametersConverter when true.
	 */

	private boolean useJsonJobParameters = false;

	@Deprecated
	public String getComposedTaskRunnerUri() {
		logDeprecationWarning("getUri");
		return this.composedTaskRunnerConfigurationProperties.getUri();
	}

	@Deprecated
	public void setComposedTaskRunnerUri(String composedTaskRunnerUri) {
		logDeprecationWarning("setUri");

		if (!StringUtils.hasText(this.composedTaskRunnerConfigurationProperties.getUri())) {
			this.composedTaskRunnerConfigurationProperties.setUri(composedTaskRunnerUri);
		}
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

	@Deprecated
	public boolean isUseUserAccessToken() {
		logDeprecationWarning();

		return this.composedTaskRunnerConfigurationProperties.isUseUserAccessToken() == null ? false :
				this.composedTaskRunnerConfigurationProperties.isUseUserAccessToken();
	}

	@Deprecated
	public void setUseUserAccessToken(boolean useUserAccessToken) {
		logDeprecationWarning();

		if (this.composedTaskRunnerConfigurationProperties.isUseUserAccessToken() == null) {
			this.composedTaskRunnerConfigurationProperties.setUseUserAccessToken(useUserAccessToken);
		}
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

	public boolean isUseKubernetesSecretsForDbCredentials() {
		return useKubernetesSecretsForDbCredentials;
	}

	public void setUseKubernetesSecretsForDbCredentials(boolean useKubernetesSecretsForDbCredentials) {
		this.useKubernetesSecretsForDbCredentials = useKubernetesSecretsForDbCredentials;
	}

	private void logDeprecationWarning() {
		logDeprecationWarning(null);
	}

	private void logDeprecationWarning(String newMethodName) {
		String callingMethodName = Thread.currentThread().getStackTrace()[2].getMethodName();
		LOGGER.warn(this.getClass().getName() + "." + callingMethodName
				+ " is deprecated. Please use " + ComposedTaskRunnerConfigurationProperties.class.getName() + "."
				+ (newMethodName != null ? newMethodName : callingMethodName));
	}

	void setComposedTaskRunnerConfigurationProperties(ComposedTaskRunnerConfigurationProperties
							composedTaskRunnerConfigurationProperties) {
		if (composedTaskRunnerConfigurationProperties != null) {
			this.composedTaskRunnerConfigurationProperties = composedTaskRunnerConfigurationProperties;
		}
	}

	public int getExecutionDeleteChunkSize() {
		return executionDeleteChunkSize;
	}

	public void setExecutionDeleteChunkSize(int executionDeleteChunkSize) {
		this.executionDeleteChunkSize = executionDeleteChunkSize;
	}

	public boolean isUseJsonJobParameters() {
		return useJsonJobParameters;
	}

	public void setUseJsonJobParameters(boolean useJsonJobParameters) {
		this.useJsonJobParameters = useJsonJobParameters;
	}
}
