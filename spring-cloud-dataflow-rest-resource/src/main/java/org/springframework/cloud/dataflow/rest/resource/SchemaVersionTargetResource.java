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

package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.cloud.dataflow.core.AppBootSchemaVersion;
import org.springframework.hateoas.RepresentationModel;

/**
 * Resource for {@link org.springframework.cloud.dataflow.core.SchemaVersionTarget}
 * @author Corneil du Plessis
 */
public class SchemaVersionTargetResource extends RepresentationModel<SchemaVersionTargetResource> {
	private String name;
	private AppBootSchemaVersion schemaVersion;
	private String taskPrefix;
	private String batchPrefix;
	private String datasource;

	public SchemaVersionTargetResource() {
	}

	public SchemaVersionTargetResource(String name, AppBootSchemaVersion schemaVersion, String taskPrefix, String batchPrefix, String datasource) {
		this.name = name;
		this.schemaVersion = schemaVersion;
		this.taskPrefix = taskPrefix;
		this.batchPrefix = batchPrefix;
		this.datasource = datasource;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AppBootSchemaVersion getSchemaVersion() {
		return schemaVersion;
	}

	public void setSchemaVersion(AppBootSchemaVersion schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	public String getTaskPrefix() {
		return taskPrefix;
	}

	public void setTaskPrefix(String taskPrefix) {
		this.taskPrefix = taskPrefix;
	}

	public String getBatchPrefix() {
		return batchPrefix;
	}

	public void setBatchPrefix(String batchPrefix) {
		this.batchPrefix = batchPrefix;
	}

	public String getDatasource() {
		return datasource;
	}

	public void setDatasource(String datasource) {
		this.datasource = datasource;
	}
}
