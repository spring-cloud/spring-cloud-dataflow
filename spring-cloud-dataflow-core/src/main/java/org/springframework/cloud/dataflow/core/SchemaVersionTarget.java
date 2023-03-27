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

package org.springframework.cloud.dataflow.core;

import java.util.Objects;

/**
 * This represents the combination of BootSchemaVersion and prefixes for the various schemas.
 * @author Corneil du Plessis
 */
public class SchemaVersionTarget {
	private String name;
	private AppBootSchemaVersion schemaVersion;
	private String taskPrefix;
	private String batchPrefix;
	private String datasource;

	public SchemaVersionTarget() {
	}

	public SchemaVersionTarget(String name, AppBootSchemaVersion schemaVersion, String taskPrefix, String batchPrefix, String datasource) {
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
		return schemaVersion == null ? AppBootSchemaVersion.defaultVersion() : schemaVersion;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SchemaVersionTarget that = (SchemaVersionTarget) o;

		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}
	public static SchemaVersionTarget createDefault(AppBootSchemaVersion schemaVersion) {
		return new SchemaVersionTarget(schemaVersion.name().toLowerCase(), schemaVersion, schemaVersion.name() + "_TASK_", schemaVersion.name() + "_BATCH_", null);
	}

	@Override
	public String toString() {
		return "SchemaVersionTarget{" +
				"name='" + name + '\'' +
				", schemaVersion=" + schemaVersion +
				", taskPrefix='" + taskPrefix + '\'' +
				", batchPrefix='" + batchPrefix + '\'' +
				", datasource='" + datasource + '\'' +
				'}';
	}
}
