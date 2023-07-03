package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.hateoas.RepresentationModel;

public class LaunchResponseResource extends RepresentationModel<LaunchResponseResource> {
	private long taskId;
	private String schemaTarget;

	public LaunchResponseResource() {
	}

	public LaunchResponseResource(long taskId, String schemaTarget) {
		this.taskId = taskId;
		this.schemaTarget = schemaTarget;
	}

	public long getTaskId() {
		return taskId;
	}

	public String getSchemaTarget() {
		return schemaTarget;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public void setSchemaTarget(String schemaTarget) {
		this.schemaTarget = schemaTarget;
	}
}
