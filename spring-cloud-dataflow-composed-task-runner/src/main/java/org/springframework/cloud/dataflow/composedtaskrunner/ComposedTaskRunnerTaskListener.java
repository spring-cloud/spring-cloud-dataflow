package org.springframework.cloud.dataflow.composedtaskrunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.task.listener.TaskExecutionListener;
import org.springframework.cloud.task.repository.TaskExecution;

public class ComposedTaskRunnerTaskListener implements TaskExecutionListener {
	private final static Logger logger = LoggerFactory.getLogger(ComposedTaskRunnerTaskListener.class);

	private static Long executionId = null;

	@Override
	public void onTaskStartup(TaskExecution taskExecution) {
		executionId = taskExecution.getExecutionId();
		logger.info("onTaskStartup:executionId={}", executionId);
	}

	public static Long getExecutionId() {
		return executionId;
	}
}
