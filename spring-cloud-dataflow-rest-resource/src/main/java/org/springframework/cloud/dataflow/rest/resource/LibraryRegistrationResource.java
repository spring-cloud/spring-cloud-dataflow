/*
 * Copyright 2015-2016 the original author or authors.
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

import org.springframework.hateoas.PagedResources;

/**
 * REST resource for a library registration.
 *
 * @author Eric Bottard
 */
public class LibraryRegistrationResource extends ModuleRegistrationResource {

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected LibraryRegistrationResource() {
	}

	public LibraryRegistrationResource(String name, String type, String uri) {
		super(name, type, uri);
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 */
	public static class Page extends PagedResources<LibraryRegistrationResource> {
	}

}
