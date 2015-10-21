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

import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.util.Assert;

/**
 * This maps a (name + type) pair to artifact coordinates.
 *
 * @author Patrick Peralta
 */
public class ArtifactRegistration implements Comparable<ArtifactRegistration> {

	/**
	 * Module/Library symbolic name.
	 */
	private final String name;

	/**
	 * Artifact type.
	 */
	private final ArtifactType type;

	/**
	 * Maven coordinates for the artifact.
	 */
	private final ArtifactCoordinates coordinates;

	/**
	 * Construct a {@code ArtifactRegistration} object.
	 *
	 * @param name artifact name
	 * @param type artifact type
	 * @param coordinates coordinates for the artifact
	 */
	public ArtifactRegistration(String name, ArtifactType type, ArtifactCoordinates coordinates) {
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
	public ArtifactType getType() {
		return type;
	}

	/**
	 * @see #coordinates
	 */
	public ArtifactCoordinates getCoordinates() {
		return coordinates;
	}

	@Override
	public String toString() {
		return "ArtifactRegistration{" +
				"name='" + name + '\'' +
				", type='" + type + '\'' +
				", coordinates=" + coordinates +
				'}';
	}

	@Override
	public int compareTo(ArtifactRegistration that) {
		int i = this.type.compareTo(that.type);
		if (i == 0) {
			i = this.name.compareTo(that.name);
		}
		return i;
	}

}
