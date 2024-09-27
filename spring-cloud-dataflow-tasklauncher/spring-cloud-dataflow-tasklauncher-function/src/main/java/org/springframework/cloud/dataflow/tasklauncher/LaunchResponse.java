package org.springframework.cloud.dataflow.tasklauncher;

import java.util.Objects;

public class LaunchResponse {
	private Long taskId;
	public LaunchResponse() {
	}

	public LaunchResponse(Long taskId) {
		this.taskId = taskId;
	}

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LaunchResponse that = (LaunchResponse) o;

		if (!Objects.equals(taskId, that.taskId)) return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = taskId != null ? taskId.hashCode() : 0;
		result = 31 * result;
		return result;
	}

	@Override
	public String toString() {
		return "LaunchResponse{" +
				"taskId=" + taskId +
				'}';
	}
}
