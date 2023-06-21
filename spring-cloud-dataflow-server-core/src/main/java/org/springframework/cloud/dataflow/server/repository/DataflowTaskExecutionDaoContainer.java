package org.springframework.cloud.dataflow.server.repository;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.util.Assert;

public class DataflowTaskExecutionDaoContainer {
	private final Map<String, DataflowTaskExecutionDao> taskExecutionContainer = new HashMap<>();

	public DataflowTaskExecutionDaoContainer() {
	}

	public void add(String schemaTarget, DataflowTaskExecutionDao dataflowTaskExecutionDao) {
		taskExecutionContainer.put(schemaTarget, dataflowTaskExecutionDao);
	}

	public DataflowTaskExecutionDao get(String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		DataflowTaskExecutionDao result = taskExecutionContainer.get(schemaTarget);
		Assert.notNull(result, "Expected DataflowTaskExecutionDao for " + schemaTarget);
		return result;
	}
}
