/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.admin.config;

import static org.springframework.cloud.dataflow.core.ArtifactType.processor;
import static org.springframework.cloud.dataflow.core.ArtifactType.sink;
import static org.springframework.cloud.dataflow.core.ArtifactType.source;
import static org.springframework.cloud.dataflow.core.ArtifactType.task;

import javax.annotation.PostConstruct;

import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistration;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.util.Assert;

/**
 * Populates a {@link ArtifactRegistry} with default modules.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class ArtifactRegistryPopulator {

	/**
	 * Group ID for default stream modules.
	 */
	private static final String DEFAULT_STREAM_GROUP_ID = "org.springframework.cloud.stream.module";

	/**
	 * Group ID for default task modules.
	 */
	private static final String DEFAULT_TASK_GROUP_ID = "org.springframework.cloud.task.module";

	/**
	 * Version number for default modules.
	 */
	private static final String DEFAULT_VERSION = "1.0.0.BUILD-SNAPSHOT";

	/**
	 * Default classifier for default modules.
	 */
	private static final String DEFAULT_CLASSIFIER = "exec";

	/**
	 * Default extension for default modules.
	 */
	private static final String DEFAULT_EXTENSION = "jar";

	/**
	 * The {@link ArtifactRegistry} to populate.
	 */
	private final ArtifactRegistry artifactRegistry;

	/**
	 * Construct a {@code ArtifactRegistryPopulator} with the provided {@link ArtifactRegistry}.
	 *
	 * @param artifactRegistry the {@link ArtifactRegistry} to populate.
	 */
	public ArtifactRegistryPopulator(ArtifactRegistry artifactRegistry) {
		Assert.notNull(artifactRegistry, "ArtifactRegistry must not be null");
		this.artifactRegistry = artifactRegistry;
	}

	/**
	 * Populate the registry with default module coordinates;
	 * will not overwrite existing values.
	 */
	@PostConstruct
	public void populateDefaults() {
		populateDefault("file", source);
		populateDefault("ftp", source);
		populateDefault("http", source);
		populateDefault("sftp", source);
		populateDefault("time", source);
		populateDefault("twitterstream", source);
		populateDefault("filter", processor);
		populateDefault("groovy-filter", processor);
		populateDefault("groovy-transform", processor);
		populateDefault("transform", processor);
		populateDefault("cassandra", sink);
		populateDefault("counter", sink);
		populateDefault("file", sink);
		populateDefault("ftp", sink);
		populateDefault("gemfire", sink);
		populateDefault("hdfs", sink);
		populateDefault("jdbc", sink);
		populateDefault("log", sink);
		populateDefault("redis", sink);
		populateDefault("websocket", sink);
		populateDefault("timestamp", task);
	}

	/**
	 * Populate the registry with default values for the provided
	 * module name and type; will not overwrite existing values.
	 *
	 * @param name module name
	 * @param type module type
	 */
	private void populateDefault(String name, ArtifactType type) {
		if (this.artifactRegistry.find(name, type) == null) {
			this.artifactRegistry.save(new ArtifactRegistration(name, type,
				(type == task) ?
					defaultTaskCoordinatesFor(name + '-' + type) :
					defaultStreamCoordinatesFor(name + '-' + type)));
		}
	}

	/**
	 * Return the default task coordinates for the provided module name.
	 *
	 * @param moduleName module name for which to provide default coordinates
	 * @return default coordinates for the provided module
	 */
	private ArtifactCoordinates defaultTaskCoordinatesFor(String moduleName) {
		return ArtifactCoordinates.parse(String.format("%s:%s:%s:%s:%s",
				DEFAULT_TASK_GROUP_ID, moduleName, DEFAULT_EXTENSION, DEFAULT_CLASSIFIER, DEFAULT_VERSION));
	}


	/**
	 * Return the default stream coordinates for the provided module name.
	 *
	 * @param moduleName module name for which to provide default coordinates
	 * @return default coordinates for the provided module
	 */
	private ArtifactCoordinates defaultStreamCoordinatesFor(String moduleName) {
		return ArtifactCoordinates.parse(String.format("%s:%s:%s:%s:%s",
				DEFAULT_STREAM_GROUP_ID, moduleName, DEFAULT_EXTENSION, DEFAULT_CLASSIFIER, DEFAULT_VERSION));
	}

}
