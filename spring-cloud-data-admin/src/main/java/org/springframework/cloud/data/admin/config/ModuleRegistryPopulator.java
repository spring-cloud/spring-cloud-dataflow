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

package org.springframework.cloud.data.admin.config;

import javax.annotation.PostConstruct;

import org.springframework.cloud.data.core.ModuleCoordinates;
import org.springframework.cloud.data.module.registry.ModuleRegistry;
import org.springframework.util.Assert;

/**
 * Populates a {@link ModuleRegistry} with default modules.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class ModuleRegistryPopulator {

	/**
	 * Group ID for default modules.
	 */
	private static final String DEFAULT_GROUP_ID = "org.springframework.cloud.stream.module";

	/**
	 * Version number for default modules.
	 */
	private static final String DEFAULT_VERSION = "1.0.0.BUILD-SNAPSHOT";

	/**
	 * The {@link ModuleRegistry} to populate.
	 */
	private final ModuleRegistry moduleRegistry;

	/**
	 * Construct a {@code ModuleRegistryPopulator} with the provided {@link ModuleRegistry}.
	 *
	 * @param moduleRegistry the {@link ModuleRegistry} to populate.
	 */
	public ModuleRegistryPopulator(ModuleRegistry moduleRegistry) {
		Assert.notNull(moduleRegistry, "ModuleRegistry must not be null");
		this.moduleRegistry = moduleRegistry;
	}

	/**
	 * Populate the registry with default module coordinates;
	 * will not overwrite existing values.
	 */
	@PostConstruct
	public void populateDefaults() {
		populateDefault("http", "source");
		populateDefault("time", "source");
		populateDefault("filter", "processor");
		populateDefault("groovy-filter", "processor");
		populateDefault("groovy-transform", "processor");
		populateDefault("transform", "processor");
		populateDefault("counter", "sink");
		populateDefault("log", "sink");
	}

	/**
	 * Populate the registry with default values for the provided
	 * module name and type; will not overwrite existing values.
	 *
	 * @param name module name
	 * @param type module type
	 */
	private void populateDefault(String name, String type) {
		if (this.moduleRegistry.findByNameAndType(name, type) == null) {
			this.moduleRegistry.save(name, type, defaultCoordinatesFor(name + '-' + type));
		}
	}

	/**
	 * Return the default coordinates for the provided module name.
	 *
	 * @param moduleName module name for which to provide default coordinates
	 * @return default coordinates for the provided module
	 */
	private ModuleCoordinates defaultCoordinatesFor(String moduleName) {
		return ModuleCoordinates.parse(String.format("%s:%s:%s",
				DEFAULT_GROUP_ID, moduleName, DEFAULT_VERSION));
	}

}
