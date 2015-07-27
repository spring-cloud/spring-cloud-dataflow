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

package org.springframework.cloud.data.module;

import java.io.Serializable;

import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Unique identifier for a {@link org.springframework.xd.module.ModuleDescriptor}.
 * Methods {@link #toUID()} and {@link #fromUID(String)} can be used to convert
 * a key to a string and a string to a key, respectively. The UID string may be
 * used to uniquely identify a module in a database or execution environment.
 *
 * @author Patrick Peralta
 */
public class ModuleKey implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The name of the associated group or deployment unit this
	 * module belongs to, such as a stream or job.
	 */
	private final String group;

	/**
	 * The name of the module as defined in the module registry,
	 * such as {@code http} or {@code file}.
	 */
	private final String name;

	/**
	 * The label provided to uniquely identify the module as part
	 * of group membership. For instance, if a group has multiple
	 * {@code filter} modules, each will have a unique label.
	 */
	private final String label;

	/**
	 * Construct a {@code ModuleKey}.
	 *
	 * @param group name of group (stream/job) this module belongs to
	 * @param name  name of module as defined in module registry
	 * @param label label to uniquely identify this module in its group
	 */
	public ModuleKey(String group, String name, String label) {
		Assert.hasText(group);
		Assert.hasText(name);
		Assert.hasText(label);
		Assert.doesNotContain(group, ".");
		Assert.doesNotContain(name, ".");
		Assert.doesNotContain(label, ".");
		this.group = group;
		this.name = name;
		this.label = label;
	}

	/**
	 * @see #group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @see #name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see #label
	 */
	public String getLabel() {
		return label;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ModuleKey that = (ModuleKey) o;
		return this.group.equals(that.group)
				&& this.name.equals(that.name)
				&& this.label.equals(that.label);
	}

	@Override
	public int hashCode() {
		int result = group.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + label.hashCode();
		return result;
	}

	/**
	 * Return a string containing the key fields separated by
	 * periods. This string may be used as a key in a database
	 * to uniquely identify a module.
	 *
	 * @return string representation of this key
	 */
	public String toUID() {
		return String.format("%s.%s.%s", this.group, this.name, this.label);
	}

	/**
	 * Parse the given string and return a {@code ModuleKey} based
	 * on the string contents.
	 *
	 * @param uid uid containing the fields for a {@code ModuleKey}
	 * @return a new {@code ModuleKey} based on the provided string
	 * @throws NullPointerException if a null uid is provided
	 * @throws IllegalArgumentException if an invalid uid is provided
	 */
	public static ModuleKey fromUID(String uid) {
		if (uid == null) {
			throw new NullPointerException("uid == null");
		}

		String[] fields = uid.split("\\.");
		if (fields.length != 3) {
			throw new IllegalArgumentException(String.format("invalid format for uid '%s'", uid));
		}

		return new ModuleKey(fields[0], fields[1], fields[2]);
	}

	/**
	 * Return a {@code ModuleKey} based on the provided {@link ModuleDescriptor}.
	 *
	 * @param descriptor module descriptor to generate a key for
	 * @return new {@code ModuleKey} for the provided moulde descriptor
	 */
	public static ModuleKey fromModuleDescriptor(ModuleDescriptor descriptor) {
		return new ModuleKey(descriptor.getGroup(), descriptor.getModuleName(),
				descriptor.getModuleLabel());
	}

}
