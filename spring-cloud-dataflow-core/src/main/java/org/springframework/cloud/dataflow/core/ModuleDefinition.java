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

package org.springframework.cloud.dataflow.core;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Representation of a module, including module properties provided via the DSL
 * definition. This does not include module information required at deployment
 * time (such as the number of module instances).
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 */
public class ModuleDefinition {

	/**
	 * Name of module.
	 */
	private final String name;

	/**
	 * Symbolic name of a module. This may be generated to a default
	 * value or specified in the DSL string.
	 */
	private final String label;

	/**
	 * Name of deployable unit this module instance belongs to (such as a stream).
	 */
	private final String group;

	/**
	 * Parameters for module. This is specific to the type of module - for
	 * instance an http module would include a port number as a parameter.
	 */
	private final Map<String, String> parameters;


	/**
	 * Construct a {@code ModuleDefinition}. This constructor is private; use
	 * {@link ModuleDefinition.Builder} to create a new instance.
	 *
	 * @param name name of module
	 * @param label label used for module in stream definition
	 * @param group group this module belongs to (e.g. stream)
	 * @param parameters module parameters; may be {@code null}
	 */
	private ModuleDefinition(String name, String label, String group, Map<String, String> parameters) {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(label, "label must not be null");
		Assert.notNull(group, "group must not be null");
		this.name = name;
		this.label = label;
		this.group = group;
		this.parameters = parameters == null
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new HashMap<String, String>(parameters));
	}

	/**
	 * Return the name of this module.
	 *
	 * @return module name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return symbolic name of a module. This may be explicitly specified in
	 * the DSL string, which is required if the group contains another module
	 * with the same name. Otherwise, it will be the same as the module name.
	 *
	 * @return module label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Return name of deployable unit this module instance belongs to
	 * (such as a stream).
	 *
	 * @return group name
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Return parameters for module. This is specific to the type of module - for
	 * instance an http module would include a port number as a parameter.
	 *
	 * @return read-only map of module parameters
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModuleDefinition other = (ModuleDefinition) obj;
		if (group == null) {
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("name", this.name)
				.append("label", this.label)
				.append("group", this.group)
				.append("parameters", this.parameters).toString();
	}


	/**
	 * Builder object for {@code ModuleDefinition}.
	 * This object is mutable to allow for flexibility in specifying module
	 * fields/parameters during parsing.
	 */
	public static class Builder {

		/**
		 * @see ModuleDefinition#name
		 */
		private String name;

		/**
		 * @see ModuleDefinition#label
		 */
		private String label;

		/**
		 * @see ModuleDefinition#group
		 */
		private String group;

		/**
		 * @see ModuleDefinition#parameters
		 */
		private final Map<String, String> parameters = new HashMap<String, String>();

		/**
		 * Create a new builder that is initialized with properties of the given definition.
		 * Useful for "mutating" a definition by building a slightly different copy.
		 */
		public static Builder from(ModuleDefinition definition) {
			Builder builder = new Builder();
			builder.setGroup(definition.getGroup())
				.setLabel(definition.getLabel())
				.setName(definition.getName())
				.addParameters(definition.getParameters());
			return builder;
		}


		/**
		 * Set the module name.
		 *
		 * @param name name of module
		 * @return this builder object
		 *
		 * @see ModuleDefinition#name
		 */
		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Set the module label.
		 *
		 * @param label name of module label
		 * @return this builder object
		 *
		 * @see ModuleDefinition#label
		 */
		public Builder setLabel(String label) {
			this.label = label;
			return this;
		}

		/**
		 * Set the module group.
		 *
		 * @param group name of module group
		 * @return this builder object
		 *
		 * @see ModuleDefinition#group
		 */
		public Builder setGroup(String group) {
			this.group = group;
			return this;
		}

		/**
		 * Set a module parameter.
		 *
		 * @param name parameter name
		 * @param value parameter value
		 * @return this builder object
		 *
		 * @see ModuleDefinition#parameters
		 */
		public Builder setParameter(String name, String value) {
			this.parameters.put(name, value);
			return this;
		}

		/**
		 * Add the contents of the provided map to the map of module parameters.
		 *
		 * @param parameters module parameters
		 * @return this builder object
		 *
		 * @see ModuleDefinition#parameters
		 */
		public Builder addParameters(Map<String, String> parameters) {
			this.parameters.putAll(parameters);
			return this;
		}

		/**
		 * Return name of module.
		 *
		 * @return module name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Return symbolic name of a module. This may be explicitly specified in
		 * the DSL string, which is required if the group contains another module
		 * with the same name. Otherwise, it will be the same as the module name.
		 *
		 * @return module label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Return name of deployable unit this module instance belongs to
		 * (such as a stream).
		 *
		 * @return group name
		 */
		public String getGroup() {
			return group;
		}

		/**
		 * Return parameters for module. This is specific to the type of module -
		 * for instance an http module would include a port number as a parameter.
		 * <br />
		 * Note that the contents of this map are <b>mutable</b>.
		 *
		 * @return map of module parameters
		 */
		public Map<String, String> getParameters() {
			return parameters;
		}

		/**
		 * Return a new instance of {@link ModuleDefinition}.
		 *
		 * @return new instance of {@code ModuleDefinition}
		 */
		public ModuleDefinition build() {
			if (this.label == null) {
				this.label = this.name;
			}
			return new ModuleDefinition(this.name, this.label, this.group, this.parameters);
		}
	}

}
