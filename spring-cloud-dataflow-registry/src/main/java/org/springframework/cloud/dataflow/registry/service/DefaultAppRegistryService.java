/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.registry.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.AuditServiceUtils;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Convenience wrapper for the {@link AppRegistryService} that operates on higher level
 * {@link DefaultAppRegistryService} objects and supports on-demand loading of
 * {@link Resource}s.
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
 * @author Chris Schaefer
 */
@Transactional
public class DefaultAppRegistryService implements AppRegistryService {

	public static final String METADATA_KEY_SUFFIX = "metadata";

	protected static final Logger logger = LoggerFactory.getLogger(DefaultAppRegistryService.class);

	private final AppRegistrationRepository appRegistrationRepository;

	private AppResourceCommon appResourceCommon;

	protected final AuditRecordService auditRecordService;

	protected final AuditServiceUtils auditServiceUtils;

	public DefaultAppRegistryService(AppRegistrationRepository appRegistrationRepository,
			AppResourceCommon appResourceCommon, AuditRecordService auditRecordService) {
		Assert.notNull(appResourceCommon, "'appResourceCommon' must not be null");
		Assert.notNull(appRegistrationRepository, "'appRegistrationRepository' must not be null");
		Assert.notNull(auditRecordService, "'auditRecordService' must not be null");
		this.appResourceCommon = appResourceCommon;
		this.appRegistrationRepository = appRegistrationRepository;
		this.auditRecordService = auditRecordService;
		this.auditServiceUtils = new AuditServiceUtils();
	}

	@Override
	public AppRegistration find(String name, ApplicationType type) {
		return this.getDefaultApp(name, type);
	}

	@Override
	public AppRegistration find(String name, ApplicationType type, String version) {
		return this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(name, type, version);
	}

	@Override
	public AppRegistration getDefaultApp(String name, ApplicationType type) {
		return this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(name, type);
	}

	@Override
	public void validate(AppRegistration registration, String uri, String version) {
		if (registration != null && StringUtils.hasText(version)) {
			String defaultAppUri = registration.getUri().toString();
			String defaultAppUriNoVersion = removeLastMatch(defaultAppUri, registration.getVersion());
			String newAppUriNoVersion = removeLastMatch(uri, version);
			if (!ObjectUtils.nullSafeEquals(defaultAppUriNoVersion, newAppUriNoVersion)) {
				throw new IllegalArgumentException("Existing default application [" + defaultAppUri
						+ "] can only differ by a version but is [" + uri + "]");
			}
		}
	}

	private static String removeLastMatch(String original, String match) {
		StringBuilder builder = new StringBuilder();
		int start = original.lastIndexOf(match);
		builder.append(original.substring(0, start));
		builder.append(original.substring(start + match.length()));
		return builder.toString();
	}

	@Override
	public void setDefaultApp(String name, ApplicationType type, String version) {
		AppRegistration oldDefault = this.appRegistrationRepository
				.findAppRegistrationByNameAndTypeAndDefaultVersionIsTrue(name, type);
		if (oldDefault != null) {
			oldDefault.setDefaultVersion(false);
			this.appRegistrationRepository.save(oldDefault);
		}

		AppRegistration newDefault = this.appRegistrationRepository
				.findAppRegistrationByNameAndTypeAndVersion(name, type, version);

		if (newDefault == null) {
			throw new NoSuchAppRegistrationException(name, type, version);
		}

		newDefault.setDefaultVersion(true);

		this.appRegistrationRepository.save(newDefault);

		this.auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.APP_REGISTRATION,
				AuditActionType.UPDATE, newDefault.getName(),
				this.auditServiceUtils.convertAppRegistrationToAuditData(newDefault));
	}

	@Override
	public List<AppRegistration> findAll() {
		return this.appRegistrationRepository.findAll();
	}

	@Override
	public Page<AppRegistration> findAllByTypeAndNameIsLike(ApplicationType type, String name, Pageable pageable) {
		if (!StringUtils.hasText(name) && type == null) {
			return findAll(pageable);
		}
		else if (StringUtils.hasText(name) && type == null) {
			return this.appRegistrationRepository.findAllByNameContainingIgnoreCase(name, pageable);
		}
		else if (StringUtils.hasText(name)) {
			return this.appRegistrationRepository.findAllByTypeAndNameContainingIgnoreCase(type, name, pageable);
		}
		else {
			return this.appRegistrationRepository.findAllByType(type, pageable);
		}
	}

	@Override
	public Page<AppRegistration> findAll(Pageable pageable) {
		return this.appRegistrationRepository.findAll(pageable);
	}

	@Override
	public AppRegistration save(String name, ApplicationType type, String version, URI uri, URI metadataUri) {
		return this.save(new AppRegistration(name, type, version, uri, metadataUri));
	}

	@Override
	public AppRegistration save(AppRegistration app) {
		AppRegistration createdApp;

		AppRegistration appRegistration = this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(
				app.getName(), app.getType(), app.getVersion());
		if (appRegistration != null) {
			appRegistration.setUri(app.getUri());
			appRegistration.setMetadataUri(app.getMetadataUri());
			createdApp = this.appRegistrationRepository.save(appRegistration);

			populateAuditData(AuditActionType.UPDATE, createdApp);
		}
		else {
			if (getDefaultApp(app.getName(), app.getType()) == null) {
				app.setDefaultVersion(true);
			}
			createdApp = this.appRegistrationRepository.save(app);

			populateAuditData(AuditActionType.CREATE, createdApp);
		}

		return createdApp;
	}

	private void populateAuditData(AuditActionType auditActionType, AppRegistration appRegistration) {
		if (appRegistration == null) {
			logger.error("App registration failed, app not saved into database!");
		}
		else {
			this.auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.APP_REGISTRATION,
					auditActionType, appRegistration.getName(),
					this.auditServiceUtils.convertAppRegistrationToAuditData(appRegistration));
		}
	}

	/**
	 * Deletes an {@link AppRegistration}. If the {@link AppRegistration} does not exist, a
	 * {@link NoSuchAppRegistrationException} will be thrown.
	 *
	 * @param name Name of the AppRegistration to delete
	 * @param type Type of the AppRegistration to delete
	 * @param version Version of the AppRegistration to delete
	 */
	public void delete(String name, ApplicationType type, String version) {
		this.appRegistrationRepository.deleteAppRegistrationByNameAndTypeAndVersion(name, type, version);

		populateAuditData(AuditActionType.DELETE, new AppRegistration(name, type, version, URI.create(""), URI.create("")));
	}

	@Override
	public void deleteAll(Iterable<AppRegistration> appRegistrations) {
		this.appRegistrationRepository.deleteAll(appRegistrations);
	}

	protected boolean isOverwrite(AppRegistration app, boolean overwrite) {
		return overwrite || this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(app.getName(),
				app.getType(), app.getVersion()) == null;
	}

	@Override
	public boolean appExist(String name, ApplicationType type) {
		return getDefaultApp(name, type) != null;
	}

	@Override
	public boolean appExist(String name, ApplicationType type, String version) {
		return find(name, type, version) != null;
	}

	@Override
	public Resource getAppResource(AppRegistration appRegistration) {
		return this.appResourceCommon.getResource(appRegistration.getUri().toString());
	}

	@Override
	public Resource getAppMetadataResource(AppRegistration appRegistration) {
		return this.appResourceCommon.getMetadataResource(appRegistration.getUri(), appRegistration.getMetadataUri());
	}

	@Override
	public String getResourceVersion(Resource resource) {
		return this.appResourceCommon.getResourceVersion(resource);
	}

	@Override
	public String getResourceWithoutVersion(Resource resource) {
		return this.appResourceCommon.getResourceWithoutVersion(resource);
	}

	/**
	 * Returns the version for the given resource URI string.
	 *
	 * @param uriString String representation of the resource URI
	 * @return the resource version
	 */
	@Override
	public String getResourceVersion(String uriString) {
		return this.getResourceVersion(this.appResourceCommon.getResource(uriString));
	}

	private String getVersionOrBroken(String uri) {
		try {
			return this.getResourceVersion(uri);
		}
		catch (IllegalStateException ise) {
			logger.warn("", ise);
			return "broken";
		}
	}

	protected Properties loadProperties(Resource resource) {
		try {
			return PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException e) {
			throw new RuntimeException("Error reading from " + resource.getDescription(), e);
		}
	}

	@Override
	public List<AppRegistration> importAll(boolean overwrite, Resource... resources) {
		return Stream.of(resources)
				.map(this::loadProperties)
				.flatMap(prop -> prop.entrySet().stream()
						.map(toStringAndUriFunc)
						.flatMap(kv -> toValidAppRegistration(kv, metadataUriFromProperties(kv.getKey(), prop)))
						.filter(a -> isOverwrite(a, overwrite))
						.map(ar -> save(ar)))
				.collect(Collectors.toList());
	}

	/**
	 * Builds a {@link Stream} from key/value mapping.
	 * @return
	 * <ul>
	 * <li>valid AppRegistration as single element Stream</li>
	 * <li>silently ignores well malformed metadata entries (0 element Stream) or</li>
	 * <li>fails otherwise.</li>
	 * </ul>
	 *
	 * @param kv key/value representing app key (key) and app URI (value)
	 * @param metadataURI metadataUri computed from a given app key
	 */
	protected Stream<AppRegistration> toValidAppRegistration(Map.Entry<String, URI> kv, URI metadataURI) {
		String key = kv.getKey();
		String[] tokens = key.split("\\.");
		if (tokens.length == 2) {
			String name = tokens[1];
			ApplicationType type = ApplicationType.valueOf(tokens[0]);
			URI appURI = warnOnMalformedURI(key, kv.getValue());

			String version = getVersionOrBroken(appURI.toString());

			return Stream.of(new AppRegistration(name, type, version, appURI, metadataURI));
		}
		else {
			Assert.isTrue(tokens.length == 3 && METADATA_KEY_SUFFIX.equals(tokens[2]),
					"Invalid format for app key '" + key + "'in file. Must be <type>.<name> or <type>.<name>"
							+ ".metadata");
			return Stream.empty();
		}
	}

	protected URI metadataUriFromProperties(String key, Properties properties) {
		String metadataValue = properties.getProperty(key + "." + METADATA_KEY_SUFFIX);
		try {
			return metadataValue != null ? warnOnMalformedURI(key, new URI(metadataValue)) : null;
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	protected URI warnOnMalformedURI(String key, URI uri) {
		if (StringUtils.isEmpty(uri)) {
			logger.warn(String.format("Error when registering '%s': URI is required", key));
		}
		else if (!StringUtils.hasText(uri.getScheme())) {
			logger.warn(
					String.format("Error when registering '%s' with URI %s: URI scheme must be specified", key, uri));
		}
		else if (!StringUtils.hasText(uri.getSchemeSpecificPart())) {
			logger.warn(String.format("Error when registering '%s' with URI %s: URI scheme-specific part must be " +
					"specified", key, uri));
		}
		return uri;
	}

	protected static final Function<Map.Entry<Object, Object>, AbstractMap.SimpleImmutableEntry<String, URI>> toStringAndUriFunc = kv -> {
		try {
			return new AbstractMap.SimpleImmutableEntry<>((String) kv.getKey(), new URI((String) kv.getValue()));
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	};
}
