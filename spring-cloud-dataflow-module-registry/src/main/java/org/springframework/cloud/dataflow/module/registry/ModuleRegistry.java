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

package org.springframework.cloud.dataflow.module.registry;


import java.util.List;

import org.springframework.cloud.dataflow.core.ModuleType;

/**
 * {@code ModuleRegistry} is used to manage module registrations. This
 * includes operations such as find, register, and delete.
 *
 * @see ModuleRegistration
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public interface ModuleRegistry {

	/**
	 * Look up the registration for a module by name and type. If
	 * a registration does not exist, {@code null} is returned.
	 *
	 * @param name the module name
	 * @param type the module type
	 *
	 * @return registration for a module, or {@code null} if not found
	 */
	ModuleRegistration find(String name, ModuleType type);

	/**
	 * Return all module registrations.
	 *
	 * @return all module registrations
	 */
	List<ModuleRegistration> findAll();

	/**
	 * Save a new module registration. Pre-existing registrations
	 * with a given name and type will be overwritten.
	 *
	 * @param registration module registration to save
	 */
	void save(ModuleRegistration registration);

	/**
	 * Unregister a module by name and type.
	 *
	 * @param name the module name
	 * @param type the module type
	 */
	void delete(String name, ModuleType type);

}
