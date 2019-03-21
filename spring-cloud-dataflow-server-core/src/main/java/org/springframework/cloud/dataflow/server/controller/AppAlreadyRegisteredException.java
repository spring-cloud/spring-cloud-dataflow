/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller;

import org.springframework.cloud.dataflow.core.AppRegistration;

/**
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Eric Bottard
 * @author Gary Russell
 * @author Patrick Peralta
 */
public class AppAlreadyRegisteredException extends IllegalStateException {

	private static final long serialVersionUID = 1L;

	private final AppRegistration previous;

	public AppAlreadyRegisteredException(AppRegistration previous) {
		this.previous = previous;
	}

	@Override
	public String getMessage() {
		return String.format("The '%s:%s' application is already registered as %s", previous.getType(),
				previous.getName(), previous.getUri());
	}

	public AppRegistration getPrevious() {
		return previous;
	}
}
