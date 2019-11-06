/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.core;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

/**
 * Description of an execution of a task including resource to be executed and how it was configured via Spring Cloud
 * Data Flow
 *
 * @author Mark Pollack
 * @author Michael Minella
 * @since 2.3
 */
@Entity
@Table(name = "TaskExecutionManifest")
public class TaskExecutionManifest extends AbstractEntity {

	@NotNull
	@Column(name = "task_execution_id")
	private Long taskExecutionId;

	@NotNull
	@Column(name = "task_name")
	private String taskName;

	@NotNull
	@Column(name = "task_execution_manifest")
	@Convert(converter = TaskExecutionManifestConverter.class)
	private Manifest manifest = new Manifest();

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public Manifest getManifest() {
		return manifest;
	}

	public void setManifest(Manifest manifest) {
		this.manifest = manifest;
	}

	/**
	 * Id for the task execution this manifest is related to.
	 *
	 * @return task execution id
	 */
	public Long getTaskExecutionId() {
		return taskExecutionId;
	}

	/**
	 * Set the task execution id this manfiest is related to.
	 *
	 * @param taskExecutionId id of the task execution
	 */
	public void setTaskExecutionId(Long taskExecutionId) {
		this.taskExecutionId = taskExecutionId;
	}

	public static class Manifest {

		private AppDeploymentRequest taskDeploymentRequest;

		private List<AppDeploymentRequest> subTaskDeploymentRequests;

		private String platformName;

		public AppDeploymentRequest getTaskDeploymentRequest() {
			return taskDeploymentRequest;
		}

		public void setTaskDeploymentRequest(AppDeploymentRequest taskDeploymentRequest) {
			this.taskDeploymentRequest = taskDeploymentRequest;
		}

		public List<AppDeploymentRequest> getSubTaskDeploymentRequests() {
			return subTaskDeploymentRequests;
		}

		public void setSubTaskDeploymentRequests(List<AppDeploymentRequest> subTaskDeploymentRequests) {
			this.subTaskDeploymentRequests = subTaskDeploymentRequests;
		}

		public String getPlatformName() {
			return platformName;
		}

		public void setPlatformName(String platformName) {
			this.platformName = platformName;
		}
	}
}
