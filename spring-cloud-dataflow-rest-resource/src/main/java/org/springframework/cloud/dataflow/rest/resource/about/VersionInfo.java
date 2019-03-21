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
 * @author Glenn Renfro
 */
public class VersionInfo {

	private Dependency implementation = new Dependency();

	private Dependency core = new Dependency();

	private Dependency dashboard = new Dependency();

	private Dependency shell = new Dependency();

	/**
	 * Default constructor for serialization frameworks.
	 */
	public VersionInfo() {
	}

	/**
	 * Retrieves the current {@link Dependency} for the implementation.
	 *
	 * @return {@link Dependency} instance containing Implementation information.
	 */
	public Dependency getImplementation() {
		return implementation;
	}

	/**
	 * Establish the {@link Dependency} for the implementation.
	 *
	 * @param implementation the {@link Dependency} instance for the implementation.
	 */
	public void setImplementation(Dependency implementation) {
		this.implementation = implementation;
	}

	/**
	 * Retrieves the current {@link Dependency} for the core Data Flow instance.
	 *
	 * @return {@link Dependency} instance containing core Data Flow information.
	 */
	public Dependency getCore() {
		return core;
	}

	/**
	 * Establish the {@link Dependency} for the Data Flow core.
	 *
	 * @param core the {@link Dependency} instance for the Data Flow core.
	 */
	public void setCore(Dependency core) {
		this.core = core;
	}

	/**
	 * Retrieves the current {@link Dependency} for the Data Flow UI instance.
	 *
	 * @return {@link Dependency} instance containing Data Flow UI information.
	 */
	public Dependency getDashboard() {
		return dashboard;
	}

	/**
	 * Establish the {@link Dependency} for the dashboard.
	 *
	 * @param dashboard the {@link Dependency} instance for the dashboard.
	 */
	public void setDashboard(Dependency dashboard) {
		this.dashboard = dashboard;
	}

	/**
	 * Retrieves the current {@link Dependency} for the shell instance.
	 *
	 * @return {@link Dependency} instance containing shell information.
	 */
	public Dependency getShell() {
		return shell;
	}

	/**
	 * Establish the {@link Dependency} for the shell.
	 *
	 * @param shell the {@link Dependency} instance for the shell.
	 */
	public void setShell(Dependency shell) {
		this.shell = shell;
	}
}
