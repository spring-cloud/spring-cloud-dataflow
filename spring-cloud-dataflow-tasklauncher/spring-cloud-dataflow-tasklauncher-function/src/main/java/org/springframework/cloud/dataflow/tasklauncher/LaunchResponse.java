package org.springframework.cloud.dataflow.tasklauncher;

import java.util.Objects;

public class LaunchResponse {
	private Long taskId;
	private String schemaTarget;

	public LaunchResponse() {
	}

	public LaunchResponse(Long taskId, String schemaTarget) {
		this.taskId = taskId;
		this.schemaTarget = schemaTarget;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
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

		if (!Objects.equals(taskId, that.taskId)) return false;
		return Objects.equals(schemaTarget, that.schemaTarget);
	}

	@Override
	public int hashCode() {
		int result = taskId != null ? taskId.hashCode() : 0;
		result = 31 * result + (schemaTarget != null ? schemaTarget.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "LaunchResponse{" +
				"taskId=" + taskId +
				", schemaTarget='" + schemaTarget + '\'' +
				'}';
	}
}
