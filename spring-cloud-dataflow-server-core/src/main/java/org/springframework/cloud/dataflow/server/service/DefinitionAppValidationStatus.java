/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * Contains the validation status of a node within a stream definition.
 *
 * @author Glenn Renfro
 */
public class DefinitionAppValidationStatus {

	private String definitionName;

	private String definitionDSL;

	private Map<String, String> appsStatuses;

	public DefinitionAppValidationStatus(String definitionName, String definitionDSL) {
		this.definitionName = definitionName;
		this.definitionDSL = definitionDSL;
		this.appsStatuses = new HashMap<>();
	}

	public String getDefinitionName() {
		return definitionName;
	}

	public void setDefinitionName(String definitionName) {
		this.definitionName = definitionName;
	}

	public String getDefinitionDSL() {
		return definitionDSL;
	}

	public void setDefinitionDSL(String definitionDSL) {
		this.definitionDSL = definitionDSL;
	}

	public Map<String, String> getAppsStatuses() {
		return appsStatuses;
	}

	public void setAppsStatuses(Map<String, String> appsStatuses) {
		this.appsStatuses = appsStatuses;
	}
}
