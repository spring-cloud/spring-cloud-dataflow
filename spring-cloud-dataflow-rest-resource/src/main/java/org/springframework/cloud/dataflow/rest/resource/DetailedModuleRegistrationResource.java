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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.hateoas.PagedResources;

/**
 * Extension of {@link ModuleRegistrationResource} that contains module options
 * and other detailed module information.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Patrick Peralta
 */
public class DetailedModuleRegistrationResource extends ModuleRegistrationResource {

	/**
	 * Optional short description of the module.
	 */
	private String shortDescription;

	/**
	 * List of module options.
	 */
	private final List<ConfigurationMetadataProperty> options = new ArrayList<>();


	/**
	 * Default constructor for serialization frameworks.
	 */
	protected DetailedModuleRegistrationResource() {
	}

	/**
	 * Construct a {@code DetailedModuleRegistrationResource} object.
	 *
	 * @param name module name
	 * @param type module type
	 * @param coordinates Maven coordinates for module artifact
	 */
	public DetailedModuleRegistrationResource(String name, String type, String coordinates) {
		super(name, type, coordinates);
	}

	/**
	 * Construct a {@code DetailedModuleRegistrationResource} object based
	 * on the provided {@link ModuleRegistrationResource}.
	 *
	 * @param resource {@code ModuleRegistrationResource} from which to obtain
	 *                 module registration data
	 */
	public DetailedModuleRegistrationResource(ModuleRegistrationResource resource) {
		super(resource.getName(), resource.getType(), resource.getCoordinates());
	}

	/**
	 * Add a module option.
	 *
	 * @param option module option to add
	 */
	public void addOption(ConfigurationMetadataProperty option) {
		options.add(option);
	}

	/**
	 * Return a list of module options.
	 *
	 * @return list of module options
	 */
	public List<ConfigurationMetadataProperty> getOptions() {
		return options;
	}

	/**
	 * Set a description for this module.
	 *
	 * @param shortDescription description for module
	 */
	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * Return a description for this module.
	 *
	 * @return description for this module
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 */
	public static class Page extends PagedResources<DetailedModuleRegistrationResource> {
	}

}
