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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.Docker;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Utility class for Cloud Foundry Application Manifest related operations.
 *
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
public class CloudFoundryApplicationManifestUtils {

	private static final int GIBI = 1_024;

	public static ApplicationManifest updateApplicationPath(ApplicationManifest cfApplicationManifest, Resource application) {
		ApplicationManifest.Builder applicationManifestBuilder = ApplicationManifest.builder()
				.from(cfApplicationManifest);
		try {
			if (application != null && application instanceof DockerResource) {
				String uriString = application.getURI().toString();
				applicationManifestBuilder.docker(
						Docker.builder().image(uriString.replaceFirst("docker:", "")).build())
						.path(null);
			}
			else {
				applicationManifestBuilder.path(application.getFile().toPath());
			}
		}
		catch (IOException e) {
			throw new SkipperException(e.getMessage());
		}
		return applicationManifestBuilder.build();
	}

	public static ApplicationManifest updateApplicationName(Release release) {
		String name = release.getName() + "-v" + release.getVersion();
		ApplicationManifest cfApplicationManifest = ApplicationManifest.builder()
				.name(name)
				.build();
		return cfApplicationManifest;
	}

	public static Map<String, String> getCFManifestMap(ApplicationManifest applicationManifest) {
		String applicationManifestString = applicationManifest.toString();
		applicationManifestString = applicationManifestString.substring(("ApplicationManifest{").length(),
				applicationManifestString.length() - 1);
		List<String> applicationManifestProperties = Arrays.asList(
				StringUtils.commaDelimitedListToStringArray(applicationManifestString));
		Map<String, String> applicationManifestMap = new HashMap<>();
		for (String applicationManifestProperty: applicationManifestProperties) {
			String[] splitString = StringUtils.split(applicationManifestProperty, "=");
			String valueString = splitString[1].replaceAll("\\[", "").replaceAll("\\]", "");
			applicationManifestMap.put(splitString[0], valueString);
		}
		return applicationManifestMap;
	}

	public static Integer memoryInteger(String text) {
		Integer value = null;
		try {
			value = Integer.parseInt(text);
		} catch (Exception e) {
		}
		if (StringUtils.hasText(text)) {
			if (text.endsWith("G")) {
				value = Integer.parseInt(text.substring(0, text.length() - 1)) * GIBI;
			} else if (text.endsWith("GB")) {
				value = Integer.parseInt(text.substring(0, text.length() - 2)) * GIBI;
			} else if (text.endsWith("M")) {
				value = Integer.parseInt(text.substring(0, text.length() - 1));
			} else if (text.endsWith("MB")) {
				value = Integer.parseInt(text.substring(0, text.length() - 2));
			}
		}
		return value;
	}
}
