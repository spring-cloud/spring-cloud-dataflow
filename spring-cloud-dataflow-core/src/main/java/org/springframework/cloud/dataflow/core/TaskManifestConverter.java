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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * @author Michael Minella
 */
public class TaskManifestConverter implements AttributeConverter<TaskExecutionManifest.Manifest, String> {

	private ObjectMapper objectMapper;

	public TaskManifestConverter() {
		this.objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Resource.class,
				new ResourceDeserializer(new AppResourceCommon(new MavenProperties(), new DefaultResourceLoader())));
		this.objectMapper.registerModule(module);
		this.objectMapper.addMixIn(Resource.class, ResourceMixin.class);
		this.objectMapper.addMixIn(AppDefinition.class, AppDefinitionMixin.class);
		this.objectMapper.addMixIn(AppDeploymentRequest.class, AppDeploymentRequestMixin.class);
		this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
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
