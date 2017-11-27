/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource.about;

/**
 * Provides version information about core libraries used.
 *
 * @author Gunnar Hillert
 * @author Glenn Renfro
 */
public class VersionInfo {

	private Dependency implementationDependency = new Dependency();

	private Dependency coreDependency = new Dependency();

	private Dependency dashboardDependency = new Dependency();

	private Dependency shellDependency = new Dependency();

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
	public Dependency getImplementationDependency() {
		return implementationDependency;
	}

	/**
	 * Establish the {@link Dependency} for the implementation.
	 *
	 * @param implementationDependency the {@link Dependency} instance for the implementation.
	 */
	public void setImplementationDependency(Dependency implementationDependency) {
		this.implementationDependency = implementationDependency;
	}

	/**
	 * Retrieves the current {@link Dependency} for the core Data Flow instance.
	 *
	 * @return {@link Dependency} instance containing core Data Flow information.
	 */
	public Dependency getCoreDependency() {
		return coreDependency;
	}

	/**
	 * Establish the {@link Dependency} for the Data Flow core.
	 *
	 * @param coreDependency the {@link Dependency} instance for the Data Flow core.
	 */
	public void setCoreDependency(Dependency coreDependency) {
		this.coreDependency = coreDependency;
	}

	/**
	 * Retrieves the current {@link Dependency} for the Data Flow UI instance.
	 *
	 * @return {@link Dependency} instance containing Data Flow UI information.
	 */
	public Dependency getDashboardDependency() {
		return dashboardDependency;
	}

	/**
	 * Establish the {@link Dependency} for the dashboard.
	 *
	 * @param dashboardDependency the {@link Dependency} instance for the dashboard.
	 */
	public void setDashboardDependency(Dependency dashboardDependency) {
		this.dashboardDependency = dashboardDependency;
	}

	/**
	 * Retrieves the current {@link Dependency} for the shell instance.
	 *
	 * @return {@link Dependency} instance containing shell information.
	 */
	public Dependency getShellDependency() {
		return shellDependency;
	}

	/**
	 * Establish the {@link Dependency} for the shell.
	 *
	 * @param shellDependency the {@link Dependency} instance for the shell.
	 */
	public void setShellDependency(Dependency shellDependency) {
		this.shellDependency = shellDependency;
	}
}
