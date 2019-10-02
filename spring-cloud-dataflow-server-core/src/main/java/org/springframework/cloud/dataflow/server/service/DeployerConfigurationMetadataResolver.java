/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.BeansException;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties.DeployerProperties;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.util.ObjectUtils;

public class DeployerConfigurationMetadataResolver implements ApplicationContextAware {

	private static final String CONFIGURATION_METADATA_PATTERN = "classpath*:/META-INF/spring-configuration-metadata.json";
	private static final String KEY_PREFIX = "spring.cloud.deployer.";
	private ApplicationContext applicationContext;
	private final DeployerProperties deployerProperties;

	public DeployerConfigurationMetadataResolver(DeployerProperties deployerProperties) {
		this.deployerProperties = deployerProperties;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Resolve all configuration metadata properties prefixed with {@code spring.cloud.deployer.}
	 *
	 * @return the list
	 */
	public List<ConfigurationMetadataProperty> resolve() {
		List<ConfigurationMetadataProperty> metadataProperties = new ArrayList<>();
		ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
		try {
			Resource[] resources = applicationContext.getResources(CONFIGURATION_METADATA_PATTERN);
			for (Resource resource : resources) {
				builder.withJsonResource(resource.getInputStream());
			}
		}
		catch (IOException e) {
			throw new SkipperException("Unable to read configuration metadata", e);
		}
		ConfigurationMetadataRepository metadataRepository = builder.build();
		Map<String, ConfigurationMetadataGroup> groups = metadataRepository.getAllGroups();
		// 1. go through all groups and their properties
		// 2. filter 'spring.cloud.deployer.' properties
		// 3. pass in only group includes, empty passes through all
		// 4. pass in group excludes
		// 5. same logic for properties for includes and excludes
		// 6. what's left is white/black listed props
		groups.values().stream()
			.filter(g -> g.getId().startsWith(KEY_PREFIX))
			.filter(groupEmptyOrAnyMatch(deployerProperties.getGroupIncludes()))
			.filter(groupNoneMatch(deployerProperties.getGroupExcludes()))
			.forEach(g -> {
				g.getProperties().values().stream()
					.filter(propertyEmptyOrAnyMatch(deployerProperties.getPropertyIncludes()))
					.filter(propertyNoneMatch(deployerProperties.getPropertyExcludes()))
					.forEach(p -> {
						metadataProperties.add(p);
					});
			});
		return metadataProperties;
	}

	private Predicate<ConfigurationMetadataProperty> propertyEmptyOrAnyMatch(String[] matches) {
		if (ObjectUtils.isEmpty(matches)) {
			return p -> true;
		}
		return p -> Arrays.stream(matches).anyMatch(p.getId()::equals);
	}

	private Predicate<ConfigurationMetadataProperty> propertyNoneMatch(String[] matches) {
		return p -> Arrays.stream(matches).noneMatch(p.getId()::equals);
	}

	private Predicate<ConfigurationMetadataGroup> groupEmptyOrAnyMatch(String[] matches) {
		if (ObjectUtils.isEmpty(matches)) {
			return g -> true;
		}
		return g -> Arrays.stream(matches).anyMatch(g.getId()::equals);
	}

	private Predicate<ConfigurationMetadataGroup> groupNoneMatch(String[] matches) {
		return g -> Arrays.stream(matches).noneMatch(g.getId()::equals);
	}
}
