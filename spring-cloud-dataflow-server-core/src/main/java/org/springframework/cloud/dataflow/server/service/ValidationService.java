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

import org.springframework.cloud.dataflow.core.ApplicationType;

/**
 * Perform validation on the provided application name and type.
 *
 * Implementations can test if maven/docker coordinates are valid.
 *
 * @author Mark Pollack
 */
public interface ValidationService {

	/**
	 * Checks if application with its name and type is registered.
	 *
	 * @param name the name
	 * @param applicationType the application type
	 * @return true, if is registered
	 */
	boolean isRegistered(String name, ApplicationType applicationType);

	/**
	 * Validate application with given name and type.
	 *
	 * @param name the name
	 * @param applicationType the application type
	 * @return true, if successful
	 */
	boolean validate(String name, ApplicationType applicationType);
}
