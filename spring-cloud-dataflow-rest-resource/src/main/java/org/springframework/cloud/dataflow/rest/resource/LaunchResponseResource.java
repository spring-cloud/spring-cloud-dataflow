package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.hateoas.RepresentationModel;

public class LaunchResponseResource extends RepresentationModel<LaunchResponseResource> {
	private long executionId;
	private String schemaTarget;

	public LaunchResponseResource() {
	}

	public LaunchResponseResource(long executionId, String schemaTarget) {
		this.executionId = executionId;
		this.schemaTarget = schemaTarget;
	}

	public long getExecutionId() {
		return executionId;
	}

	public String getSchemaTarget() {
		return schemaTarget;
	}

	public void setExecutionId(long executionId) {
		this.executionId = executionId;
	}

	public void setSchemaTarget(String schemaTarget) {
		this.schemaTarget = schemaTarget;
	}
}
