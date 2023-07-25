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
import org.springframework.util.StringUtils;

/**
 * Provide container of DataflowTaskExecutionMetadataDao for each schema target;
 * @author Corneil du Plessis
 */
public class DataflowTaskExecutionMetadataDaoContainer {
	private final Map<String, DataflowTaskExecutionMetadataDao> dataflowTaskExecutionMetadataDaos = new HashMap<>();

	public DataflowTaskExecutionMetadataDaoContainer() {
	}

	public void add(String schemaTarget, DataflowTaskExecutionMetadataDao dao) {
		dataflowTaskExecutionMetadataDaos.put(schemaTarget, dao);
	}

	public DataflowTaskExecutionMetadataDao get(String schemaTarget) {
		if(!StringUtils.hasText(schemaTarget)) {
			schemaTarget = SchemaVersionTarget.defaultTarget().getName();
		}
		DataflowTaskExecutionMetadataDao result = dataflowTaskExecutionMetadataDaos.get(schemaTarget);
		Assert.notNull(result, "Expected DataflowTaskExecutionMetadataDao for " + schemaTarget);
		return result;
	}
}
