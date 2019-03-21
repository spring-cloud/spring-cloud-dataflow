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

package org.springframework.cloud.dataflow.server.repository;

/**
 * Thrown when a stream deployment is attempted on incompatible
 * {@link org.springframework.cloud.dataflow.server.stream.StreamDeployers} deployer
 *
 * @author Christian Tzolov
 */
public class IncompatibleStreamDeployerException extends RuntimeException {

	private final String name;

	public IncompatibleStreamDeployerException(String name) {
		this(name, "Can perform this stream operation only on deployer: " + name);
	}

	public IncompatibleStreamDeployerException(String name, String message) {
		super(message);
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
