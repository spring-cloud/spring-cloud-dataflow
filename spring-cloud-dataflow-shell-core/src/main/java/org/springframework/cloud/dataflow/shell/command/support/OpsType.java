package org.springframework.cloud.dataflow.shell.command.support;

/**
 *
 * @author Gunnar Hillert
 *
 */
public enum OpsType {
	STREAM,
	COUNTER,
	FIELD_VALUE_COUNTER,
	AGGREGATE_COUNTER,
	TASK,
	JOB,
	APP_REGISTRY,
	COMPLETION,
	RUNTIME;
}
