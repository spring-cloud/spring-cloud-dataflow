/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource.about;

/**
 * Provides version information about core libraries used.
 *
 * @author Gunnar Hillert
 */
public class VersionInfo {

	private Dependency implementation = new Dependency();

	private Dependency core = new Dependency();

	private Dependency dashboard = new Dependency();

	/**
	 * Default constructor for serialization frameworks.
	 */
	public VersionInfo() {
	}

	public Dependency getCore() {
		return core;
	}

	public void setCore(Dependency core) {
		this.core = core;
	}

	public Dependency getImplementation() {
		return implementation;
	}

	public void setImplementation(Dependency implementation) {
		this.implementation = implementation;
	}

	public Dependency getDashboard() {
		return dashboard;
	}

	public void setDashboard(Dependency dashboard) {
		this.dashboard = dashboard;
	}
}
