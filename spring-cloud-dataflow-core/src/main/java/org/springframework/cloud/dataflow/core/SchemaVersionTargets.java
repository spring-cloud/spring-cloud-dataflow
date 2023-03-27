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

import java.util.List;

/**
 * Will be the response to provide list of schema targets along with the name of the default.
 * @author Corneil du Plessis
 */
public class SchemaVersionTargets {
	private String defaultSchemaTarget;
	private List<SchemaVersionTarget> schemas;

	public SchemaVersionTargets(String defaultSchemaTarget, List<SchemaVersionTarget> schemas) {
		this.defaultSchemaTarget = defaultSchemaTarget;
		this.schemas = schemas;
	}

	public String getDefaultSchemaTarget() {
		return defaultSchemaTarget;
	}

	public void setDefaultSchemaTarget(String defaultSchemaTarget) {
		this.defaultSchemaTarget = defaultSchemaTarget;
	}

	public List<SchemaVersionTarget> getSchemas() {
		return schemas;
	}

	public void setSchemas(List<SchemaVersionTarget> schemas) {
		this.schemas = schemas;
	}

	@Override
	public String toString() {
		return "SchemaVersionTargets{" +
				"defaultSchemaTarget='" + defaultSchemaTarget + '\'' +
				", schemas=" + schemas +
				'}';
	}
}
