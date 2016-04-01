/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.artifact.registry;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Convenience wrapper for the {@link UriRegistry} that operates on higher level
 * {@link AppRegistration} objects and supports on-demand loading of {@link Resource}s.
 *
 * @author Mark Fisher
 */
public class AppRegistry {

	private final UriRegistry uriRegistry;

	private final ResourceLoader resourceLoader;

	public AppRegistry(UriRegistry uriRegistry, ResourceLoader resourceLoader) {
		this.uriRegistry = uriRegistry;
		this.resourceLoader = resourceLoader;
	}

	public AppRegistration find(String name, ArtifactType type) {
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
			String[] tokens = entry.getKey().split("\\.", 2);
			apps.add(new AppRegistration(tokens[1], ArtifactType.valueOf(tokens[0]),
					entry.getValue(), this.resourceLoader));
		}
		return apps;
	}

	public void save(String name, ArtifactType type, URI uri) {
		this.uriRegistry.register(key(name, type), uri);
	}

	public void delete(String name, ArtifactType type) {
		this.uriRegistry.unregister(key(name, type));
	}

	private String key(String name, ArtifactType type) {
		return String.format("%s.%s", type, name);
	}
}
