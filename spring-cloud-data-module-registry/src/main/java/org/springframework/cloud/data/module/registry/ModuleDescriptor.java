/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.data.module.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Representation of a module in the context of a defined stream or job.
 * @author Mark Fisher
 * @author Gary Russell
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 * @author David Turanski 
 */
//TODO: Compiles but needs work. Placeholder for now
public class ModuleDescriptor implements Comparable<ModuleDescriptor> {

	/**
	 * Symbolic name of a module. This may be generated to a default
	 * value or specified in the DSL string.
	 */
	private final String moduleLabel;

	/**
	 * Name of deployable unit this module instance belongs to (such as a stream or job).
	 */
	private final String group;

	/**
	 * Name of source channel, if defined by the stream/job definition.
	 * May be {@code null}.
	 */
	private final String sourceChannelName;

	/**
	 * Name of sink channel, if defined by the stream/job definition.
	 * May be {@code null}.
	 */
	private final String sinkChannelName;

	/**
	 * Position in stream/job definition relative to the other modules
	 * in the definition. 0 indicates the first/leftmost position.
	 */
	private final int index;

	/**
	 * Parameters for module. This is specific to the type of module - for
	 * instance an http module would include a port number as a parameter.
	 */
	private final Map<String, String> parameters;

	/**
	 * If this is a composite module, this list contains the modules that
	 * this module consists of; otherwise this list is empty.
	 */
	private final List<ModuleDescriptor> children;

	/**
	 * Provides the name and type of the module. Typically this module is present under
	 * {@code $XD_HOME/modules/[module_type]/[module_name]}.
	 * Also contains information required for module deployment (i.e. classpath).
	 */
	private final ModuleDefinition moduleDefinition;


	/**
	 * Construct a {@code ModuleDescriptor}. This constructor is private; use
	 * {@link ModuleDescriptor.Builder} to create a new instance.
	 * @param moduleLabel label used for module in stream/job definition
	 * @param group group this module belongs to (stream/job)
	 * @param sourceChannelName name of source channel; may be {@code null}
	 * @param sinkChannelName name of sink channel; may be {@code null}
	 * @param index position of module in stream/job definition
	 * @param moduleDefinition module definition
	 * @param parameters module parameters; may be {@code null}
	 * @param children if this is a composite module, this list contains the modules
	 * that comprise this module; may be {@code null}
	 */
	private ModuleDescriptor(String moduleLabel, String group,
			String sourceChannelName, String sinkChannelName,
			int index, ModuleDefinition moduleDefinition,
			Map<String, String> parameters, List<ModuleDescriptor> children) {
		Assert.notNull(moduleLabel, "moduleLabel must not be null");
		Assert.notNull(group, "group must not be null");
		this.moduleLabel = moduleLabel;
		this.group = group;
		this.sourceChannelName = sourceChannelName;
		this.sinkChannelName = sinkChannelName;
		this.index = index;
		this.moduleDefinition = moduleDefinition;
		this.parameters = parameters == null
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new HashMap<String, String>(parameters));
		this.children = children == null
				? Collections.<ModuleDescriptor>emptyList()
				: Collections.unmodifiableList(new ArrayList<ModuleDescriptor>(children));
	}

	/**
	 * Return name of module. Typically this module is present under
	 * {@code $XD_HOME/modules/[module type]}.
	 * @return module name
	 */
	public String getArtifactId() {
		return moduleDefinition.getArtifactId();
	}

	/**
	 * Return symbolic name of a module. This may be explicitly specified in
	 * the DSL string, which is required if the stream contains another module
	 * with the same name. Otherwise, it will be the same as the module name.
	 * @return module label
	 */
	public String getModuleLabel() {
		return moduleLabel;
	}

	/**
	 * Return name of deployable unit this module instance belongs to
	 * (such as a stream or job).
	 * @return group name
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Return position in stream/job definition relative to the other
	 * modules in the definition. 0 indicates the first/leftmost position.
	 * @return module index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Return the module type.
	 * @return module type
	 */
	public String getGroupId() {
		return moduleDefinition.getGroupId();
	}

	/**
	 * Return name of source channel, if defined by the stream/job definition.
	 * May be {@code null}.
	 * @return source channel name, or {@code null} if no source channel defined
	 */
	public String getSourceChannelName() {
		return sourceChannelName;
	}

	/**
	 * Return name of sink channel, if defined by the stream/job definition.
	 * May be {@code null}.
	 * @return sink channel name, or {@code null} if no sink channel defined
	 */
	public String getSinkChannelName() {
		return sinkChannelName;
	}

	/**
	 * Return parameters for module. This is specific to the type of module - for
	 * instance an http module would include a port number as a parameter.
	 * @return read-only map of module parameters
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}

	/**
	 * If this is a composite module, this list contains the modules that this
	 * module consists of; otherwise this list is empty.
	 * @return sub modules for this module, or empty list if this is not a composite module
	 */
	public List<ModuleDescriptor> getChildren() {
		return children;
	}

	/**
	 * Return the {@link ModuleDefinition} for this module. {@code ModuleDefinition}
	 * contains information required to load the module (i.e. classpath).
	 * @return module definition for this module descriptor
	 */
	public ModuleDefinition getModuleDefinition() {
		return moduleDefinition;
	}

	/**
	 * Return true if this is a composed module. A composed module consists
	 * of one or more child modules.
	 * @return true if this is a composed modules
	 * @see #children
	 */
	public boolean isComposed() {
		return !children.isEmpty();
	}

	/**
	 * Create a new {@link Key} for this {@code ModuleDescriptor}.
	 * @return new {@code Key}
	 */
	public Key createKey() {
		return new Key(getGroup(), getGroupId(), getModuleLabel());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("moduleName", moduleDefinition != null ? moduleDefinition.getArtifactId() : null)
				.append("moduleLabel", moduleLabel)
				.append("group", group)
				.append("sourceChannelName", sourceChannelName)
				.append("sinkChannelName", sinkChannelName)
				.append("index", index)
				.append("type", moduleDefinition != null ? moduleDefinition.getGroupId() : null)
				.append("parameters", parameters)
				.append("children", children).toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(ModuleDescriptor o) {
		Assert.notNull(o, "ModuleDescriptor must not be null");
		return (index < o.index) ? -1 : ((index == o.index) ? 0 : 1);
	}


	/**
	 * Builder object for {@link ModuleDescriptor}.
	 * This object is mutable to allow for flexibility in specifying module
	 * type/fields/parameters during parsing.
	 */
	public static class Builder {

		/**
		 * @see ModuleDescriptor#moduleLabel
		 */
		private String moduleLabel;

		/**
		 * @see ModuleDescriptor#group
		 */
		private String group;

		/**
		 * @see ModuleDescriptor#sourceChannelName
		 */
		private String sourceChannelName;

		/**
		 * @see ModuleDescriptor#sinkChannelName
		 */
		private String sinkChannelName;

		/**
		 * @see ModuleDescriptor#index
		 */
		private int index;

		private String name;
		
		private String groupId;

		private String artifactId;

		private String version;

		/**
		 * @see ModuleDescriptor#parameters
		 */
		private final Map<String, String> parameters = new HashMap<String, String>();

		/**
		 * @see ModuleDescriptor#children
		 */
		private final List<ModuleDescriptor> children = new ArrayList<ModuleDescriptor>();

		private ModuleDefinition moduleDefinition;


		/**
		 * Set the module name.
		 * @param artifactId name of module
		 * @return this builder object
		 * @see ModuleDescriptor#getArtifactId
		 */
		public Builder setArtifactId(String artifactId) {
			this.artifactId = artifactId;
			return this;
		}

		/**
		 * Set the module label.
		 * @param moduleLabel name of module label
		 * @return this builder object
		 * @see ModuleDescriptor#moduleLabel
		 */
		public Builder setModuleLabel(String moduleLabel) {
			this.moduleLabel = moduleLabel;
			return this;
		}

		/**
		 * Set the module group.
		 * @param group name of module group
		 * @return this builder object
		 * @see ModuleDescriptor#group
		 */
		public Builder setGroup(String group) {
			this.group = group;
			return this;
		}

		public Builder setVersion(String version) {
			this.version = version;
			return this;
		}

		/**
		 * Set the module source channel name.
		 * @param sourceChannelName name of source channel; may be {@code null}
		 * @return this builder object
		 * @see ModuleDescriptor#sourceChannelName
		 */
		public Builder setSourceChannelName(String sourceChannelName) {
			this.sourceChannelName = sourceChannelName;
			return this;
		}

		/**
		 * Set the module sink channel name.
		 * @param sinkChannelName name of sink channel; may be {@code null}
		 * @return this builder object
		 * @see ModuleDescriptor#sinkChannelName
		 */
		public Builder setSinkChannelName(String sinkChannelName) {
			this.sinkChannelName = sinkChannelName;
			return this;
		}

		/**
		 * Set the module index.
		 * @param index position of module in stream/job definition
		 * @return this builder object
		 * @see ModuleDescriptor#index
		 */
		public Builder setIndex(int index) {
			this.index = index;
			return this;
		}

		/**
		 * Set the module type.
		 * @param groupId module groupId
		 * @return this builder object
		 * @see ModuleDescriptor#getGroupId() ()
		 */
		public Builder setGroupId(String groupId) {
			this.groupId = groupId;
			return this;
		}

		/**
		 * Add the list of children to the list of sub modules. This only applies if
		 * this builder is for a composite module.
		 * @param children sub modules
		 * @return this builder object
		 * @see ModuleDescriptor#children
		 */
		public Builder addChildren(List<ModuleDescriptor> children) {
			this.children.addAll(children);
			return this;
		}

		/**
		 * Set a module parameter.
		 * @param name parameter name
		 * @param value parameter value
		 * @return this builder object
		 * @see ModuleDescriptor#parameters
		 */
		public Builder setParameter(String name, String value) {
			this.parameters.put(name, value);
			return this;
		}

		/**
		 * Add the contents of the provided map to the map of module parameters.
		 * @param parameters module parameters
		 * @return this builder object
		 * @see ModuleDescriptor#parameters
		 */
		public Builder addParameters(Map<String, String> parameters) {
			this.parameters.putAll(parameters);
			return this;
		}


		/**
		 * Return symbolic name of a module. This may be explicitly specified in
		 * the DSL string, which is required if the stream contains another module
		 * with the same name. Otherwise, it will be the same as the module name.
		 * @return module label
		 */
		public String getModuleLabel() {
			return moduleLabel;
		}

		/**
		 * Return name of deployable unit this module instance belongs to
		 * (such as a stream or job).
		 * @return group name
		 */
		public String getGroup() {
			return group;
		}

		/**
		 * Return name of source channel, if defined by the stream/job definition.
		 * May be {@code null}.
		 * @return source channel name, or {@code null} if no source channel defined
		 */
		public String getSourceChannelName() {
			return sourceChannelName;
		}

		/**
		 * Return name of sink channel, if defined by the stream/job definition.
		 * May be {@code null}.
		 * @return sink channel name, or {@code null} if no sink channel defined
		 */
		public String getSinkChannelName() {
			return sinkChannelName;
		}

		/**
		 * Return position in stream/job definition relative to the other modules in
		 * the definition. 0 indicates the first/leftmost position.
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
		 * @return map of module parameters
		 */
		public Map<String, String> getParameters() {
			return parameters;
		}

		/**
		 * Create a {@code Builder} object pre-populated with the configuration
		 * for the provided {@link ModuleDescriptor}.
		 * @param descriptor module descriptor
		 * @return pre-populated builder object
		 */
		public static Builder fromModuleDescriptor(ModuleDescriptor descriptor) {
			Builder builder = new Builder();
			builder.setArtifactId(descriptor.getArtifactId());
			builder.setModuleLabel(descriptor.getModuleLabel());
			builder.setGroup(descriptor.getGroup());
			builder.setSourceChannelName(descriptor.getSourceChannelName());
			builder.setSinkChannelName(descriptor.getSinkChannelName());
			builder.setIndex(descriptor.getIndex());
			builder.setGroupId(descriptor.getGroupId());
			builder.setModuleDefinition(descriptor.getModuleDefinition());
			builder.addParameters(descriptor.getParameters());
			builder.addChildren(descriptor.getChildren());

			return builder;
		}

		/**
		 * Return a new instance of {@link ModuleDescriptor}.
		 * @return new instance of {@code ModuleDescriptor}
		 */
		public ModuleDescriptor build() {
			String label = moduleLabel != null ? moduleLabel : getModuleDefinition().getArtifactId();
			return new ModuleDescriptor(label, group,
					sourceChannelName, sinkChannelName,
					index, getModuleDefinition(),
					parameters, children);
		}

		public Builder setModuleDefinition(ModuleDefinition moduleDefinition) {
			this.moduleDefinition = moduleDefinition;
			return this;
		}

		public ModuleDefinition getModuleDefinition() {
			if (moduleDefinition == null) {
				moduleDefinition = new ModuleDefinition(name, artifactId, groupId, version) {
					@Override
					public boolean isComposed() {
						return false;
					}
				};
			}
			return moduleDefinition;
		}
	}

	/**
	 * Key to be used in Map of ModuleDescriptors.
	 */
	public static class Key implements Comparable<Key> {

		/**
		 * Group name.
		 */
		private final String group;

		/**
		 * Module groupId.
		 */
		private final String groupId;

		/**
		 * Module label.
		 */
		private final String label;

		/**
		 * Construct a key.
		 * @param group group name
		 * @param groupId module type
		 * @param label module label
		 */
		public Key(String group, String groupId, String label) {
			Assert.notNull(group, "Group is required");
			Assert.notNull(groupId, "GroupId is required");
			Assert.hasText(label, "Label is required");
			this.group = group;
			this.groupId = groupId;
			this.label = label;
		}

		/**
		 * Return the name of the stream.
		 * @return stream name
		 */
		public String getGroup() {
			return group;
		}

		/**
		 * Return the module type.
		 * @return module type
		 */
		public String getGroupId() {
			return groupId;
		}

		/**
		 * Return the module label.
		 * @return module label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compareTo(Key other) {
			int c = groupId.compareTo(other.getGroupId());
			if (c == 0) {
				c = label.compareTo(other.getLabel());
			}
			if (c == 0) {
				c = group.compareTo(other.getGroup());
			}
			return c;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o instanceof Key) {
				Key other = (Key) o;
				return group.equals(other.getGroup())
						&& groupId.equals(other.getGroupId()) && label.equals(other.getLabel());
			}

			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			int result = group.hashCode();
			result = 31 * result + groupId.hashCode();
			result = 31 * result + label.hashCode();
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return "ModuleDeploymentKey{" +
					"stream='" + group + '\'' +
					", type=" + groupId +
					", label='" + label + '\'' +
					'}';
		}

	}
}
