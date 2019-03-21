/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
		Map<String, String> applicationManifestMap = new HashMap<>();

		if (applicationManifest.getName() != null) {
			applicationManifestMap.put("spec.manifest.name", applicationManifest.getName());
		}
		if (applicationManifest.getBuildpack() != null) {
			applicationManifestMap.put("spec.manifest.buildpack", applicationManifest.getBuildpack());
		}
		if (applicationManifest.getCommand() != null) {
			applicationManifestMap.put("spec.manifest.command", applicationManifest.getCommand());
		}
		if (applicationManifest.getMemory() != null) {
			applicationManifestMap.put("spec.manifest.memory", applicationManifest.getMemory().toString());
		}
		if (applicationManifest.getDisk() != null) {
			applicationManifestMap.put("spec.manifest.disk-quota", applicationManifest.getDisk().toString());
		}
		if (applicationManifest.getTimeout() != null) {
			applicationManifestMap.put("spec.manifest.timeout", applicationManifest.getTimeout().toString());
		}
		if (applicationManifest.getInstances() != null) {
			applicationManifestMap.put("spec.manifest.instances", applicationManifest.getInstances().toString());
		}
		if (applicationManifest.getNoHostname() != null) {
			applicationManifestMap.put("spec.manifest.no-hostname", applicationManifest.getNoHostname().toString());
		}
		if (applicationManifest.getNoRoute() != null) {
			applicationManifestMap.put("spec.manifest.no-route", applicationManifest.getNoRoute().toString());
		}
		if (applicationManifest.getRandomRoute() != null) {
			applicationManifestMap.put("spec.manifest.random-route", applicationManifest.getRandomRoute().toString());
		}
		if (applicationManifest.getHealthCheckType() != null) {
			applicationManifestMap.put("spec.manifest.health-check-type", applicationManifest.getHealthCheckType().toString());
		}
		if (applicationManifest.getHealthCheckHttpEndpoint() != null) {
			applicationManifestMap.put("spec.manifest.health-check-http-endpoint", applicationManifest.getHealthCheckHttpEndpoint());
		}
		if (applicationManifest.getStack() != null) {
			applicationManifestMap.put("spec.manifest.stack", applicationManifest.getStack());
		}
		if (applicationManifest.getServices() != null) {
			int i = 0;
			for (String service : applicationManifest.getServices()) {
				applicationManifestMap.put("spec.manifest.services[" + i++ + "]", service);
			}
		}
		if (applicationManifest.getDomains() != null) {
			int i = 0;
			for (String domain : applicationManifest.getDomains()) {
				applicationManifestMap.put("spec.manifest.domains[" + i++ + "]", domain);
			}
		}
		if (applicationManifest.getHosts() != null) {
			int i = 0;
			for (String host : applicationManifest.getHosts()) {
				applicationManifestMap.put("spec.manifest.hosts[" + i++ + "]", host);
			}
		}
		if (applicationManifest.getEnvironmentVariables() != null) {
			for (Entry<String, Object> entry : applicationManifest.getEnvironmentVariables().entrySet()) {
				applicationManifestMap.put("spec.manifest.env." + entry.getKey(), entry.getValue().toString());
			}
		}

		return applicationManifestMap;
	}

	public static Integer memoryInteger(String text) {
		Integer value = null;
		try {
			value = Integer.parseInt(text);
		}
		catch (Exception e) {
		}
		if (StringUtils.hasText(text)) {
			if (text.endsWith("G")) {
				value = Integer.parseInt(text.substring(0, text.length() - 1)) * GIBI;
			}
			else if (text.endsWith("GB")) {
				value = Integer.parseInt(text.substring(0, text.length() - 2)) * GIBI;
			}
			else if (text.endsWith("M")) {
				value = Integer.parseInt(text.substring(0, text.length() - 1));
			}
			else if (text.endsWith("MB")) {
				value = Integer.parseInt(text.substring(0, text.length() - 2));
			}
		}
		return value;
	}
}
