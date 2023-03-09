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

import java.util.List;

import org.springframework.hateoas.RepresentationModel;

/**
 * Resource for {@link org.springframework.cloud.dataflow.core.SchemaVersionTargets}
 * @author Corneil du Plessis
 */
public class SchemaVersionTargetsResource extends RepresentationModel<SchemaVersionTargetsResource> {
	private String defaultSchemaTarget;

	private List<SchemaVersionTargetResource> schemas;

	public SchemaVersionTargetsResource() {
	}

	public SchemaVersionTargetsResource(String defaultSchemaTarget, List<SchemaVersionTargetResource> schemas) {
		this.defaultSchemaTarget = defaultSchemaTarget;
		this.schemas = schemas;
	}

	public String getDefaultSchemaTarget() {
		return defaultSchemaTarget;
	}

	public void setDefaultSchemaTarget(String defaultSchemaTarget) {
		this.defaultSchemaTarget = defaultSchemaTarget;
	}

	public List<SchemaVersionTargetResource> getSchemas() {
		return schemas;
	}

	public void setSchemas(List<SchemaVersionTargetResource> schemas) {
		this.schemas = schemas;
	}
}
