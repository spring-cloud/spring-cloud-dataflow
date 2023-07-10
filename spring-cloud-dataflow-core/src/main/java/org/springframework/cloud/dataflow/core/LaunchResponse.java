package org.springframework.cloud.dataflow.core;

import java.util.Objects;

public class LaunchResponse {
	private long executionId;

	private String schemaTarget;

	public LaunchResponse() {
	}

	public LaunchResponse(long executionId, String schemaTarget) {
		this.executionId = executionId;
		this.schemaTarget = schemaTarget;
	}

	public long getExecutionId() {
		return executionId;
	}

	public void setExecutionId(long executionId) {
		this.executionId = executionId;
	}

	public String getSchemaTarget() {
		return schemaTarget;
	}

	public void setSchemaTarget(String schemaTarget) {
		this.schemaTarget = schemaTarget;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LaunchResponse that = (LaunchResponse) o;

		if (executionId != that.executionId) return false;
		return Objects.equals(schemaTarget, that.schemaTarget);
	}

	@Override
	public int hashCode() {
		int result = (int) (executionId ^ (executionId >>> 32));
		result = 31 * result + (schemaTarget != null ? schemaTarget.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "LaunchResponse{" +
				"taskId=" + executionId +
				", schemaTarget='" + schemaTarget + '\'' +
				'}';
	}
}
