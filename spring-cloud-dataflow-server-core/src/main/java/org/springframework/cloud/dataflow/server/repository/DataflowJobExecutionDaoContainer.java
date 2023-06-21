package org.springframework.cloud.dataflow.server.repository;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

public class DataflowJobExecutionDaoContainer {
	private final Map<String, DataflowJobExecutionDao> jobExecutionDaos = new HashMap<>();

	public DataflowJobExecutionDaoContainer() {
	}

	public void add(String name, DataflowJobExecutionDao jobExecutionDao) {
		jobExecutionDaos.put(name, jobExecutionDao);
	}

	public DataflowJobExecutionDao get(String name) {
		DataflowJobExecutionDao result = jobExecutionDaos.get(name);
		Assert.notNull(result, "Expected to find jobExecutionDao for " + name);
		return result;
	}
}
