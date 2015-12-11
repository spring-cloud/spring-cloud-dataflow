/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import org.springframework.cloud.dataflow.rest.resource.LibraryRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available for library registrations.
 * @author Eric Bottard
 */
public interface LibraryOperations {

	/**
	 * Return a list of all library registrations.
	 * @return list of all library registrations
	 */
	PagedResources<LibraryRegistrationResource> list();

	/**
	 * Retrieve information about a library registration.
	 * @param name name of library
	 * @return detailed information about a library registration
	 */
	LibraryRegistrationResource info(String name);

	/**
	 * Register a library name with its Maven coordinates.
	 *
	 * @param name        library name
	 * @param coordinates Maven coordinates for the library BOM
	 * @param force       if {@code true}, overwrites a pre-existing registration
	 */
	ModuleRegistrationResource register(String name, String coordinates, boolean force);

	/**
	 * Unregister a library by name.
	 *
	 * @param name module name
	 */
	void unregister(String name);

}
