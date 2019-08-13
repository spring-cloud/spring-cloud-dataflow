/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the validation status of task, composed task, or stream definition.
 *
 * @author Glenn Renfro
 */
public class ValidationStatus {

	private String definitionName;

	private String definitionDsl;

	private String description;

	private Map<String, String> appsStatuses;

	public ValidationStatus(String definitionName, String definitionDsl, String description) {
		this.definitionName = definitionName;
		this.definitionDsl = definitionDsl;
		this.description = description;
		this.appsStatuses = new HashMap<>();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDefinitionName() {
		return definitionName;
	}

	public void setDefinitionName(String definitionName) {
		this.definitionName = definitionName;
	}

	public String getDefinitionDsl() {
		return definitionDsl;
	}

	public void setDefinitionDsl(String definitionDsl) {
		this.definitionDsl = definitionDsl;
	}

	public Map<String, String> getAppsStatuses() {
		return appsStatuses;
	}

	public void setAppsStatuses(Map<String, String> appsStatuses) {
		this.appsStatuses = appsStatuses;
	}
}
