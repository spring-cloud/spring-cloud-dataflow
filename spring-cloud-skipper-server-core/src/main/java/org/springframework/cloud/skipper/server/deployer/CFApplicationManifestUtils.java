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
package org.springframework.cloud.skipper.server.deployer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.jsonwebtoken.lang.Assert;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.ApplicationManifestUtils;
import org.cloudfoundry.operations.applications.Docker;

import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.FileHolder;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

public class CFApplicationManifestUtils {

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

	public static ApplicationManifest updateApplicationName(String cfManifestYamlString, Release release) {
		if (cfManifestYamlString != null) {
			try {
				File manifestYaml = File.createTempFile(release.getName() + new Random().nextLong(), "yml");
				Path manifestYamlPath = manifestYaml.toPath();
				Files.write(manifestYamlPath, cfManifestYamlString.getBytes(), StandardOpenOption.APPEND);
				List<ApplicationManifest> applicationManifestList = ApplicationManifestUtils.read(manifestYamlPath);
				manifestYaml.delete();
				Assert.isTrue(applicationManifestList.size() == 1, "Expected one application manifest entry. "
						+ "Multiple or zero application manifests are not supported yet.");
				ApplicationManifest applicationManifest = applicationManifestList.get(0);
				ApplicationManifest.Builder applicationManifestBuilder = ApplicationManifest.builder()
						.from(applicationManifest).name(getCFApplicationName(release, applicationManifest));
				return applicationManifestBuilder.build();
			}
			catch (IOException e) {
				throw new SkipperException(e.getMessage());
			}
		}
		return null;
	}

	public static ApplicationManifest updateApplicationName(Release release) {
		String cfManifestYamlString = getCFManifestYamlStringFromPackage(release);
		return updateApplicationName(cfManifestYamlString, release);
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

	private static String getCFApplicationName(Release release, ApplicationManifest applicationManifest) {
		if (!applicationManifest.getName().endsWith("-v" + release.getVersion())) {
			return String.format("%s-v%s", applicationManifest.getName(), release.getVersion());
		}
		else {
			return applicationManifest.getName();
		}
	}

	public static String getCFManifestYamlStringFromPackage(Release release) {
		List<FileHolder> fileHolders = release.getPkg().getFileHolders();
		for (FileHolder fileHolder : fileHolders) {
			String fileName = fileHolder.getName();
			if (fileName.endsWith("manifest.yaml") || fileName.endsWith("manifest.yml")) {
				return new String(fileHolder.getBytes());
			}
		}
		return null;
	}
}
