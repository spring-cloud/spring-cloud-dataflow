/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

/**
 * Helper class that allows to resolve properties or env. variables to initialize the static fields
 * required for DockerComposeMachine and DockerComposeExtension configurations.
 *
 * One can use either Java property -Dfoo.bar=bla or the corresponding environment variable FOO_BAR=bla
 * to set the input configuration.
 *
 * @author Christian Tzolov
 */
public class DockerComposeFactoryProperties {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeFactoryProperties.class);

	private static final String PREFIX = "test.docker.compose.";

	/**
	 * Comma separated list of docker-compose yaml files to install. One can use local files names
	 * or resolve files from http:/https:, classpath: or specific file: locations.
	 * Consult the {@link ResourceExtractor} for further information.
	 */
	public static final String TEST_DOCKER_COMPOSE_PATHS = PREFIX + "paths";

	/**
	 * When set to TRUE (default) the Docker Compose configuration will always try to pull the latest docker image
	 * from Docker Hub. Set to FALSE to use locally build docker images instead.
	 */
	public static final String TEST_DOCKER_COMPOSE_PULLONSTARTUP = PREFIX + "pullOnStartup";

	/**
	 * If set it disables creation of the docker compose extension.
	 * Can be used to run the test against existing SCDF/Skipper platform.
	 */
	public static final String TEST_DOCKER_COMPOSE_DISABLE_EXTENSION = PREFIX + "disable.extension";

	/**
	 * Buildapck JVM version used for the DataFlow, Skipper, CRT launcher images.
	 * If empty it defaults to the latest LTS version at the time of release.
	 * The valid values are: <code>-jdk8</code>, <code>-jdk11</code> and <code>-jdk17</code>.
	 */
	public static final String TEST_DOCKER_COMPOSE_BP_JVM_VERSION = PREFIX + "bp.jvm.version.version";

	/**
	 * Change the DataFlow version to be installed. (e.g. 2.4.0.RELEASE or 2.4.1.BUILD-SNAPSHOT ...)
	 */
	public static final String TEST_DOCKER_COMPOSE_DATAFLOW_VERSIONN = PREFIX + "dataflow.version";

	/**
	 * Change the Skipper version to be installed. (e.g. 2.3.0.RELEASE or 2.3.0.BUILD-SNAPSHOT ...)
	 */
	public static final String TEST_DOCKER_COMPOSE_SKIPPER_VERSIONN = PREFIX + "skipper.version";

	/**
	 * The DataFlow Server URI. Defaults to http://dataflow-server:9393
	 */
	public static final String TEST_DOCKER_COMPOSE_DATAFLOW_URI = PREFIX + "dataflow.uri";

	/**
	 * The Skipper Server URI. Defaults to http://skipper-server:7577
	 */
	public static final String TEST_DOCKER_COMPOSE_SKIPPER_URI = PREFIX + "skipper.uri";

	/**
	 * Set the app starters bulk install url.
	 */
	public static final String TEST_DOCKER_COMPOSE_TASK_APPS_URI = PREFIX + "task.apps.uri";

	/**
	 * Set the task apps bulk install url.
	 */
	public static final String TEST_DOCKER_COMPOSE_STREAM_APPS_URI = PREFIX + "stream.apps.uri";

	/**
	 * Overrides the APPS_PORT_RANGE mapped by skipper.
	 */
	public static final String TEST_DOCKER_COMPOSE_APPS_PORT_RANGE = PREFIX + "apps.port.range";

	/**
	 * Shortcut that configures the DooD mode. Automatically sets the env. variables needed for the DooD mode.
	 */
	public static final String TEST_DOCKER_COMPOSE_DOOD = PREFIX + "dood";

	/**
	 * Set to false to keep the exited Docker containers (applicable only in DooD mode).
	 */
	public static final String TEST_DOCKER_COMPOSE_DOCKER_DELETE_CONTAINER_ON_EXIT = PREFIX + "docker.delete.container.on.exit";


	public static final String TEST_DOCKER_COMPOSE_WAITING_FOR_SERVICE_FORMAT = PREFIX + "waiting.for.service.format";

	public static boolean getBoolean(String propertyName, boolean defaultValue) {
		String value = get(propertyName, "" + defaultValue);
		return Boolean.valueOf(value);
	}

	public static String get(String propertyName, String defaultValue) {
		return StringUtils.hasText(getPropertyOrVariableValue(propertyName)) ?
				getPropertyOrVariableValue(propertyName) : defaultValue;
	}

	public static boolean isDockerComposeDisabled() {
		return getBoolean(TEST_DOCKER_COMPOSE_DISABLE_EXTENSION, false);
	}

	/**
	 * Allows overriding the default Docker Compose file paths.
	 * @param defaultPaths Paths to be used when neither test.docker.compose.paths nor TEST_DOCKER_COMPOSE_PATHS are set.
	 * @return Returns an arrays for the Docker Compose file paths to be deployed.
	 */
	public static String[] getDockerComposePaths(String[] defaultPaths) {
		return StringUtils.hasText(getPropertyOrVariableValue(TEST_DOCKER_COMPOSE_PATHS)) ?
				toCSV(getPropertyOrVariableValue(TEST_DOCKER_COMPOSE_PATHS)) : defaultPaths;
	}

	private static String[] toCSV(String txt) {
		return Arrays.stream(txt.split(","))
				.filter(p -> StringUtils.hasText(p))
				.map(p -> p.trim())
				.collect(Collectors.toList())
				.toArray(new String[] {});
	}

	/**
	 * Converts an dot separate property name into upper case, underscore-split environment variable names.
	 * For example test.docker.compose.paths would be converted into TEST_DOCKER_COMPOSE_PATHS.
	 */
	private static String toEnv(String property) {
		return property.trim().toUpperCase(Locale.ROOT).replace(".", "_");
	}

	/**
	 * Look up a value either from a property name or corresponding env. variable.
	 */
	private static String getPropertyOrVariableValue(String propertyName) {
		if (StringUtils.hasText(System.getProperty(propertyName))) {
			return System.getProperty(propertyName);
		}
		if (StringUtils.hasText(System.getenv(toEnv(propertyName)))) {
			return System.getenv(toEnv(propertyName));
		}
		return null;
	}
}
