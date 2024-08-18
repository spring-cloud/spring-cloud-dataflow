/*
 * Copyright 2017-2020 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
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
 * @author Corneil du Plessis
 */
@Transactional
public class DefaultAppRegistryService implements AppRegistryService {

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
		AppRegistration registration = this.appRegistrationRepository.findAppRegistrationByNameAndTypeAndVersion(name, type, version);
		logger.debug("find:{}:{}:{}={}", name, type, version, registration);
		return registration;
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
				this.auditServiceUtils.convertAppRegistrationToAuditData(newDefault), null);
	}

	@Override
	public List<AppRegistration> findAll() {
		return this.appRegistrationRepository.findAll();
	}

	@Override
	public Page<AppRegistration> findAllByTypeAndNameIsLike(ApplicationType type, String name, Pageable pageable) {
		Page<AppRegistration> result = null;
		if (!StringUtils.hasText(name) && type == null) {
			result = this.appRegistrationRepository.findAll(pageable);
		}
		else if (StringUtils.hasText(name) && type == null) {
			result = this.appRegistrationRepository.findAllByNameContainingIgnoreCase(name, pageable);
		}
		else if (StringUtils.hasText(name)) {
			result = this.appRegistrationRepository.findAllByTypeAndNameContainingIgnoreCase(type, name, pageable);
		}
		else {
			result = this.appRegistrationRepository.findAllByType(type, pageable);
		}
		return result;
	}

	@Override
	public Page<AppRegistration> findAllByTypeAndNameIsLikeAndDefaultVersionIsTrue(ApplicationType type, String name,
			Pageable pageable) {
		Page<AppRegistration> result = null;
		if (!StringUtils.hasText(name) && type == null) {
			result = this.appRegistrationRepository.findAllByDefaultVersionIsTrue(pageable);
		}
		else if (StringUtils.hasText(name) && type == null) {
			result = this.appRegistrationRepository.findAllByNameContainingIgnoreCaseAndDefaultVersionIsTrue(name,
					pageable);
		}
		else if (StringUtils.hasText(name)) {
			result = this.appRegistrationRepository
					.findAllByTypeAndNameContainingIgnoreCaseAndDefaultVersionIsTrue(type, name, pageable);
		}
		else {
			result = this.appRegistrationRepository.findAllByTypeAndDefaultVersionIsTrue(type, pageable);
		}
		for (AppRegistration pagedAppRegistration : result.getContent()) {
			for (AppRegistration appRegistration : this.findAll()) {
				if (pagedAppRegistration.getName().equals(appRegistration.getName()) &&
						pagedAppRegistration.getType().equals(appRegistration.getType())) {
					if (pagedAppRegistration.getVersions() == null) {
						HashSet<String> versions = new HashSet<>();
						versions.add(appRegistration.getVersion());
						pagedAppRegistration.setVersions(versions);
					}
					else {
						pagedAppRegistration.getVersions().add(appRegistration.getVersion());
					}
				}
			}
		}
		return result;
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
					this.auditServiceUtils.convertAppRegistrationToAuditData(appRegistration), null);
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

		populateAuditData(AuditActionType.DELETE,
				new AppRegistration(name, type, version, URI.create(""), URI.create("")));
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

	@Override
	public Page<AppRegistration> findAllByTypeAndNameIsLikeAndVersionAndDefaultVersion(ApplicationType type,
			String name, String version, boolean defaultVersion, Pageable pageable) {
		return appRegistrationRepository.findAllByTypeAndNameIsLikeAndVersionAndDefaultVersion(type, name, version,
				defaultVersion, pageable);
	}

	protected Properties loadProperties(Resource resource) {
		try {
			return PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException e) {
			throw new RuntimeException("Error reading from " + resource.getDescription(), e);
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

	@Override
	public List<AppRegistration> importAll(boolean overwrite, Resource... resources) {
		List<String[]> lines = Stream.of(resources)
				// take lines
				.flatMap(this::resourceAsLines)
				// take valid splitted lines
				.flatMap(this::splitValidLines).collect(Collectors.toList());
		Map<String, AppRegistration> registrations = new HashMap<>();
		AppRegistration previous = null;
		for(String [] line : lines) {
			previous = createAppRegistrations(registrations, line, previous);
		}
		List<AppRegistration> result = registrations.values()
				.stream()
				// drop registration if it doesn't have main uri as user only had metadata
				.filter(ar -> ar.getUri() != null)
				// filter by overriding, save to repo and collect updated registrations
				.filter(ar -> isOverwrite(ar, overwrite))
				.map(ar -> {
					save(ar);
					return ar;
				}).collect(Collectors.toList());
		return result;
	}

	private AppRegistration createAppRegistrations(Map<String, AppRegistration> registrations, String[] lineSplit, AppRegistration previous) {
		String[] typeName = lineSplit[0].split("\\.");
		if (typeName.length < 2 || typeName.length > 3) {
			throw new IllegalArgumentException("Invalid format for app key '" + lineSplit[0]
					+ "'in file. Must be <type>.<name> or <type>.<name>.metadata or <type>.<name>.bootVersion");
		}
		String type = typeName[0].trim();
		String name = typeName[1].trim();
		String extra = typeName.length == 3 ? typeName[2] : null;
		String version = "bootVersion".equals(extra) ? null : getResourceVersion(lineSplit[1]);
		// This is now versioned key
		String key = type + name + version;
		if (!registrations.containsKey(key) && registrations.containsKey(type + name + "latest")) {
			key = type + name + "latest";
		}
		// Allow bootVersion in descriptor file (already in 5.0.x stream app descriptor)
		if("bootVersion".equals(extra)) {
			if (previous == null) {
				throw new IllegalArgumentException("Expected uri for bootVersion:" + lineSplit[0]);
			}
			ApplicationType appType = ApplicationType.valueOf(type);
			Assert.isTrue(appType == previous.getType() && name.equals(previous.getName()), "Expected previous to be same type and name for:" + lineSplit[0]);
			// Do nothing with bootVersion though
			return previous;
		}
		AppRegistration ar = registrations.getOrDefault(key, new AppRegistration());
		ar.setName(name);
		ar.setType(ApplicationType.valueOf(type));
		ar.setVersion(version);
		if (typeName.length == 2) {
			// normal app uri
			try {
				ar.setUri(new URI(lineSplit[1]));
				warnOnMalformedURI(lineSplit[0], ar.getUri());
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		} else if (typeName.length == 3) {
			if (extra.equals("metadata")) {
				// metadata app uri
				try {
					ar.setMetadataUri(new URI(lineSplit[1]));
					warnOnMalformedURI(lineSplit[0], ar.getMetadataUri());
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}
			} else if (!"bootVersion".equals(extra)) {
				throw new IllegalArgumentException("Invalid property: " + lineSplit[0]);
			}
		}
		registrations.put(key, ar);
		return ar;
	}

	private Stream<String> resourceAsLines(Resource resource) {
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
			return bufferedReader.lines();
		}
		catch (Exception e) {
			throw new RuntimeException("Error reading from " + resource.getDescription(), e);
		}
	}

	private Stream<String[]> splitValidLines(String line) {
		// split to key/value, filter out non valid lines and trim key and value.
		return Stream.of(line)
				.filter(skipCommentLines())
				.map(l -> l.split("="))
				.filter(split -> split.length == 2)
				.map(split -> new String[] { split[0].trim(), split[1].trim() });
	}

	private Predicate<String> skipCommentLines() {
		// skipping obvious lines which we don't even try to parse
		return line -> line != null &&
				StringUtils.hasText(line) &&
				(!line.startsWith("#") || !line.startsWith("/"));
	}
}
