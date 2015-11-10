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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
class ApplicationModuleDeployer implements ModuleDeployer {

	public static final String MARKER_ENVIRONMENT_VAR_NAME = "SPRING_CLOUD_DATAFLOW_MODULE";

	private final CloudFoundryClient cloudFoundryClient;

	private final Logger logger = LoggerFactory.getLogger(ApplicationModuleDeployer.class);

	private final CloudFoundryModuleDeployerProperties properties;

	public ApplicationModuleDeployer(CloudFoundryModuleDeployerProperties properties) {
		CloudCredentials credentials = new CloudCredentials(properties.getUsername(), properties.getPassword());
		CloudFoundryClient cloudFoundryClient = new CloudFoundryClient(credentials,
				properties.getApiEndpoint(),
				properties.getOrganization(),
				properties.getSpace(),
				properties.isSkipSslValidation());
		cloudFoundryClient.login();

		this.properties = properties;
		this.cloudFoundryClient = cloudFoundryClient;
	}

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {

		final ModuleDeploymentId moduleDeploymentId = ModuleDeploymentId.fromModuleDefinition(request.getDefinition());

		final String appName = this.deduceAppName(moduleDeploymentId);

		final CloudFoundryClient cloudFoundryClient = this.cloudFoundryClient;
		final Logger logger = this.logger;

		Undoer undoer = new Undoer();

		{
			final Staging staging = this.getStagingSettings();
			final int disk = 1024;
			final int memory = 1024;
			final List<String> uris = this.deduceUris(appName);
			final List<String> serviceNames = new ArrayList<>(this.properties.getServices());

			undoer.attempt(new Runnable() {
				@Override
				public void run() {
					logger.debug("Creating app {} using disk[{}], mem[{}]\n\tservices={}, uris={}\n\t{}", appName, disk, memory, serviceNames, uris, staging);
					cloudFoundryClient.createApplication(appName, staging, disk, memory, uris, serviceNames);
				}
			}).andUndoBy(new Runnable() {
				@Override
				public void run() {
					logger.error("Rollback: deleting app {}", appName);
					cloudFoundryClient.deleteApplication(appName);
				}
			});
		}

		{
			final Map<String, String> env = createModuleLauncherEnvironment(request);

			undoer.attempt(new Runnable() {
				@Override
				public void run() {
					logger.trace("Setting env for app {} as {}", appName, env);
					cloudFoundryClient.updateApplicationEnv(appName, env);
				}
			}).withNoParticularUndo();
		}

		{
			final Resource launcher = ApplicationModuleDeployer.this.properties.getModuleLauncherLocation();

			undoer.attempt(new Callable<Void>() {
				@Override
				public Void call() throws IOException {
					cloudFoundryClient.uploadApplication(appName, launcher.getFilename(), launcher.getInputStream(), new LoggingUploadStatusCallback(appName));
					return null;
				}
			}).withNoParticularUndo();
		}

		{
			final int instances = request.getCount();

			if (instances > 1) { // spare a network call if instances == 1
				undoer.attempt(new Runnable() {
					@Override
					public void run() {
						logger.trace("Setting number of instances for {} to {}", appName, instances);
						cloudFoundryClient.updateApplicationInstances(appName, instances);
					}
				}).withNoParticularUndo();
			}
		}

		{
			undoer.attempt(new Runnable() {
				@Override
				public void run() {
					ApplicationModuleDeployer.this.logger.debug("Starting application {}", appName);
					cloudFoundryClient.startApplication(appName);
				}
			}).withNoParticularUndo();
		}

		return moduleDeploymentId;
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		Map<ModuleDeploymentId, ModuleStatus> result = new HashMap<>();
		for (CloudApplication cloudApplication : this.cloudFoundryClient.getApplications()) {
			String moduleMarker = cloudApplication.getEnvAsMap().get(MARKER_ENVIRONMENT_VAR_NAME);
			if (moduleMarker != null) {
				int colon = moduleMarker.indexOf(':');
				String group = moduleMarker.substring(0, colon);
				String label = moduleMarker.substring(colon + 1);
				ModuleDeploymentId id = new ModuleDeploymentId(group, label);
				ModuleStatus status = buildModuleStatus(id, cloudApplication);
				result.put(id, status);
			}
		}
		return result;
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId moduleId) {
		String appName = this.deduceAppName(moduleId);
		try {
			CloudApplication cloudApplication = this.cloudFoundryClient.getApplication(appName);
			return buildModuleStatus(moduleId, cloudApplication);
		}
		catch (CloudFoundryException e) {
			return buildModuleStatus(moduleId, null);
		}
	}

	@Override
	public void undeploy(ModuleDeploymentId moduleId) {
		String appName = this.deduceAppName(moduleId);
		this.logger.debug("Undeploy: requesting deletion of app {}", appName);

		if (getModuleMarker(appName).equals(this.makeModuleMarker(moduleId))) {
			this.cloudFoundryClient.deleteApplication(appName);
		}
		else {
			this.logger.warn("Not destroying app {}, because its environment variable " + MARKER_ENVIRONMENT_VAR_NAME + " did not have the correct value", appName);
		}
	}

	private ModuleStatus buildModuleStatus(ModuleDeploymentId id, CloudApplication cloudApplication) {
		ModuleStatus.Builder statusBuilder = ModuleStatus.of(id);
		String appName = this.deduceAppName(id);
		if (cloudApplication != null) {
			InstancesInfo applicationInstances = this.cloudFoundryClient.getApplicationInstances(cloudApplication);
			if (applicationInstances != null) {
				if (applicationInstances.getInstances() != null) { // null can happen despite the STARTED check above
					List<InstanceStats> instanceStats = new ArrayList<>(this.cloudFoundryClient.getApplicationStats(appName).getRecords());
					List<InstanceInfo> instanceInfos = new ArrayList<>(applicationInstances.getInstances());
					if (instanceStats.size() != instanceInfos.size()) {
						return notRunningInstancesStatus(id, cloudApplication);
					}
					// Use instanceStat.id and instanceInfo.index as synching key
					Collections.sort(instanceStats, new Comparator<InstanceStats>() {
						@Override
						public int compare(InstanceStats o1, InstanceStats o2) {
							return Integer.valueOf(o1.getId()).compareTo(Integer.valueOf(o2.getId()));
						}
					});
					Collections.sort(instanceInfos, new Comparator<InstanceInfo>() {
						@Override
						public int compare(InstanceInfo o1, InstanceInfo o2) {
							return o1.getIndex() - o2.getIndex();
						}
					});

					for (int i = 0; i < instanceStats.size(); i++) {
						// Changes builder by side-effect
						statusBuilder.with(new CloudFoundryModuleInstanceStatus(appName, instanceInfos.get(i), instanceStats.get(i)));
					}
				}
				return statusBuilder.build();
			}
		}
		// Fall through from all if() checks above
		// No running instances, app must be stopped/updating, or even nonexistent
		return notRunningInstancesStatus(id, cloudApplication);
	}

	private Map<String, String> createModuleLauncherEnvironment(ModuleDeploymentRequest request) {
		HashMap<String, String> args = new HashMap<>();
		args.put("modules", request.getCoordinates().toString());
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, request.getDefinition().getParameters()));
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, request.getDeploymentProperties()));
		String jmxDomainName = String.format("%s.%s", request.getDefinition().getGroup(), request.getDefinition().getLabel());
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, Collections.singletonMap(JMX_DEFAULT_DOMAIN_KEY, jmxDomainName)));

		Map<String, String> env = new HashMap<>(toEnvironmentVariables(args));
		env.put(MARKER_ENVIRONMENT_VAR_NAME, this.makeModuleMarker(ModuleDeploymentId.fromModuleDefinition(request.getDefinition())));
		return env;
	}

	private String deduceAppName(ModuleDeploymentId moduleId) {
		return moduleId.getGroup() + "-" + moduleId.getLabel();
	}

	private List<String> deduceUris(String appName) {
		String uriString = appName + "." + this.properties.getDomain();
		return Collections.singletonList(uriString);
	}

	private String getModuleMarker(String appName) {
		Map<String, Object> applicationEnvironments = this.cloudFoundryClient.getApplicationEnvironment(appName);
		if (applicationEnvironments != null) {
			@SuppressWarnings("unchecked") Map<String, String> applicationEnvironment = (Map<String, String>) applicationEnvironments.get("environment_json");
			if (applicationEnvironment != null) {
				String moduleMarker = applicationEnvironment.get(MARKER_ENVIRONMENT_VAR_NAME);
				if (moduleMarker != null) {
					return moduleMarker;
				}
			}
		}
		return "";
	}

	private Staging getStagingSettings() {
		return new Staging(null, properties.getBuildpack(), null, null);
	}

	private String makeModuleMarker(ModuleDeploymentId moduleId) {
		return moduleId.getGroup() + ":" + moduleId.getLabel();
	}

	private ModuleStatus notRunningInstancesStatus(ModuleDeploymentId id, CloudApplication cloudApplication) {
		String appName = this.deduceAppName(id);
		ModuleStatus.Builder statusBuilder = ModuleStatus.of(id);
		if (cloudApplication != null) {
			for (int i = 0; i < cloudApplication.getInstances(); i++) {
				statusBuilder.with(new CloudFoundryModuleInstanceStatus(appName, i));
			}
		}
		return statusBuilder.build();
	}

	private Map<String, String> toEnvironmentVariables(HashMap<String, String> args) {

		Map<String, String> env = new HashMap<>(args.size());
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : args.entrySet()) {
			String oneArg = "--" + entry.getKey() + "=" + entry.getValue();
			sb.append(bashEscape(oneArg)).append(' ');
		}
		String asYaml = new Yaml().dump(Collections.singletonMap("arguments", sb.toString()));

		env.put("JBP_CONFIG_JAVA_MAIN", asYaml);
		return env;
	}

	private static String bashEscape(String original) {
		// Adapted from http://ruby-doc.org/stdlib-1.9.3/libdoc/shellwords/rdoc/Shellwords.html#method-c-shellescape
		return original.replaceAll("([^A-Za-z0-9_\\-.,:\\/@\\n])", "\\\\$1").replaceAll("\n", "'\\\\n'");
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
			ApplicationModuleDeployer.this.logger.debug("Upload of {}: {} (new) files to upload", this.appName, matchedFileNames.size());
		}

		@Override
		public void onProcessMatchedResources(int length) {
			ApplicationModuleDeployer.this.logger.debug("Upload of {}: {} bytes need to be uploaded", this.appName, length);
		}

		@Override
		public boolean onProgress(String status) {
			ApplicationModuleDeployer.this.logger.debug("Upload of {}: {}", this.appName, status);
			return false;
		}
	}
}
