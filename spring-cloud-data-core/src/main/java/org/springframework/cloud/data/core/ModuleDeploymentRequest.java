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
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Representation of a module deployment request. This includes
 * module options defined at definition time (as part of a stream
 * definition) as well as module deployment properties.
 *
 * @author Patrick Peralta
 */
public class ModuleDeploymentRequest {

	/**
	 * Module definition.
	 */
	private final ModuleDefinition definition;

	/**
	 * Coordinates for module jar file.
	 */
	private final ModuleCoordinates coordinates;

	/**
	 * Map of deployment properties for this module.
	 */
	private final Map<String, String> deploymentProperties;

	/**
	 * Number of module instances to launch.
	 */
	private final int count;

	/**
	 * Construct a {@code ModuleDeploymentRequest}.
	 *
	 * @param definition module definition
	 * @param coordinates maven coordinates for module jar file
	 * @param deploymentProperties map of deployment properties; may be {@code null}
	 */
	public ModuleDeploymentRequest(ModuleDefinition definition, ModuleCoordinates coordinates,
			Map<String, String> deploymentProperties) {
		Assert.notNull(definition, "definition must not be null");
		Assert.notNull(coordinates, "coordinates must not be null");
		this.definition = definition;
		this.coordinates = coordinates;
		this.deploymentProperties = deploymentProperties == null
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(deploymentProperties);
		this.count = this.deploymentProperties.containsKey("count")
				? Integer.parseInt(this.deploymentProperties.get("count"))
				: 1;
	}

	/**
	 * Construct a {@code ModuleDeploymentRequest} for one instance and
	 * no deployment properties.
	 *
	 * @param definition module definition
	 * @param coordinates coordinates for module jar file
	 */
	public ModuleDeploymentRequest(ModuleDefinition definition, ModuleCoordinates coordinates) {
		this(definition, coordinates, null);
	}

	/**
	 * @see #definition
	 */
	public ModuleDefinition getDefinition() {
		return definition;
	}

	/**
	 * @see #coordinates
	 */
	public ModuleCoordinates getCoordinates() {
		return coordinates;
	}

	/**
	 * @see #count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @see #deploymentProperties
	 */
	public Map<String, String> getDeploymentProperties() {

		return deploymentProperties;
	}
}
