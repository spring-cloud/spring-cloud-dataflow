/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.service.impl.validation;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.dataflow.configuration.metadata.BootClassLoaderFactory;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.service.ValidationService;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Implementation of Validation Service that delegates to the application registry.
 *
 * @author Glenn Renfo
 * @author Mark Pollack
 */
public class DefaultValidationService implements ValidationService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultValidationService.class);

	/**
	 * The urls and credentials to required to validate access docker resources.
	 */
	private final DockerValidatorProperties dockerValidatorProperties;

	private final AppRegistryService appRegistry;

	public DefaultValidationService(AppRegistryService appRegistry,
			DockerValidatorProperties dockerValidatorProperties) {
		Assert.notNull(dockerValidatorProperties, "DockerValidatorProperties must not be null");
		Assert.notNull(appRegistry, "AppRegistryService must not be null");
		this.dockerValidatorProperties = dockerValidatorProperties;
		this.appRegistry = appRegistry;
	}

	@Override
	public boolean isRegistered(String name, ApplicationType applicationType) {
		return appRegistry.appExist(name, applicationType);
	}

	@Override
	public boolean validate(String name, ApplicationType appType) {
		Assert.hasText(name, "name must not be null or empty");
		Assert.notNull(appType, "ApplicationType must not be null");
		boolean result = false;
		AppRegistration registration = appRegistry.find(name, appType);

		if (registration != null) {
			result = validateResource(appRegistry.getAppResource(registration));

		}
		return result;
	}

	private boolean validateResource(Resource resource) {
		boolean result = false;
		if(resource != null) {
			try {
				if ((resource instanceof DockerResource dockerResource)) {
					result = validateDockerResource(dockerValidatorProperties, dockerResource);
				}
				else {
					new BootClassLoaderFactory(resolveAsArchive(resource), null)
							.createClassLoader();
					result = true;
				}
			}
			catch (Exception ex) {
				logger.info("The app was marked invalid because: ", ex);
			}
		}
		return result;
	}

	private static boolean validateDockerResource(DockerValidatorProperties dockerValidatorProperties,
			DockerResource resource) throws Exception {
		DockerRegistryValidator info = new DockerRegistryValidator(dockerValidatorProperties, resource);
		return info.isImagePresent();
	}

	private static Archive resolveAsArchive(Resource app) throws IOException {
		Assert.notNull(app, "The resource specified for the app must not be null");
		File moduleFile = app.getFile();
		return moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive(moduleFile);
	}

}
