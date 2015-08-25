/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.rest.resource;

import java.util.ArrayList;
import java.util.List;

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
	private final List<Option> options = new ArrayList<Option>();


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
	 * @param composed if true, this is a composed module
	 */
	public DetailedModuleRegistrationResource(String name, String type, String coordinates, boolean composed) {
		super(name, type, coordinates, composed);
	}

	/**
	 * Construct a {@code DetailedModuleRegistrationResource} object based
	 * on the provided {@link ModuleRegistrationResource}.
	 *
	 * @param resource {@code ModuleRegistrationResource} from which to obtain
	 *                 module registration data
	 */
	public DetailedModuleRegistrationResource(ModuleRegistrationResource resource) {
		super(resource.getName(), resource.getType(), resource.getCoordinates(), resource.isComposed());
	}

	/**
	 * Add a module option.
	 *
	 * @param option module option to add
	 */
	public void addOption(Option option) {
		options.add(option);
	}

	/**
	 * Return a list of module options.
	 *
	 * @return list of module options
	 */
	public List<Option> getOptions() {
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
	 * Class for a module option.
	 */
	public static class Option {

		/**
		 * Option name.
		 */
		private String name;

		/**
		 * Option type.
		 */
		private String type;

		/**
		 * Option description.
		 */
		private String description;

		/**
		 * Default value for option.
		 */
		private String defaultValue;

		/**
		 * If true, this is a hidden option.
		 */
		private boolean hidden;

		/**
		 * Default constructor for serialization frameworks.
		 */
		protected Option() {
		}

		/**
		 * Construct a module {@code Option} object.
		 *
		 * @param name option name
		 * @param type option type
		 * @param description option description
		 * @param defaultValue default value for option
		 * @param hidden if true, this option is hidden
		 */
		public Option(String name, String type, String description, String defaultValue, boolean hidden) {
			this.name = name;
			this.type = type;
			this.description = description;
			this.defaultValue = defaultValue;
			this.hidden = hidden;
		}

		public String getName() {
			return name;
		}


		public String getType() {
			return type;
		}


		public String getDescription() {
			return description;
		}


		public String getDefaultValue() {
			return defaultValue;
		}

		public boolean isHidden() {
			return hidden;
		}

		@Override
		public String toString() {
			return "Option [name=" + name + ", type=" + type + ", defaultValue=" + defaultValue + ", hidden=" + hidden
					+ "]";
		}
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 */
	public static class Page extends PagedResources<DetailedModuleRegistrationResource> {
	}

}
