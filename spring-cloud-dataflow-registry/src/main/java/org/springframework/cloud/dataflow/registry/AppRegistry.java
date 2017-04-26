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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Convenience wrapper for the {@link UriRegistry} that operates on higher level
 * {@link AppRegistration} objects and supports on-demand loading of {@link Resource}s.
 * <p>
 * <p>
 * Stores AppRegistration with up to two keys:
 * </p>
 * <ul>
 * <li>{@literal <type>.<name>}: URI for the actual app</li>
 * <li>{@literal <type>.<name>.metadata}: Optional URI for the app metadata</li>
 * </ul>
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Thomas Risberg
 * @author Eric Bottard
 */
public class AppRegistry {

	private static final Logger logger = LoggerFactory.getLogger(AppRegistry.class);

	private static final String METADATA_KEY_SUFFIX = "metadata";

	private final UriRegistry uriRegistry;

	private final ResourceLoader resourceLoader;

	public AppRegistry(UriRegistry uriRegistry, ResourceLoader resourceLoader) {
		this.uriRegistry = uriRegistry;
		this.resourceLoader = resourceLoader;
	}

	public AppRegistration find(String name, ApplicationType type) {
		try {
			String key = key(name, type);
			URI uri = this.uriRegistry.find(key);
			URI metadataUri = metadataUriFromRegistry().apply(key);
			return new AppRegistration(name, type, uri, metadataUri, this.resourceLoader);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	public List<AppRegistration> findAll() {
		return this.uriRegistry.findAll().entrySet().stream().flatMap(toValidAppRegistration(metadataUriFromRegistry()))
				.collect(Collectors.toList());
	}

	public AppRegistration save(String name, ApplicationType type, URI uri, URI metadataUri) {
		this.uriRegistry.register(key(name, type), uri);
		if (metadataUri != null) {
			this.uriRegistry.register(metadataKey(name, type), metadataUri);
		}
		return new AppRegistration(name, type, uri, metadataUri, this.resourceLoader);
	}

	public List<AppRegistration> importAll(boolean overwrite, Resource... resources) {

		Set<String> keysAlreadyThere = overwrite ? Collections.emptySet() : uriRegistry.findAll().keySet();

		List<AppRegistration> apps = new ArrayList<>();
		for (Resource resource : resources) {
			Properties properties = new Properties();
			try (InputStream is = resource.getInputStream()) {
				properties.load(is);

				properties.entrySet().stream().map(toStringAndUri())
						.flatMap(toValidAppRegistration(metadataUriFromProperties(properties)))
						.filter(ar -> !keysAlreadyThere.contains(key(ar.getName(), ar.getType())))
						.collect(Collectors.toList()) // Force eager evaluation to fail
														// early
						.forEach(ar -> apps.add(save(ar.getName(), ar.getType(), ar.getUri(), ar.getMetadataUri())));
			}
			catch (IOException e) {
				throw new RuntimeException("Error reading from " + resource.getDescription(), e);
			}
		}

		return apps;
	}

	private Function<Map.Entry<Object, Object>, AbstractMap.SimpleImmutableEntry<String, URI>> toStringAndUri() {
		return kv -> {
			try {
				return new AbstractMap.SimpleImmutableEntry<>((String) kv.getKey(), new URI((String) kv.getValue()));
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		};
	}

	/**
	 * Returns a Function that either
	 * <ul>
	 * <li>turns a key/value mapping into a valid AppRegistration (1 element Stream),</li>
	 * <li>silently ignores well formed metadata entries (0 element Stream) or</li>
	 * <li>fails otherwise.</li>
	 * </ul>
	 *
	 * @param metadataUriExtractor a Function able to compute the (possibly null)
	 * metadataUri from a given app key
	 */
	private Function<Map.Entry<String, URI>, Stream<AppRegistration>> toValidAppRegistration(
			Function<String, URI> metadataUriExtractor) {
		return (Map.Entry<String, URI> kv) -> {
			String key = kv.getKey();
			String[] tokens = key.split("\\.");
			if (tokens.length == 2) {
				String name = tokens[1];
				ApplicationType type = ApplicationType.valueOf(tokens[0]);
				URI appURI = warnOnMalformedURI(key, kv.getValue());
				URI metadataURI = metadataUriExtractor.apply(key);
				return Stream.of(new AppRegistration(name, type, appURI, metadataURI, resourceLoader));

			}
			else {
				Assert.isTrue(tokens.length == 3 && METADATA_KEY_SUFFIX.equals(tokens[2]),
						"Invalid format for app key '" + key + "'in file. Must be <type>.<name> or <type>.<name>"
								+ ".metadata");
				return Stream.empty();
			}
		};
	}

	private Function<String, URI> metadataUriFromProperties(Properties properties) {
		return key -> {
			String metadataValue = properties.getProperty(metadataKey(key));
			try {
				return metadataValue != null ? warnOnMalformedURI(key, new URI(metadataValue)) : null;
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		};
	}

	private Function<String, URI> metadataUriFromRegistry() {
		return key -> {
			try {
				return uriRegistry.find(metadataKey(key));
			}
			catch (IllegalArgumentException ignored) {
				return null;
			}
		};
	}

	/**
	 * Deletes an {@link AppRegistration}. If the {@link AppRegistration} does not exist,
	 * a {@link NoSuchAppRegistrationException} will be thrown.
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

	private String metadataKey(String key) {
		return key + "." + METADATA_KEY_SUFFIX;
	}

	private URI warnOnMalformedURI(String key, URI uri) {
		if (StringUtils.isEmpty(uri)) {
			logger.warn(String.format("Error when registering '%s': URI is required", key));
		}
		else if (!StringUtils.hasText(uri.getScheme())) {
			logger.warn(
					String.format("Error when registering '%s' with URI %s: URI scheme must be specified", key, uri));
		}
		else if (!StringUtils.hasText(uri.getSchemeSpecificPart())) {
			logger.warn(String.format(
					"Error when registering '%s' with URI %s: URI scheme-specific part must be " + "specified", key,
					uri));
		}
		return uri;
	}
}
