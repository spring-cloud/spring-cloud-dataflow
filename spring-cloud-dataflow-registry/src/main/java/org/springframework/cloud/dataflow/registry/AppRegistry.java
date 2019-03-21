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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

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
 * @author Christian Tzolov
 */
public class AppRegistry extends AbstractAppRegistryCommon implements AppRegistryCommon {

	private static final Logger logger = LoggerFactory.getLogger(AppRegistry.class);

	private final UriRegistry uriRegistry;

	private static final Function<Map.Entry<Object, Object>, AbstractMap.SimpleImmutableEntry<String, URI>> toStringAndUriFUNC = kv -> {
		try {
			return new AbstractMap.SimpleImmutableEntry<>((String) kv.getKey(), new URI((String) kv.getValue()));
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	};

	public AppRegistry(UriRegistry uriRegistry, ResourceLoader resourceLoader) {
		super(resourceLoader);
		Assert.notNull(uriRegistry, "'uriRegistry' must not be null");
		this.uriRegistry = uriRegistry;
	}

	public AppRegistry(UriRegistry uriRegistry, ResourceLoader resourceLoader, MavenProperties mavenProperties) {
		super(resourceLoader, mavenProperties);
		Assert.notNull(uriRegistry, "'uriRegistry' must not be null");
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		this.uriRegistry = uriRegistry;
	}

	public AppRegistration find(String name, ApplicationType type) {
		try {
			String key = key(name, type);
			URI uri = this.uriRegistry.find(key);
			URI metadataUri = metadataUriFromRegistry(key);
			return new AppRegistration(name, type, uri, metadataUri);
		}
		catch (IllegalArgumentException e) {
			return null; // ignore and treat as not found
		}
	}

	@Override
	public List<AppRegistration> findAll() {
		return this.uriRegistry.findAll().entrySet().stream()
				.flatMap(kv -> toValidAppRegistration(kv, metadataUriFromRegistry(kv.getKey())))
				.sorted((a, b) -> a.compareTo(b))
				.collect(Collectors.toList());
	}

	public Page<AppRegistration> findAll(Pageable pageable) {
		List<AppRegistration> appRegistrations = this.findAll();
		long to = Math.min(appRegistrations.size(), pageable.getOffset() + pageable.getPageSize());

		// if a request for page is higher than number of items we actually have is either
		// a rogue request.
		// in this case we simply reset to first page.
		// we also need to explicitly set page and see what offset is when
		// building new page.
		// all this is done because we don't use a proper repository which would
		// handle all these automatically.
		int offset = 0;
		int page = 0;
		if (pageable.getOffset() <= to) {
			offset = pageable.getOffset();
			page = pageable.getPageNumber();
		}
		else if (pageable.getOffset() + pageable.getPageSize() <= to) {
			offset = pageable.getOffset();
		}

		return new PageImpl<>(appRegistrations.subList(offset, (int) to), new PageRequest(page, pageable.getPageSize()),
				appRegistrations.size());
	}

	public AppRegistration save(String name, ApplicationType type, URI uri, URI metadataUri) {
		this.uriRegistry.register(key(name, type), uri);
		if (metadataUri != null) {
			this.uriRegistry.register(metadataKey(name, type), metadataUri);
		}
		return new AppRegistration(name, type, uri, metadataUri);
	}

	@Override
	public Stream<AppRegistration> toValidAppRegistration(Map.Entry<String, URI> kv, URI metadataURI) {
		String key = kv.getKey();
		String[] tokens = key.split("\\.");
		if (tokens.length == 2) {
			String name = tokens[1];
			ApplicationType type = ApplicationType.valueOf(tokens[0]);
			URI appURI = warnOnMalformedURI(key, kv.getValue());
			return Stream.of(new AppRegistration(name, type, "none", appURI, metadataURI));
		}
		else {
			Assert.isTrue(tokens.length == 3 && METADATA_KEY_SUFFIX.equals(tokens[2]),
					"Invalid format for app key '" + key + "'in file. Must be <type>.<name> or <type>.<name>"
							+ ".metadata");
			return Stream.empty();
		}
	}

	@Override
	protected boolean isOverwrite(AppRegistration app, boolean overwrite) {
		if (overwrite) {
			return true;
		}

		return !uriRegistry.findAll().keySet().contains(key(app.getName(), app.getType()));
	}

	@Override
	public AppRegistration save(AppRegistration app) {
		return save(app.getName(), app.getType(), app.getUri(), app.getMetadataUri());
	}

	/**
	 * Deletes an {@link AppRegistration}. If the {@link AppRegistration} does not exist, a
	 * {@link NoSuchAppRegistrationException} will be thrown.
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

	@Override
	public boolean appExist(String name, ApplicationType type) {
		return find(name, type) != null;
	}

}
