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

package org.springframework.cloud.data.core;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Representation of a module in the context of a defined stream.
 * This includes module options provided in a stream definition.
 * This does not include module information required at deployment
 * time (such as the number of module instances).
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 */
public class ModuleDefinition implements Comparable<ModuleDefinition> {

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
	 * Position in stream definition relative to the other modules
	 * in the definition. 0 indicates the first/leftmost position.
	 */
	private final int index;

	/**
	 * Map of channel bindings for this module (such as input and/or output channels).
	 */
	private final Map<String, String> bindings;

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
	 * @param group group this module belongs to (stream)
	 * @param index position of module in stream definition
	 * @param bindings map of channel bindings; may not be {@code null}
	 * @param parameters module parameters; may be {@code null}
	 */
	private ModuleDefinition(String name, String label, String group,
			int index, Map<String, String> bindings, Map<String, String> parameters) {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(label, "label must not be null");
		Assert.notNull(group, "group must not be null");
		Assert.notNull(bindings, "bindings must not be null");
		Assert.notEmpty(bindings, "bindings must not be empty");
		this.name = name;
		this.label = label;
		this.group = group;
		this.index = index;
		this.bindings = Collections.unmodifiableMap(new HashMap<String, String>(bindings));
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
	 * the DSL string, which is required if the stream contains another module
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
	 * Return position in stream definition relative to the other
	 * modules in the definition. 0 indicates the first/leftmost position.
	 *
	 * @return module index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Return map of channel bindings for this module (such as input
	 * and/or output channels).
	 *
	 * @return map of channel bindings for module
	 */
	public Map<String, String> getBindings() {
		return this.bindings;
	}

	/**
	 * Return parameters for module. This is specific to the type of module - for
	 * instance an http module would include a port number as a parameter.
	 *
	 * @return read-only map of module parameters
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("name", this.name)
				.append("label", this.label)
				.append("group", this.group)
				.append("bindings", this.bindings)
				.append("index", index)
				.append("parameters", parameters).toString();
	}

	@Override
	public int compareTo(ModuleDefinition o) {
		Assert.notNull(o, "ModuleDefinition must not be null");
		return (index < o.index) ? -1 : ((index == o.index) ? 0 : 1);
	}


	/**
	 * Builder object for {@code ModuleDefinition}.
	 * This object is mutable to allow for flexibility in specifying module
	 * type/fields/parameters during parsing.
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
		 * @see ModuleDefinition#index
		 */
		private int index;

		/**
		 * @see ModuleDefinition#bindings
		 */
		private Map<String, String> bindings = new HashMap<String, String>();

		/**
		 * @see ModuleDefinition#parameters
		 */
		private final Map<String, String> parameters = new HashMap<String, String>();


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
		 * Set the module index.
		 *
		 * @param index position of module in stream definition
		 * @return this builder object
		 *
		 * @see ModuleDefinition#index
		 */
		public Builder setIndex(int index) {
			this.index = index;
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
		 * Set a module binding.
		 *
		 * @param name binding name
		 * @param value binding value
		 * @return this builder object
		 *
		 * @see ModuleDefinition#bindings
		 */
		public Builder setBinding(String name, String value) {
			this.bindings.put(name, value);
			return this;
		}

		/**
		 * Add the contents of the provided map to the map of module bindings.
		 *
		 * @param bindings module bindings
		 * @return this builder object
		 *
		 * @see ModuleDefinition#bindings
		 */
		public Builder addBindings(Map<String, String> bindings) {
			this.bindings.putAll(bindings);
			return this;
		}

		/**
		 * Return name of module. Typically this module is present under
		 * {@code $XD_HOME/modules/[module type]}.
		 *
		 * @return module name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Return symbolic name of a module. This may be explicitly specified in
		 * the DSL string, which is required if the stream contains another module
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
		 * Return position in stream definition relative to the other modules in
		 * the definition. 0 indicates the first/leftmost position.
		 *
		 * @return module index
		 */
		public int getIndex() {
			return index;
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
		 * Return bindings for module. Note that the contents of this map are <b>mutable</b>.
		 *
		 * @return map of module bindings
		 */
		public Map<String, String> getBindings() {
			return bindings;
		}

		/**
		 * Return a new instance of {@link ModuleDefinition}.
		 *
		 * @return new instance of {@code ModuleDefinition}
		 */
		public ModuleDefinition build() {
			return new ModuleDefinition(name, label, group,
					index, bindings, parameters);
		}
	}

}
