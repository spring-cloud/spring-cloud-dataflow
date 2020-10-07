package org.springframework.cloud.dataflow.server.repository;

/**
 * This exception is used in
 * {@link org.springframework.cloud.dataflow.server.controller.TaskDefinitionController}
 * when requesting a combination of params which is not acceptable.
 *
 * @author siddhant sorann
 */
public class TaskQueryParamException extends RuntimeException {

	public TaskQueryParamException(String[] params) {
		super(String.format("The following params cannot be used together in the API request: %s",
				String.join(", ", params)));
	}
}
