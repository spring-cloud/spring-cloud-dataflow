/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.registry;

import java.net.URI;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * This maps a (name + type) pair to a URI and provides on-demand access to the {@link Resource}.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class AppRegistration implements Comparable<AppRegistration> {

	/**
	 * App name.
	 */
	private final String name;

	/**
	 * App type.
	 */
	private final ApplicationType type;

	/**
	 * URI for the app resource.
	 */
	private final URI uri;

	/**
	 * {@link ResourceLoader} to load the Resource for this app.
	 */
	private final ResourceLoader loader;

	/**
	 * The actual {@link Resource} for this app, loaded on-demand and cached.
	 */
	private volatile Resource resource;

	/**
	 * Construct an {@code AppRegistration} object.
	 *
	 * @param name app name
	 * @param type app type
	 * @param uri URI for the app resource
	 * @param loader the {@link ResourceLoader} that loads the {@link Resource} for this app
	 */
	public AppRegistration(String name, ApplicationType type, URI uri, ResourceLoader loader) {
		Assert.hasText(name, "name is required");
		Assert.notNull(type, "type is required");
		Assert.notNull(uri, "uri is required");
		Assert.notNull(loader, "ResourceLoader must not be null");
		this.name = name;
		this.type = type;
		this.uri = uri;
		this.loader = loader;
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
	public ApplicationType getType() {
		return type;
	}

	/**
	 * @see #uri
	 */
	public URI getUri() {
		return uri;
	}

	public Resource getResource() {
		if (this.resource == null) {
			this.resource = this.loader.getResource(this.uri.toString());
		}
		return this.resource;
	}

	@Override
	public String toString() {
		return "AppRegistration{" +
				"name='" + name + '\'' +
				", type='" + type + '\'' +
				", uri=" + uri +
				'}';
	}

	@Override
	public int compareTo(AppRegistration that) {
		int i = this.type.compareTo(that.type);
		if (i == 0) {
			i = this.name.compareTo(that.name);
		}
		return i;
	}

}
