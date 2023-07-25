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

package org.springframework.cloud.dataflow.schema;

import java.util.List;

/**
 * Will provide response to list all schema versions supported along with the default.
 * @author Corneil du Plessis
 */
public class AppBootSchemaVersions {
	private AppBootSchemaVersion defaultSchemaVersion;
	private List<AppBootSchemaVersion> versions;

	public AppBootSchemaVersions() {
	}

	public AppBootSchemaVersions(AppBootSchemaVersion defaultSchemaVersion, List<AppBootSchemaVersion> versions) {
		this.defaultSchemaVersion = defaultSchemaVersion;
		this.versions = versions;
	}

	public AppBootSchemaVersion getDefaultSchemaVersion() {
		return defaultSchemaVersion;
	}

	public void setDefaultSchemaVersion(AppBootSchemaVersion defaultSchemaVersion) {
		this.defaultSchemaVersion = defaultSchemaVersion;
	}

	public List<AppBootSchemaVersion> getVersions() {
		return versions;
	}

	public void setVersions(List<AppBootSchemaVersion> versions) {
		this.versions = versions;
	}

	@Override
	public String toString() {
		return "AppBootSchemaVersions{" +
				"defaultSchemaVersion=" + defaultSchemaVersion +
				", versions=" + versions +
				'}';
	}
}
