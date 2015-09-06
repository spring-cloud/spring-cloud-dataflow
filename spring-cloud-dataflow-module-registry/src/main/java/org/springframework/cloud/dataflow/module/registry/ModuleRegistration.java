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

import org.springframework.cloud.dataflow.core.ModuleCoordinates;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.util.Assert;

/**
 * Registration for a module name, {@link ModuleType type}, and
 * {@link ModuleCoordinates artifact coordinates}.
 *
 * @author Patrick Peralta
 */
public class ModuleRegistration implements Comparable<ModuleRegistration> {

	/**
	 * Module name.
	 */
	private final String name;

	/**
	 * Module type.
	 */
	private final ModuleType type;

	/**
	 * Maven coordinates for module artifact.
	 */
	private final ModuleCoordinates coordinates;

	/**
	 * Construct a {@code ModuleRegistration} object.
	 *
	 * @param name module name
	 * @param type module type
	 * @param coordinates coordinates for module artifact
	 */
	public ModuleRegistration(String name, ModuleType type, ModuleCoordinates coordinates) {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(type, "type must not be null");
		Assert.notNull(coordinates, "coordinates must not be null");
		this.name = name;
		this.type = type;
		this.coordinates = coordinates;
	}

	/**
	 * @see #name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see #type
	 */
	public ModuleType getType() {
		return type;
	}

	/**
	 * @see #coordinates
	 */
	public ModuleCoordinates getCoordinates() {
		return coordinates;
	}

	@Override
	public String toString() {
		return "ModuleRegistration{" +
				"name='" + name + '\'' +
				", type='" + type + '\'' +
				", coordinates=" + coordinates +
				'}';
	}

	@Override
	public int compareTo(ModuleRegistration that) {
		int i = this.type.compareTo(that.type);
		if (i == 0) {
			i = this.name.compareTo(that.name);
		}
		return i;
	}

}
