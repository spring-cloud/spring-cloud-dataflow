/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl.validation;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.dataflow.configuration.metadata.BootClassLoaderFactory;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Offers utilities that validate if the URI of the application are valid.
 *
 * @author Glenn Renfro
 */
public class AppValidationUtils {
	private static Log logger = LogFactory.getLog(AppValidationUtils.class);

	/**
	 * Verifies the attributes that an app in the registry can be resolved to a
	 * a real jar or container.
	 * @param dockerValidatorProperties Properties required to connect to a docker registry.
	 * @param appRegistry Registry containing data for the apps.
	 * @param name The name of the app to validate.
	 * @param appType The type of app to be validated (source, sink, processor or, task)
	 * @return true if the app can be resolve-able.  Else false.
	 */
	public static boolean validateApp(DockerValidatorProperties dockerValidatorProperties,
			AppRegistryCommon appRegistry, String name, ApplicationType appType) {
		Assert.notNull(dockerValidatorProperties, "DockerValidatorProperties must not be null");
		Assert.notNull(appRegistry, "AppRegistryCommon must not be null");
		Assert.hasText(name, "name must not be null or empty");
		Assert.notNull(appType, "ApplicationType must not be null");
		boolean result = false;
		AppRegistration registration = appRegistry.find(name, appType);

		if (registration != null) {
			Resource resource = appRegistry.getAppResource(registration);
			try {
				if ((resource instanceof DockerResource)) {
					result = validateDockerResource(dockerValidatorProperties, (DockerResource) resource);
				}
				else {
					new BootClassLoaderFactory(resolveAsArchive(appRegistry.getAppResource(registration)), null).createClassLoader();
					result = true;
				}
			}
			catch (Exception ex) {
				logger.info(String.format("The app %s was marked invalid because: ", name), ex);
			}
		}
		return result;
	}

	private static boolean validateDockerResource(DockerValidatorProperties dockerValidatorProperties, DockerResource resource) throws Exception {
		DockerRegistryValidator info = new DockerRegistryValidator(dockerValidatorProperties, resource);
		return info.isImagePresent();
	}

	private static Archive resolveAsArchive(Resource app) throws IOException {
		File moduleFile = app.getFile();
		return moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive(moduleFile);
	}
}
