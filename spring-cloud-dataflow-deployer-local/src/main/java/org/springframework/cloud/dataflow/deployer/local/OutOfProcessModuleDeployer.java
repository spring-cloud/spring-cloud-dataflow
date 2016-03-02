/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.deployer.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.DeploymentState;
import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleArgumentQualifier;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * A ModuleDeployer implementation that spins off a new JVM process per module instance.
 * @author Eric Bottard
 * @author Marius Bogoevici
 */
public class OutOfProcessModuleDeployer implements ModuleDeployer {

	private static final String APP_LAUNCHER = "spring-cloud-dataflow-app-launcher";

	private String moduleLauncherPath;

	private Path logPathRoot;

	private static final Log logger = LogFactory.getLog(OutOfProcessModuleDeployer.class);

	private static final Collection<String> ARRAY_PROPERTIES = new HashSet<>(
			Arrays.asList("includes", "excludes"));

	private Map<ModuleDeploymentId, List<Instance>> running = new ConcurrentHashMap<>();

	@Autowired
	private OutOfProcessModuleDeployerProperties properties;

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {

		String module = request.getCoordinates().toString();

		ModuleDeploymentId moduleDeploymentId = ModuleDeploymentId
				.fromModuleDefinition(request.getDefinition());
		List<Instance> processes = new ArrayList<>();
		running.put(moduleDeploymentId, processes);

		boolean useDynamicPort = !request.getDefinition().getParameters()
				.containsKey(SERVER_PORT_KEY);

		HashMap<String, String> args = new HashMap<>();
		args.put("modules", request.getCoordinates().toString());
		@SuppressWarnings("unchecked")
		Map<String, String> custom = mergeProperties(properties.getSharedArgs(),
				request.getDefinition().getParameters(),
				request.getDeploymentProperties());
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, custom));
		String jmxDomainName = String.format("%s.%s", request.getDefinition().getGroup(),
				request.getDefinition().getLabel());
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0,
				Collections.singletonMap(JMX_DEFAULT_DOMAIN_KEY, jmxDomainName)));

		args.put("endpoints.shutdown.enabled", "true");
		args.put("endpoints.jmx.unique-names", "true");
		args.put("module.name", request.getDefinition().getName());
		args.put("module.group", request.getDefinition().getGroup());
		args.put("module.label", request.getDefinition().getLabel());

		try {
			String groupDeploymentId = request.getDeploymentProperties()
					.get(GROUP_DEPLOYMENT_ID);
			if (groupDeploymentId == null) {
				groupDeploymentId = request.getDefinition().getGroup() + "-"
						+ System.currentTimeMillis();
			}
			Path moduleDeploymentGroupDir = Paths
					.get(logPathRoot.toFile().getAbsolutePath(), groupDeploymentId);
			if (!Files.exists(moduleDeploymentGroupDir)) {
				Files.createDirectory(moduleDeploymentGroupDir);
				moduleDeploymentGroupDir.toFile().deleteOnExit();
			}
			Path workDir = Files.createDirectory(
					Paths.get(moduleDeploymentGroupDir.toFile().getAbsolutePath(),
							moduleDeploymentId.toString()));
			if (properties.isDeleteFilesOnExit()) {
				workDir.toFile().deleteOnExit();
			}
			for (int i = 0; i < request.getCount(); i++) {
				int port = useDynamicPort
						? SocketUtils.findAvailableTcpPort(DEFAULT_SERVER_PORT)
						: Integer.parseInt(request.getDefinition().getParameters()
								.get(SERVER_PORT_KEY));
				if (useDynamicPort) {
					args.putAll(ModuleArgumentQualifier.qualifyArgs(0, Collections
							.singletonMap(SERVER_PORT_KEY, String.valueOf(port))));
				}

				String[] command = getJavaCommand();
				ProcessBuilder builder = new ProcessBuilder(command);
				builder.environment().clear();
				builder.environment().putAll(args);

				Instance instance = new Instance(moduleDeploymentId, i, builder, workDir,
						port);
				processes.add(instance);
				if (properties.isDeleteFilesOnExit()) {
					instance.stdout.deleteOnExit();
					instance.stderr.deleteOnExit();
				}
				logger.info("deploying module " + module + " instance " + i
						+ "\n   Logs will be in " + workDir);

			}
		}
		catch (IOException e) {
			throw new RuntimeException("Exception trying to deploy " + request, e);
		}

		return moduleDeploymentId;
	}

	private String[] getJavaCommand() {
		String[] command = new String[properties.getJavaOpts().length + 3];
		command[0] = properties.getJavaCmd();
		for (int j=0; j<properties.getJavaOpts().length; j++) {
			command[j+1] = properties.getJavaOpts()[j];
		}
		command[properties.getJavaOpts().length + 1] = "-jar";
		command[properties.getJavaOpts().length + 2] = moduleLauncherPath;
		return command;
	}

	private Map<String, String> mergeProperties(
			@SuppressWarnings("unchecked") Map<String, String>... properties) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (Map<String, String> item : properties) {
			for (String key : item.keySet()) {
				if (ARRAY_PROPERTIES.contains(key)) {
					String value = result.get(key);
					if (value != null) {
						value = value + ",";
					}
					else {
						value = "";
					}
					value = value + item.get(key);
					result.put(key, value);
				}
				else {
					result.put(key, item.get(key));
				}
			}
		}
		return result;
	}

	@Override
	public void undeploy(ModuleDeploymentId id) {
		List<Instance> processes = running.get(id);
		if (processes != null) {
			for (Instance instance : processes) {
				if (isAlive(instance.process)) {
					shutdownAndWait(instance);
				}
			}
			running.remove(id);
		}
	}

	private void shutdownAndWait(Instance instance) {
		try {
			restTemplate.postForObject(instance.moduleUrl + "/shutdown", null,
					String.class);
			instance.process.waitFor();
		}
		catch (InterruptedException | ResourceAccessException e) {
			instance.process.destroy();
		}
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		List<Instance> instances = running.get(id);
		ModuleStatus.Builder builder = ModuleStatus.of(id);
		if (instances != null) {
			for (Instance instance : instances) {
				builder.with(instance);
			}
		}
		return builder.build();
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		Map<ModuleDeploymentId, ModuleStatus> result = new HashMap<>();
		for (Map.Entry<ModuleDeploymentId, List<Instance>> entry : running.entrySet()) {
			ModuleStatus.Builder builder = ModuleStatus.of(entry.getKey());
			for (Instance instance : entry.getValue()) {
				builder.with(instance);
			}
			result.put(entry.getKey(), builder.build());
		}
		return result;
	}

	/**
	 * Copies the module launcher jar somewhere at startup, so that it can be invoked
	 * later via {@literal java -jar}.
	 */
	@PostConstruct
	public void setup() throws IOException {
		File launcherFile = Files.createTempFile(APP_LAUNCHER, ".jar").toFile();
		launcherFile.deleteOnExit();
		FileCopyUtils.copy(new ClassPathResource(APP_LAUNCHER + ".jar").getInputStream(),
				new FileOutputStream(launcherFile));
		this.moduleLauncherPath = launcherFile.getAbsolutePath();
		this.logPathRoot = Files.createTempDirectory(
				properties.getWorkingDirectoriesRoot(), "spring-cloud-data-flow-");
	}

	@PreDestroy
	public void shutdown() throws Exception {
		for (ModuleDeploymentId moduleDeploymentId : running.keySet()) {
			undeploy(moduleDeploymentId);
		}
	}

	private static class Instance implements ModuleInstanceStatus {

		private final ModuleDeploymentId moduleDeploymentId;

		private final int instanceNumber;

		private final Process process;

		private final File workDir;

		private final File stdout;

		private final File stderr;

		private final URL moduleUrl;

		private Instance(ModuleDeploymentId moduleDeploymentId, int instanceNumber,
				ProcessBuilder builder, Path workDir, int port) throws IOException {
			this.moduleDeploymentId = moduleDeploymentId;
			this.instanceNumber = instanceNumber;
			builder.directory(workDir.toFile());
			String workDirPath = workDir.toFile().getAbsolutePath();
			this.stdout = Files
					.createFile(
							Paths.get(workDirPath, "stdout_" + instanceNumber + ".log"))
					.toFile();
			this.stderr = Files
					.createFile(
							Paths.get(workDirPath, "stderr_" + instanceNumber + ".log"))
					.toFile();
			builder.redirectOutput(this.stdout);
			builder.redirectError(this.stderr);
			builder.environment().put("INSTANCE_INDEX", Integer.toString(instanceNumber));
			this.process = builder.start();
			this.workDir = workDir.toFile();
			moduleUrl = new URL("http", Inet4Address.getLocalHost().getHostAddress(),
					port, "");

		}

		@Override
		public String getId() {
			return moduleDeploymentId + "-" + instanceNumber;
		}

		@Override
		public DeploymentState getState() {
			boolean alive = isAlive(process);
			if (!alive) {
				return DeploymentState.failed;
			}
			try {
				HttpURLConnection urlConnection = (HttpURLConnection) moduleUrl
						.openConnection();
				urlConnection.connect();
				urlConnection.disconnect();
				return DeploymentState.deployed;
			}
			catch (IOException e) {
				return DeploymentState.deploying;
			}
		}

		@Override
		public Map<String, String> getAttributes() {
			HashMap<String, String> result = new HashMap<>();
			result.put("working.dir", workDir.getAbsolutePath());
			result.put("stdout", stdout.getAbsolutePath());
			result.put("stderr", stderr.getAbsolutePath());
			result.put("url", moduleUrl.toString());
			return result;
		}
	}

	// Copy-pasting of JDK8+ isAlive method to retain JDK7 compatibility
	private static boolean isAlive(Process process) {
		try {
			process.exitValue();
			return false;
		}
		catch (IllegalThreadStateException e) {
			return true;
		}
	}
}
