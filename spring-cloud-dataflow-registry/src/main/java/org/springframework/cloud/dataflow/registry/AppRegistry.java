/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.registry;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistryPopulator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Convenience wrapper for the {@link UriRegistry} that operates on higher level
 * {@link AppRegistration} objects and supports on-demand loading of {@link Resource}s.
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Thomas Risberg
 */
public class AppRegistry {

	private final UriRegistry uriRegistry;

	private final UriRegistryPopulator uriRegistryPopulator;

	private final ResourceLoader resourceLoader;

	public AppRegistry(UriRegistry uriRegistry, ResourceLoader resourceLoader) {
		this.uriRegistry = uriRegistry;
		this.uriRegistryPopulator = new UriRegistryPopulator();
		this.uriRegistryPopulator.setResourceLoader(resourceLoader);
		this.resourceLoader = resourceLoader;
	}

	public AppRegistration find(String name, ApplicationType type) {
		try {
			URI uri = this.uriRegistry.find(key(name, type));
			return new AppRegistration(name, type, uri, this.resourceLoader);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	public List<AppRegistration> findAll() {
		List<AppRegistration> apps = new ArrayList<>();
		for (Map.Entry<String, URI> entry : this.uriRegistry.findAll().entrySet()) {
			apps.add(createAppRegistration(entry.getKey(), entry.getValue()));
		}
		return apps;
	}

	public AppRegistration save(String name, ApplicationType type, URI uri) {
		this.uriRegistry.register(key(name, type), uri);
		return new AppRegistration(name, type, uri, this.resourceLoader);
	}

	public List<AppRegistration> importAll(boolean overwrite, String... resourceUris) {
		List<AppRegistration> apps = new ArrayList<>();
		for (String uri : resourceUris) {
			try {
				Map<String, URI> registered = this.uriRegistryPopulator.populateRegistry(
						overwrite, this.uriRegistry, uri);
				for (Map.Entry<String, URI> entry : registered.entrySet()) {
					apps.add(createAppRegistration(entry.getKey(), entry.getValue()));
				}
			}
			catch (Exception e) {
				throw new IllegalStateException("Error when registering applications from " + uri + ": " + e.getMessage(), e);
			}
		}
		return apps;
	}

	/**
	 * Deletes an {@link AppRegistration}. If the {@link AppRegistration} does not
	 * exist, a {@link NoSuchAppRegistrationException} will be thrown.
	 *
	 * @param name Name of the AppRegistration to delete
	 * @param type Type of the AppRegistration to delete
	 */
	public void delete(String name, ApplicationType type) {
		if (this.find(name, type) != null) {
			this.uriRegistry.unregister(key(name, type));
		}
		else {
			throw new NoSuchAppRegistrationException(name, type);
		}
	}

	private String key(String name, ApplicationType type) {
		return String.format("%s.%s", type, name);
	}

	private AppRegistration createAppRegistration(String key, URI uri) {
		String[] tokens = key.split("\\.", 2);
		if (tokens.length != 2) {
			throw new IllegalArgumentException("Invalid application key: " + key +
					"; the expected format is <name>.<type>");
		}
		return new AppRegistration(tokens[1], ApplicationType.valueOf(tokens[0]), uri, this.resourceLoader);
	}
}
