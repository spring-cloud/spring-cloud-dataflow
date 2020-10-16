/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.Deprecation.Level;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationConfigurationMetadataResolver} that knows how to look either
 * inside Spring Boot uber-jars or an application Container Image's configuration labels.
 *
 * <p>
 * Supports Boot 1.3 and 1.4+ layouts thanks to a pluggable BootClassLoaderCreation
 * strategy.
 * <p>
 * Supports Docker and OCI image format for retrieving the metadata.
 *
 * @author Eric Bottard
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 */
public class BootApplicationConfigurationMetadataResolver extends ApplicationConfigurationMetadataResolver {

	private static final Logger logger = LoggerFactory.getLogger(BootApplicationConfigurationMetadataResolver.class);

	private static final String CONFIGURATION_METADATA_PATTERN = "classpath*:/META-INF/spring-configuration-metadata.json";

	// this is superseded by name prefixed with dataflow and will get removed in future
	private static final String DEPRECATED_SPRING_CONFIGURATION_PROPERTIES = "classpath*:/META-INF/spring-configuration-metadata-whitelist.properties";

	// this is superseded by VISIBLE_PROPERTIES
	private static final String DEPRECATED_DATAFLOW_CONFIGURATION_PROPERTIES = "classpath*:/META-INF/dataflow-configuration-metadata-whitelist.properties";

	private static final String VISIBLE_PROPERTIES = "classpath*:/META-INF/dataflow-configuration-metadata.properties";

	private static final String CONFIGURATION_PROPERTIES_CLASSES = "configuration-properties.classes";

	private static final String CONFIGURATION_PROPERTIES_NAMES = "configuration-properties.names";

	private static final String CONFIGURATION_PROPERTIES_INBOUND_PORTS = "configuration-properties.inbound-ports";

	private static final String CONFIGURATION_PROPERTIES_OUTBOUND_PORTS = "configuration-properties.outbound-ports";

	private static final String CONTAINER_IMAGE_CONFIGURATION_METADATA_LABEL_NAME = "org.springframework.cloud.dataflow.spring-configuration-metadata.json";

	private final Set<String> globalVisibleProperties = new HashSet<>();

	private final Set<String> globalVisibleClasses = new HashSet<>();

	private final ClassLoader parent;

	private ContainerImageMetadataResolver containerImageMetadataResolver;

	public BootApplicationConfigurationMetadataResolver(ContainerImageMetadataResolver containerImageMetadataResolver) {
		this(null, containerImageMetadataResolver);
	}

	public BootApplicationConfigurationMetadataResolver(ClassLoader parent,
			ContainerImageMetadataResolver containerImageMetadataResolver) {
		this.parent = parent;
		this.containerImageMetadataResolver = containerImageMetadataResolver;
		JarFile.registerUrlProtocolHandler();
		try {
			loadVisible(
					visibleConfigurationMetadataResources(
							ApplicationConfigurationMetadataResolver.class.getClassLoader()),
					this.globalVisibleClasses,
					this.globalVisibleProperties);
		}
		catch (IOException e) {
			throw new RuntimeException("Error reading global list of visible configuration properties", e);
		}
	}

	private static Resource[] visibleConfigurationMetadataResources(ClassLoader classLoader) throws IOException {
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver(classLoader);
		Resource[] configurationResources = resourcePatternResolver.getResources(VISIBLE_PROPERTIES);

		Resource[] deprecatedSpringConfigurationResources = resourcePatternResolver
				.getResources(DEPRECATED_SPRING_CONFIGURATION_PROPERTIES);
		if (deprecatedSpringConfigurationResources.length > 0) {
			logger.warn("The use of " + DEPRECATED_SPRING_CONFIGURATION_PROPERTIES + " is a deprecated. Please use "
					+ VISIBLE_PROPERTIES + " instead.");
		}
		Resource[] deprecatedDataflowConfigurationResources = resourcePatternResolver
				.getResources(DEPRECATED_DATAFLOW_CONFIGURATION_PROPERTIES);
		if (deprecatedDataflowConfigurationResources.length > 0) {
			logger.warn("The use of " + DEPRECATED_DATAFLOW_CONFIGURATION_PROPERTIES
					+ " is a deprecated. Please use " + VISIBLE_PROPERTIES + " instead.");
		}

		return concatArrays(configurationResources, deprecatedSpringConfigurationResources,
				deprecatedDataflowConfigurationResources);

	}

	private static Resource[] concatArrays(final Resource[]... arrays) {
		return Arrays.stream(arrays).flatMap(Arrays::stream).toArray(Resource[]::new);
	}

	/**
	 * Return metadata about configuration properties that are documented via <a href=
	 * "https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html">
	 * Spring Boot configuration metadata</a> and visible in an app.
	 *
	 * @param app a Spring Cloud Stream app; typically a Boot uberjar, but directories are
	 *     supported as well
	 */
	@Override
	public List<ConfigurationMetadataProperty> listProperties(Resource app, boolean exhaustive) {
		try {
			if (app != null) {
				if (isDockerSchema(app.getURI())) {
					return resolvePropertiesFromContainerImage(app.getURI());
				}
				else {
					Archive archive = resolveAsArchive(app);
					return listProperties(archive, exhaustive);
				}
			}
		}
		catch (Exception e) {
			logger.warn("Failed to retrieve properties for resource {} because of {}",
					app, ExceptionUtils.getRootCauseMessage(e));
			if (logger.isDebugEnabled()) {
				logger.debug("(Details) for failed to retrieve properties for resource:" + app, e);
			}
			return Collections.emptyList();
		}

		return Collections.emptyList();
	}

	@Override
	public Map<String, Set<String>> listPortNames(Resource app) {
		try {
			if (app != null) {
				if (isDockerSchema(app.getURI())) {
					return resolvePortNamesFromContainerImage(app.getURI());
				}
				else {
					Archive archive = resolveAsArchive(app);
					return listPortNames(archive);
				}
			}
		}
		catch (Exception e) {
			logger.warn("Failed to retrieve port names for resource {} because of {}",
					app, ExceptionUtils.getRootCauseMessage(e));
			if (logger.isDebugEnabled()) {
				logger.debug("(Details) for failed to retrieve port names for resource:" + app, e);
			}
			return Collections.emptyMap();
		}

		return Collections.emptyMap();
	}

	private boolean isDockerSchema(URI uri) {
		return uri != null && uri.getScheme() != null && uri.getScheme().contains("docker");
	}

	private List<ConfigurationMetadataProperty> resolvePropertiesFromContainerImage(URI imageUri) {
		String imageName = imageUri.getSchemeSpecificPart();

		Map<String, String> labels = this.containerImageMetadataResolver.getImageLabels(imageName);
		if (CollectionUtils.isEmpty(labels)) {
			return Collections.emptyList();
		}

		String encodedMetadata = labels.get(CONTAINER_IMAGE_CONFIGURATION_METADATA_LABEL_NAME);
		if (!StringUtils.hasText(encodedMetadata)) {
			return Collections.emptyList();
		}

		try {
			ConfigurationMetadataRepository configurationMetadataRepository = ConfigurationMetadataRepositoryJsonBuilder
					.create().withJsonResource(new ByteArrayInputStream(encodedMetadata.getBytes()))
					.build();

			List<ConfigurationMetadataProperty> result = configurationMetadataRepository.getAllProperties().entrySet()
					.stream()
					.map(e -> e.getValue())
					.collect(Collectors.toList());
			return result;
		}
		catch (Exception e) {
			throw new AppMetadataResolutionException("Invalid Metadata for " + imageName);
		}
	}

	private Map<String, Set<String>> resolvePortNamesFromContainerImage(URI imageUri) {
		String imageName = imageUri.getSchemeSpecificPart();
		Map<String, Set<String>> portsMap = new HashMap<>();
		Map<String, String> labels = this.containerImageMetadataResolver.getImageLabels(imageName);
		if (CollectionUtils.isEmpty(labels)) {
			return Collections.emptyMap();
		}
		String inboundPortMapping = labels.get(CONFIGURATION_PROPERTIES_INBOUND_PORTS);
		if (StringUtils.hasText(inboundPortMapping)) {
			Set<String> inboundPorts = new HashSet<>();
			inboundPorts.addAll(Arrays.asList(StringUtils
					.delimitedListToStringArray(inboundPortMapping, ",", " ")));
			portsMap.put("inbound", inboundPorts);
		}
		String outboundPortMapping = labels.get(CONFIGURATION_PROPERTIES_OUTBOUND_PORTS);
		if (StringUtils.hasText(outboundPortMapping)) {
			Set<String> outboundPorts = new HashSet<>();
			outboundPorts.addAll(Arrays.asList(StringUtils
					.delimitedListToStringArray(outboundPortMapping, ",", " ")));
			portsMap.put("outbound", outboundPorts);
		}
		return portsMap;
	}

	public List<ConfigurationMetadataProperty> listProperties(Archive archive, boolean exhaustive) {
		try (URLClassLoader moduleClassLoader = new BootClassLoaderFactory(archive, parent).createClassLoader()) {
			List<ConfigurationMetadataProperty> result = new ArrayList<>();
			ResourcePatternResolver moduleResourceLoader = new PathMatchingResourcePatternResolver(moduleClassLoader);
			Collection<String> visibleClasses = new HashSet<>(this.globalVisibleClasses);
			Collection<String> visibleProperties = new HashSet<>(this.globalVisibleProperties);

			loadVisible(visibleConfigurationMetadataResources(moduleClassLoader), visibleClasses,
					visibleProperties);

			ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
			for (Resource r : moduleResourceLoader.getResources(CONFIGURATION_METADATA_PATTERN)) {
				builder.withJsonResource(r.getInputStream());
			}
			for (ConfigurationMetadataGroup group : builder.build().getAllGroups().values()) {
				if (exhaustive || isVisible(group, visibleClasses)) {
					for (ConfigurationMetadataProperty property : group.getProperties().values()) {
						if (!isDeprecatedError(property)) {
							result.add(property);
						}
					}

				} // Props in the root group have an id that looks prefixed itself. Handle
				// here
				else if ("_ROOT_GROUP_".equals(group.getId())) {
					for (ConfigurationMetadataProperty property : group.getProperties().values()) {
						if (isVisible(property, visibleProperties)) {
							if (!isDeprecatedError(property)) {
								result.add(property);
							}
						}
					}
				}
				else { // Look for per property WL
					for (ConfigurationMetadataProperty property : group.getProperties().values()) {
						if (isVisible(property, visibleProperties)) {
							if (!isDeprecatedError(property)) {
								result.add(property);
							}
						}
					}
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new RuntimeException("Exception trying to list configuration properties for application " + archive,
					e);
		}
	}

	private Map<String, Set<String>> listPortNames(Archive archive) {
		try (URLClassLoader moduleClassLoader = new BootClassLoaderFactory(archive, parent).createClassLoader()) {
			Set<String> inboundPorts = new HashSet<>();
			Set<String> outboundPorts = new HashSet<>();
			Map<String, Set<String>> portsMap = new HashMap<>();

			for (Resource resource : visibleConfigurationMetadataResources(moduleClassLoader)) {
				Properties properties = new Properties();
				properties.load(resource.getInputStream());
				inboundPorts.addAll(Arrays.asList(StringUtils
						.delimitedListToStringArray(properties.getProperty(CONFIGURATION_PROPERTIES_INBOUND_PORTS), ",",
								" ")));
				portsMap.put("inbound", inboundPorts);
				outboundPorts.addAll(Arrays.asList(StringUtils
						.delimitedListToStringArray(properties.getProperty(CONFIGURATION_PROPERTIES_OUTBOUND_PORTS),
								",", " ")));
				portsMap.put("outbound", outboundPorts);
			}
			return portsMap;
		}
		catch (Exception e) {
			throw new AppMetadataResolutionException(
					"Exception trying to list configuration properties for application " + archive,
					e);
		}
	}

	@Override
	public URLClassLoader createAppClassLoader(Resource app) {
		try {
			return new BootClassLoaderFactory(resolveAsArchive(app), parent).createClassLoader();
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to resolve application resource: " + app.getDescription(), e);
		}
	}

	private Archive resolveAsArchive(Resource app) throws IOException {
		File moduleFile = app.getFile();
		return moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive(moduleFile);
	}

	/**
	 * Loads visible properties and group classes and add them to the given collections.
	 */
	private void loadVisible(Resource[] resources, Collection<String> classes, Collection<String> names)
			throws IOException {
		for (Resource resource : resources) {
			Properties properties = new Properties();
			properties.load(resource.getInputStream());
			classes.addAll(Arrays.asList(StringUtils
					.delimitedListToStringArray(properties.getProperty(CONFIGURATION_PROPERTIES_CLASSES), ",", " ")));
			names.addAll(Arrays.asList(StringUtils
					.delimitedListToStringArray(properties.getProperty(CONFIGURATION_PROPERTIES_NAMES), ",", " ")));
		}
	}

	/**
	 * Return whether a single property has been listed as being a "main" configuration
	 * property.
	 */
	private boolean isVisible(ConfigurationMetadataProperty property, Collection<String> properties) {
		return properties.contains(property.getId());
	}

	/**
	 * Checks if property is error deprecated.
	 *
	 * @param property the configuration property
	 * @return if property is error deprecated
	 */
	private boolean isDeprecatedError(ConfigurationMetadataProperty property) {
		return property.getDeprecation() != null && property.getDeprecation().getLevel() == Level.ERROR;
	}

	/**
	 * Return whether a configuration property group (class) has been listed as being a "main"
	 * group.
	 */
	private boolean isVisible(ConfigurationMetadataGroup group, Collection<String> classes) {
		Set<String> sourceTypes = group.getSources().keySet();
		return !sourceTypes.isEmpty() && classes.containsAll(sourceTypes);
	}

}
