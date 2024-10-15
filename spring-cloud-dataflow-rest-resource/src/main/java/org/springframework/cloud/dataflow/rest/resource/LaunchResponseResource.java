package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.hateoas.RepresentationModel;

public class LaunchResponseResource extends RepresentationModel<LaunchResponseResource> {
	private long executionId;

	public LaunchResponseResource() {
	}

	public LaunchResponseResource(long executionId) {
		this.executionId = executionId;
	}

	public long getExecutionId() {
		return executionId;
	}

	public void setExecutionId(long executionId) {
		this.executionId = executionId;
	}

}
