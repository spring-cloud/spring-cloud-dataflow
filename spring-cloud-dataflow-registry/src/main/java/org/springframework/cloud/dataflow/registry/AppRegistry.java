/*
 * Copyright 2017 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
 * @author Ilayaperumal Gopinathan
 * @author Oleg Zhurakousky
 */
public class AppRegistry {

	private static final Logger logger = LoggerFactory.getLogger(AppRegistry.class);

	private static final String METADATA_KEY_SUFFIX = "metadata";

	private final UriRegistry uriRegistry;

	private final ResourceLoader resourceLoader;

	private static final Function<Map.Entry<Object, Object>, AbstractMap.SimpleImmutableEntry<String, URI>> toStringAndUriFUNC =
			kv -> {
				try {
					return new AbstractMap.SimpleImmutableEntry<>((String) kv.getKey(), new URI((String) kv.getValue()));
				}
				catch (URISyntaxException e) {
					throw new IllegalArgumentException(e);
				}
			};

	public AppRegistry(UriRegistry uriRegistry, ResourceLoader resourceLoader) {
		Assert.notNull(uriRegistry, "'uriRegistry' must not be null");
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		this.uriRegistry = uriRegistry;
		this.resourceLoader = resourceLoader;
	}

	public AppRegistration find(String name, ApplicationType type) {
		try {
			String key = key(name, type);
			URI uri = this.uriRegistry.find(key);
			URI metadataUri = metadataUriFromRegistry(key);
			return new AppRegistration(name, type, uri, metadataUri, this.resourceLoader);
		}
		catch (IllegalArgumentException e) {
			return null; //ignore and treat as not found
		}
	}

	public List<AppRegistration> findAll() {
		return this.uriRegistry.findAll().entrySet().stream()
				.flatMap(kv -> toValidAppRegistration(kv, metadataUriFromRegistry(kv.getKey())))
				.sorted((a,b) -> a.compareTo(b))
				.collect(Collectors.toList());
	}

	public Page<AppRegistration> findAll(Pageable pageable) {
		List<AppRegistration> appRegistrations = this.findAll();
		long to = Math.min(appRegistrations.size(), pageable.getOffset() + pageable.getPageSize());

		return new PageImpl<>(appRegistrations.subList(pageable.getOffset(), (int) to), pageable,
				appRegistrations.size());
	}

	public AppRegistration save(String name, ApplicationType type, URI uri, URI metadataUri) {
		this.uriRegistry.register(key(name, type), uri);
		if (metadataUri != null) {
			this.uriRegistry.register(metadataKey(name, type), metadataUri);
		}
		return new AppRegistration(name, type, uri, metadataUri, this.resourceLoader);
	}

	public List<AppRegistration> importAll(boolean overwrite, Resource... resources) {
		Set<String> registeredKeys = overwrite ? Collections.emptySet() : uriRegistry.findAll().keySet();
		return Stream.of(resources)
			.map(this::loadProperties)
			.flatMap(prop -> prop.entrySet().stream()
					.map(toStringAndUriFUNC)
					.flatMap(kv -> toValidAppRegistration(kv, metadataUriFromProperties(kv.getKey(), prop)))
					.filter(ar -> !registeredKeys.contains(key(ar.getName(), ar.getType())))
					.map(ar -> save(ar.getName(), ar.getType(), ar.getUri(), ar.getMetadataUri()))
			).collect(Collectors.toList());
	}

	private Properties loadProperties(Resource resource) {
		try {
			return PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException e) {
			throw new RuntimeException("Error reading from " + resource.getDescription(), e);
		}
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

	/**
	 * Builds a {@link Stream} from key/value mapping.
	 * @return
	 * <ul>
	 * <li>valid AppRegistration as single element Stream </li>
	 * <li>silently ignores well malformed metadata entries (0 element Stream) or</li>
	 * <li>fails otherwise.</li>
	 * </ul>
	 *
	 * @param kv key/value representing app key (key) and app URI (value)
	 * @param metadataURI metadataUri computed from a given app key
	 */
	private Stream<AppRegistration> toValidAppRegistration(Entry<String, URI> kv, URI metadataURI) {
		String key = kv.getKey();
		String[] tokens = key.split("\\.");
		if (tokens.length == 2) {
			String name = tokens[1];
			ApplicationType type = ApplicationType.valueOf(tokens[0]);
			URI appURI = warnOnMalformedURI(key, kv.getValue());
			return Stream.of(new AppRegistration(name, type, appURI, metadataURI, resourceLoader));
		}
		else {
			Assert.isTrue(tokens.length == 3 && METADATA_KEY_SUFFIX.equals(tokens[2]),
					"Invalid format for app key '" + key + "'in file. Must be <type>.<name> or <type>.<name>"
							+ ".metadata");
			return Stream.empty();
		}
	}

	private URI metadataUriFromProperties(String key, Properties properties) {
		String metadataValue = properties.getProperty(metadataKey(key));
		try {
			return metadataValue != null ? warnOnMalformedURI(key, new URI(metadataValue)) : null;
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private URI metadataUriFromRegistry(String key) {
		try {
			return uriRegistry.find(metadataKey(key));
		}
		catch (IllegalArgumentException ignored) {
			return null;
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
			logger.warn(String.format("Error when registering '%s' with URI %s: URI scheme must be specified", key, uri));
		}
		else if (!StringUtils.hasText(uri.getSchemeSpecificPart())) {
			logger.warn(String.format("Error when registering '%s' with URI %s: URI scheme-specific part must be " +
					"specified", key, uri));
		}
		return uri;
	}
}
