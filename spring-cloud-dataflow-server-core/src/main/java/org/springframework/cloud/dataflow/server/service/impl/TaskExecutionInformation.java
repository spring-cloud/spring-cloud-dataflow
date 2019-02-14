/*
 * Copyright 2015-2018 the original author or authors.
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

import java.util.Map;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.core.io.Resource;

/**
 * Stores execution information.
 *
 * @author Daniel Serleg
 */
public class TaskExecutionInformation {
	private boolean composed;

	private Map<String, String> taskDeploymentProperties;

	private TaskDefinition taskDefinition;

	private Resource appResource;

	private Resource metadataResource;
	private TaskDefinition originalTaskDefinition;

	public TaskExecutionInformation() {
	}

	public boolean isComposed() {
		return composed;
	}

	public void setComposed(boolean composed) {
		this.composed = composed;
	}

	public Map<String, String> getTaskDeploymentProperties() {
		return taskDeploymentProperties;
	}

	public void setTaskDeploymentProperties(Map<String, String> taskDeploymentProperties) {
		this.taskDeploymentProperties = taskDeploymentProperties;
	}

	public TaskDefinition getTaskDefinition() {
		return taskDefinition;
	}

	public void setTaskDefinition(TaskDefinition taskDefinition) {
		this.taskDefinition = taskDefinition;
	}

	public Resource getAppResource() {
		return appResource;
	}

	public void setAppResource(Resource appResource) {
		this.appResource = appResource;
	}

	public Resource getMetadataResource() {
		return metadataResource;
	}

	public void setMetadataResource(Resource metadataResource) {
		this.metadataResource = metadataResource;
	}

	public void setOriginalTaskDefinition(TaskDefinition originalTaskDefinition) {
		this.originalTaskDefinition = originalTaskDefinition;
	}

	public TaskDefinition getOriginalTaskDefinition() {
		return originalTaskDefinition;
	}
}
