package org.springframework.cloud.dataflow.server.task;

import org.springframework.cloud.dataflow.core.TaskDeployment;

public interface TaskDeploymentReader {
	TaskDeployment getDeployment(String externalTaskId);
	TaskDeployment getDeployment(String externalTaskId, String platform);
	TaskDeployment findByDefinitionName(String definitionName);
}
