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

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Records the association of taskDeploymentId to the platfornName the task was launched
 * on.
 * @author Mark Pollack
 */
@Entity
@Table(name = "TaskDeployment")
@EntityListeners(AuditingEntityListener.class)
public class TaskDeployment extends AbstractEntity {

	@NotNull
	@Column(name = "task_deployment_id")
	private String taskDeploymentId;

	@NotNull
	@Column(name = "platform_name")
	private String platformName;

	@NotNull
	@Column(name = "task_definition_name")
	private String taskDefinitionName;

	@CreatedDate
	@Column(name = "created_on")
	private Instant createdOn;

	public TaskDeployment() {
	}

	public String getTaskDeploymentId() {
		return taskDeploymentId;
	}

	public void setTaskDeploymentId(String taskDeploymentId) {
		this.taskDeploymentId = taskDeploymentId;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public String getTaskDefinitionName() {
		return taskDefinitionName;
	}

	public void setTaskDefinitionName(String taskDefinitionName) {
		this.taskDefinitionName = taskDefinitionName;
	}

	public Instant getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Instant createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("TaskDeployment{");
		sb.append("taskDeploymentId='").append(taskDeploymentId).append('\'');
		sb.append(", platformName='").append(platformName).append('\'');
		sb.append(", taskDefinitionName='").append(taskDefinitionName).append('\'');
		sb.append(", createdOn=").append(createdOn);
		sb.append('}');
		return sb.toString();
	}
}
