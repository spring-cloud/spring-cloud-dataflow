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

package org.springframework.cloud.dataflow.module.deployer.cloudfoundry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleArgumentQualifier;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.core.io.Resource;

/**
 * A {@link ModuleDeployer} which deploys modules as applications running in a space in CloudFoundry.
 *
 * @author Eric Bottard
 */
public class ApplicationModuleDeployer implements ModuleDeployer {

	private final CloudFoundryModuleDeployerProperties properties;

	private CloudFoundryClient cloudFoundryClient;

	private final Logger logger = LoggerFactory.getLogger(ApplicationModuleDeployer.class);

	public ApplicationModuleDeployer(CloudFoundryModuleDeployerProperties properties) {
		this.properties = properties;
		CloudCredentials credentials = new CloudCredentials(properties.getUsername(), properties.getPassword());
		cloudFoundryClient = new CloudFoundryClient(credentials,
				properties.getApiEndpoint(),
				properties.getOrganization(),
				properties.getSpace());
		cloudFoundryClient.login();
	}

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {

		final String appName = deduceAppName(request);

		String command = null;
		String buildpack = "https://github.com/cloudfoundry/java-buildpack.git#69abec6d2726f73a22339caa6ae7739f060002e4";
		String stack = null;
		Integer healthCheckTimeout = null;

		final Staging staging = new Staging(command, buildpack, stack, healthCheckTimeout);
		final Integer disk = 1024;
		final Integer memory = 1024;
		final List<String> uris = deduceUris(request);
		final List<String> serviceNames = new ArrayList<>(properties.getServices());

		Rollbacker rollbacker = new Rollbacker();

		rollbacker.attempt(new Runnable() {
			@Override
			public void run() {
				logger.debug("Creating app {} using disk[{}], mem[{}]\n\tservices={}, uris={}\n\t{}", appName, disk, memory, serviceNames, uris, staging);
				cloudFoundryClient.createApplication(appName, staging, disk, memory, uris, serviceNames);
			}
		}).andRollbackBy(new Runnable() {
			@Override
			public void run() {
				logger.error("Rollback: deleting app {}", appName);
				cloudFoundryClient.deleteApplication(appName);
			}
		});

		final Map<String, String> env = createModuleLauncherEnvironment(request);
		rollbacker.attempt(new Runnable() {
			@Override
			public void run() {
				logger.trace("Setting env for app {} as {}", appName, env);
				cloudFoundryClient.updateApplicationEnv(appName, env);
			}
		}).withNoParticularRollback();

		rollbacker.attempt(new Callable<Void>() {
			@Override
			public Void call() throws IOException {
				Resource launcher = properties.getModuleLauncherLocation();
				cloudFoundryClient.uploadApplication(appName, launcher.getFilename(), launcher.getInputStream(), new LoggingUploadStatusCallback(appName));
				return null;
			}
		}).withNoParticularRollback();

		final int instances = request.getCount();
		if (instances > 1) { // spare a network call if instances == 1
			rollbacker.attempt(new Runnable() {
				@Override
				public void run() {
					logger.trace("Setting number of instances for {} to {}", appName, instances);
					cloudFoundryClient.updateApplicationInstances(appName, instances);
				}
			}).withNoParticularRollback();
		}

		rollbacker.attempt(new Runnable() {
			@Override
			public void run() {
				logger.debug("Starting application {}", appName);
				cloudFoundryClient.startApplication(appName);
			}
		}).withNoParticularRollback();

		ModuleDefinition definition = request.getDefinition();
		ModuleDeploymentId moduleDeploymentId = new ModuleDeploymentId(definition.getGroup(), definition.getLabel());
		return moduleDeploymentId;
	}

	@Override
	public void undeploy(ModuleDeploymentId moduleId) {
		final String appName = deduceAppName(moduleId);
		logger.debug("Undeploy: requesting deletion of app {}", appName);
		cloudFoundryClient.deleteApplication(appName);
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		Map<ModuleDeploymentId, ModuleStatus> result = new HashMap<>();
		for (CloudApplication cloudApplication : cloudFoundryClient.getApplications()) {
			String moduleMarker = cloudApplication.getEnvAsMap().get("SPRING_CLOUD_DATAFLOW_MODULE");
			if (moduleMarker != null) {
				int colon = moduleMarker.indexOf(':');
				String group = moduleMarker.substring(0, colon);
				String label = moduleMarker.substring(colon + 1);
				ModuleDeploymentId id = new ModuleDeploymentId(group, label);
				ModuleStatus status = buildModuleStatus(id, cloudApplication);
				result.put(id, status);
			}
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId moduleId) {
		String appName = deduceAppName(moduleId);
		try {
			CloudApplication cloudApplication = cloudFoundryClient.getApplication(appName);
			return buildModuleStatus(moduleId, cloudApplication);
		}
		catch (CloudFoundryException e) {
			return buildModuleStatus(moduleId, null);
		}
	}

	private ModuleStatus buildModuleStatus(ModuleDeploymentId id, CloudApplication cloudApplication) {
		ModuleStatus.Builder statusBuilder = ModuleStatus.of(id);
		String appName = deduceAppName(id);
		if (cloudApplication != null && cloudApplication.getState() == CloudApplication.AppState.STARTED) {
			InstancesInfo applicationInstances = cloudFoundryClient.getApplicationInstances(cloudApplication);
			Iterator<InstanceStats> instanceStats = cloudFoundryClient.getApplicationStats(appName).getRecords().iterator();
			for (InstanceInfo instance : applicationInstances.getInstances()) {
				// Changes builder by side-effect
				statusBuilder.with(new CloudFoundryModuleInstanceStatus(appName, instance, instanceStats.next()));
			}
		} // No running instances, app must be stopped/updating, or even inexistent
		else {
			if (cloudApplication != null) {
				for (int i = 0; i < cloudApplication.getInstances(); i++) {
					statusBuilder.with(new CloudFoundryModuleInstanceStatus(appName, i));
				}
			}
		}
		return statusBuilder.build();
	}

	private String deduceAppName(ModuleDeploymentId moduleId) {
		return moduleId.getGroup() + "-" + moduleId.getLabel();
	}

	private String deduceAppName(ModuleDeploymentRequest request) {
		return request.getDefinition().getGroup() + "-" + request.getDefinition().getLabel();
	}

	private List<String> deduceUris(ModuleDeploymentRequest request) {
		String url = deduceAppName(request) + "." + properties.getDomain();
		return Arrays.asList(url);
	}

	private Map<String, String> createModuleLauncherEnvironment(ModuleDeploymentRequest request) {
		HashMap<String, String> args = new HashMap<>();
		args.put("modules", request.getCoordinates().toString());
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, request.getDefinition().getParameters()));
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, request.getDeploymentProperties()));

		Map<String, String> env = new HashMap<>(toEnvironmentVariables(args));
		env.put("SPRING_CLOUD_DATAFLOW_MODULE", request.getDefinition().getGroup() + ":" + request.getDefinition().getLabel());

		return env;
	}

	private Map<String, String> toEnvironmentVariables(HashMap<String, String> args) {

		Map<String, String> env = new HashMap<>(args.size());
//		for (Map.Entry<String, String> entry : args.entrySet()) {
//			env.put(entry.getKey().toUpperCase().replace('.', '_'), entry.getValue());
//		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : args.entrySet()) {
			sb.append("--").append(entry.getKey()).append('=').append(entry.getValue()).append(' ');
		}
		String asYaml = new Yaml().dump(Collections.singletonMap("arguments", sb.toString()));

		env.put("JBP_CONFIG_JAVA_MAIN", asYaml);
		return env;
	}

	/**
	 * Status callback that prints debug information using the outer class logger.
	 *
	 * @author Eric Bottard
	 */
	private class LoggingUploadStatusCallback implements UploadStatusCallback {
		private final String appName;

		public LoggingUploadStatusCallback(String appName) {
			this.appName = appName;
		}

		@Override
		public void onCheckResources() {

		}

		@Override
		public void onMatchedFileNames(Set<String> matchedFileNames) {
			logger.debug("Upload of {}: {} (new) files to upload", appName, matchedFileNames.size());
		}

		@Override
		public void onProcessMatchedResources(int length) {
			logger.debug("Upload of {}: {} bytes need to be uploaded", appName, length);
		}

		@Override
		public boolean onProgress(String status) {
			logger.debug("Upload of {}: {}", appName, status);
			return false;
		}
	}
}
