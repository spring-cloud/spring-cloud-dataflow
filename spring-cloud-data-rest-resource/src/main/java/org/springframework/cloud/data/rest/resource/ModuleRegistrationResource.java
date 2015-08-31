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

package org.springframework.cloud.data.rest.resource;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 * Rest resource for a module registration.
 *
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Patrick Peralta
 */
public class ModuleRegistrationResource extends ResourceSupport {

	/**
	 * Module name.
	 */
	private String name;

	/**
	 * Module type.
	 */
	private String type;

	/**
	 * Maven coordinates for module artifact. String is in standard format
	 * such as {@code groupId:artifactId:version}.
	 */
	private String coordinates;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected ModuleRegistrationResource() {
	}

	/**
	 * Construct a {@code ModuleRegistrationResource}.
	 *
	 * @param name module name
	 * @param type module type
	 * @param coordinates coordinates for module artifact
	 */
	public ModuleRegistrationResource(String name, String type, String coordinates) {
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
	public String getType() {
		return type;
	}

	/**
	 * @see #coordinates
	 */
	public String getCoordinates() {
		return coordinates;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 */
	public static class Page extends PagedResources<ModuleRegistrationResource> {
	}

}
