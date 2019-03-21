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
package org.springframework.cloud.skipper.domain;

/**
 * Provides version information about core libraries used.
 *
 * @author Janne Valkealahti
 *
 */
public class VersionInfo {

	private Dependency server = new Dependency();

	private Dependency shell = new Dependency();

	/**
	 * Default constructor for serialization frameworks.
	 */
	public VersionInfo() {
	}

	/**
	 * Retrieves the current {@link Dependency} for the Skipper Server instance.
	 *
	 * @return {@link Dependency} instance containing Skipper Server information.
	 */
	public Dependency getServer() {
		return server;
	}

	/**
	 * Establish the {@link Dependency} for the Skipper server.
	 *
	 * @param server the {@link Dependency} instance for the Skipper server.
	 */
	public void setServer(Dependency server) {
		this.server = server;
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
