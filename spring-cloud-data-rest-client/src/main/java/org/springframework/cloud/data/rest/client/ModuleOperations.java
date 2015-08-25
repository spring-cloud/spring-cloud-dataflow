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

package org.springframework.cloud.data.rest.client;

import org.springframework.cloud.data.core.ModuleType;
import org.springframework.cloud.data.rest.resource.DetailedModuleRegistrationResource;
import org.springframework.cloud.data.rest.resource.ModuleRegistrationResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available for module registrations.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Eric Bottard
 * @author Patrick Peralta
 */
public interface ModuleOperations {

	/**
	 * Return a list of all module registrations.
	 *
	 * @return list of all module registrations
	 */
	PagedResources<ModuleRegistrationResource> list();

	/**
	 * Return a list of all module registrations for the given {@link ModuleType}.
	 *
	 * @param type module type for which to return a list of registrations
	 * @return list of all module registrations for the given module type
	 */
	PagedResources<ModuleRegistrationResource> list(ModuleType type);

	/**
	 * Retrieve information about a module registration.
	 *
	 * @param name name of module
	 * @param type module type
	 *
	 * @return detailed information about a module registration
	 */
	DetailedModuleRegistrationResource info(String name, ModuleType type);

	/**
	 * Register a module name and type with its Maven coordinates.
	 *
	 * @param name  module name
	 * @param type  module type
	 * @param coordinates  Maven coordinates for the module artifact
	 * @param force if {@code true}, overwrites a pre-existing registration
	 */
	ModuleRegistrationResource register(String name, ModuleType type,
			String coordinates, boolean force);

	/**
	 * Unregister a module name and type.
	 *
	 * @param name  module name
	 * @param type  module type
	 */
	void unregister(String name, ModuleType type);

}
