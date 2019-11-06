/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.core;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Michael Minella
 */
@Component
public class TaskExecutionManifestConverter implements AttributeConverter<TaskExecutionManifest.Manifest, String> {

	private static ObjectMapper objectMapper;

	@Autowired
	public void setObjectMapper(ObjectMapper objectMapper){
		TaskExecutionManifestConverter.objectMapper = objectMapper;
		System.out.println(">> Object Mapper is set: " + objectMapper);
	}

	@Override
	public String convertToDatabaseColumn(TaskExecutionManifest.Manifest taskManifest) {
		try {
			return this.objectMapper.writeValueAsString(taskManifest);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unable to serialize manifest to JSON", e);
		}
	}

	@Override
	public TaskExecutionManifest.Manifest convertToEntityAttribute(String s) {
		try {
			return this.objectMapper.readValue(s, TaskExecutionManifest.Manifest.class);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unable to deserialize JSON into a manifest", e);
		}
	}
}
