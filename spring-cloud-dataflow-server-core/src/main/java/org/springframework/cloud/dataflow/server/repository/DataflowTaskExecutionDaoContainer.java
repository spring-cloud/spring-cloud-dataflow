/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
