package org.springframework.cloud.dataflow.aggregate.task;

import org.springframework.cloud.dataflow.core.TaskDeployment;

public interface TaskDeploymentReader {
	TaskDeployment getDeployment(String externalTaskId);
	TaskDeployment findByDefinitionName(String definitionName);
}
