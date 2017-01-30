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
import org.springframework.util.Assert;

/**
 * Convenience wrapper for the {@link UriRegistry} that operates on higher level
 * {@link AppRegistration} objects and supports on-demand loading of {@link Resource}s.
 *
 * <p>Stores AppRegistration with up to two keys:<ul>
 *     <li>{@literal <type>.<name>}: URI for the actual app</li>
 *     <li>{@literal <type>.<name>.metadata}: Optional URI for the app metadata</li>
 * </ul></p>
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Thomas Risberg
 * @author Eric Bottard
 */
public class AppRegistry {

	public static final String METADATA_KEY_SUFFIX = "metadata";

	private final UriRegistry uriRegistry;

	private final ResourceLoader resourceLoader;

	public AppRegistry(UriRegistry uriRegistry, ResourceLoader resourceLoader) {
		this.uriRegistry = uriRegistry;
		this.resourceLoader = resourceLoader;
	}

	public AppRegistration find(String name, ApplicationType type) {
		try {
			URI uri = this.uriRegistry.find(key(name, type));
			URI metadataUri = null;
			try {
				metadataUri = this.uriRegistry.find(metadataKey(name, type));
			}
			catch (IllegalArgumentException ignored) {
			}
			return new AppRegistration(name, type, uri, metadataUri, this.resourceLoader);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	public List<AppRegistration> findAll() {
		List<AppRegistration> apps = new ArrayList<>();
		for (Map.Entry<String, URI> entry : this.uriRegistry.findAll().entrySet()) {
			String[] tokens = entry.getKey().split("\\.");
			if (tokens.length == 2) {
				String name = tokens[1];
				ApplicationType type = ApplicationType.valueOf(tokens[0]);
				URI metadataUri = null;
				try {
					metadataUri = this.uriRegistry.find(entry.getKey() + "." + METADATA_KEY_SUFFIX);
				}
				catch (IllegalArgumentException ignored) {
				}
				apps.add(new AppRegistration(name, type, entry.getValue(), metadataUri, this.resourceLoader));
			} else {
				Assert.isTrue(tokens.length == 3
						&& METADATA_KEY_SUFFIX.equals(tokens[2]),
					"Invalid format for app key '" + entry.getKey() + "'in registry. Must be <type>.<name> or <type>.<name>.metadata");
			}
		}
		return apps;
	}

	public AppRegistration save(String name, ApplicationType type, URI uri, URI metadataUri) {
		this.uriRegistry.register(key(name, type), uri);
		if (metadataUri != null) {
			this.uriRegistry.register(metadataKey(name, type), metadataUri);
		}
		return new AppRegistration(name, type, uri, metadataUri, this.resourceLoader);
	}

	public List<AppRegistration> importAll(boolean overwrite, Resource... resources) {
		List<AppRegistration> apps = new ArrayList<>();
		for (Resource resource :resources) {
			try {
				Map<String, URI> registered = UriRegistryPopulator.populateRegistry(
					overwrite, this.uriRegistry, resource);
				for (Map.Entry<String, URI> entry : registered.entrySet()) {
					String[] tokens = entry.getKey().split("\\.");
					if (tokens.length == 2) {
						String name = tokens[1];
						ApplicationType type = ApplicationType.valueOf(tokens[0]);
						URI metadataUri = registered.get(entry.getKey() + "." + METADATA_KEY_SUFFIX); // can be null
						apps.add(new AppRegistration(name, type, entry.getValue(), metadataUri, this.resourceLoader));
					} else {
						Assert.isTrue(tokens.length == 3
							&& METADATA_KEY_SUFFIX.equals(tokens[2]),
							"Invalid format for app key '" + entry.getKey() + "'in file. Must be <type>.<name> or <type>.<name>.metadata");
					}
				}
			}
			catch (Exception e) {
				throw new IllegalStateException("Error when registering applications from " + resource.getDescription() + ": " + e.getMessage(), e);
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
			this.uriRegistry.unregister(metadataKey(name, type));
		}
		else {
			throw new NoSuchAppRegistrationException(name, type);
		}
	}

	private String key(String name, ApplicationType type) {
		return String.format("%s.%s", type, name);
	}

	private String metadataKey(String name, ApplicationType type) {
		return String.format("%s.%s.%s", type, name, METADATA_KEY_SUFFIX);
	}
}
