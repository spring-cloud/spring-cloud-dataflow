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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.data.core.ModuleDefinition;
import org.springframework.cloud.data.core.ModuleDeploymentId;
import org.springframework.cloud.data.core.ModuleDeploymentRequest;
import org.springframework.cloud.data.module.deployer.ModuleArgumentQualifier;
import org.springframework.core.io.Resource;

/**
 * Converts between Module information and Application information.
 * Sole owner of correspondence between module ids and application names; also builds environment for the module launcher application.
 *
 * @author Steve Powell
 */
class CloudFoundryModuleDeploymentConverter {

	//TODO: validate the prefixing strategy
	private static final String APPLICATION_PREFIX = "spring_cloud_data_";

	private Resource moduleLauncherResource;

	CloudFoundryModuleDeploymentConverter(Resource moduleLauncherResource) {
		this.moduleLauncherResource = moduleLauncherResource;
	}

	String toApplicationName(ModuleDeploymentId moduleDeploymentId) {
		return APPLICATION_PREFIX + moduleDeploymentId.toString();
	}

	ModuleDeploymentId toModuleDeploymentId(String applicationName) {
		String moduleIdString = moduleIdString(applicationName);
		if (moduleIdString == null) {
			return null;
		}
		try {
			return ModuleDeploymentId.parse(moduleIdString);
		}
		catch (IllegalArgumentException e) {
			return null; // We ignore invalid format cases
		}
	}

	Resource toModuleLauncherResource(ModuleDefinition moduleDefinition) {
		// We use a fixed launcher application
		return this.moduleLauncherResource;
	}

	Map<String, String> toModuleLauncherEnvironment(ModuleDeploymentRequest moduleDeploymentRequest) {
		HashMap<String, String> environment = new HashMap<>();
		environment.put("modules", moduleDeploymentRequest.getCoordinates().toString());
		environment.putAll(ModuleArgumentQualifier.qualifyArgs(0, moduleDeploymentRequest.getDefinition().getParameters()));
		environment.putAll(ModuleArgumentQualifier.qualifyArgs(0, moduleDeploymentRequest.getDeploymentProperties()));
		return toEnvironmentVariables(environment);
	}

	/**
	 * Produce a copy of a {@code Map<String,String>} with the keys converted into environment variable names. Dots are
	 * replaced by underscores, and the alphabetic characters are upper-cased. Any resulting key clashes will result in
	 * entry losses.
	 */
	private static Map<String, String> toEnvironmentVariables(Map<String, String> args) {
		Map<String, String> env = new HashMap<>(args.size());
		for (Map.Entry<String, String> entry : args.entrySet()) {
			env.put(entry.getKey().toUpperCase().replace('.', '_'), entry.getValue());
		}
		return env;
	}

	private static String moduleIdString(String applicationName) {
		if (applicationName.startsWith(APPLICATION_PREFIX)) {
			return applicationName.substring(APPLICATION_PREFIX.length());
		}
		return null;
	}
}
