package org.springframework.cloud.dataflow.server.repository;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.util.Assert;

public class DataflowTaskExecutionMetadataDaoContainer {
	private final Map<String, DataflowTaskExecutionMetadataDao> dataflowTaskExecutionMetadataDaos = new HashMap<>();

	public DataflowTaskExecutionMetadataDaoContainer() {
	}

	public void add(String schemaTarget, DataflowTaskExecutionMetadataDao dao) {
		dataflowTaskExecutionMetadataDaos.put(schemaTarget, dao);
	}

	public DataflowTaskExecutionMetadataDao get(String schemaTarget) {
		if(schemaTarget == null) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		DataflowTaskExecutionMetadataDao result = dataflowTaskExecutionMetadataDaos.get(schemaTarget);
		Assert.notNull(result, "Expected DataflowTaskExecutionMetadataDao for " + schemaTarget);
		return result;
	}
}
