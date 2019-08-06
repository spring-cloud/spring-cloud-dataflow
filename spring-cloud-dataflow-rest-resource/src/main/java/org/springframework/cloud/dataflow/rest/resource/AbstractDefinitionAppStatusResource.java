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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.RepresentationModel;

/**
 * Resource representing the status of an app in a definition.
 *
 * @author Glenn Renfro
 */
public abstract class AbstractDefinitionAppStatusResource<T extends RepresentationModel<? extends T>> extends RepresentationModel<T> {

	private String appName;
	private String  dsl;

	Map<String,String> appStatuses;

	protected AbstractDefinitionAppStatusResource() {
	}

	public AbstractDefinitionAppStatusResource(String appName, String dsl, Map<String, String> appStatuses) {
		this.dsl = dsl;
		this.appName = appName;
		this.appStatuses = new HashMap<>();
		this.appStatuses.putAll(appStatuses);
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getDsl() {
		return dsl;
	}

	public void setDsl(String dsl) {
		this.dsl = dsl;
	}

	public Map<String, String> getAppStatuses() {
		return appStatuses;
	}

	public void setAppStatuses(Map<String, String> appStatuses) {
		this.appStatuses = appStatuses;
	}
}
