package org.springframework.cloud.dataflow.core;

public class LaunchResponse {
	private long executionId;

	public LaunchResponse() {
	}

	public LaunchResponse(long executionId) {
		this.executionId = executionId;
	}

	public long getExecutionId() {
		return executionId;
	}

	public void setExecutionId(long executionId) {
		this.executionId = executionId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LaunchResponse that = (LaunchResponse) o;

		if (executionId != that.executionId) return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (executionId ^ (executionId >>> 32));
		result = 31 * result;
		return result;
	}

	@Override
	public String toString() {
		return "LaunchResponse{" +
				"taskId=" + executionId +
				'}';
	}
}
