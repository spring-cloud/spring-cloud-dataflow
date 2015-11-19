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

package org.springframework.cloud.dataflow.admin.spi.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleArgumentQualifier;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;

/**
 * A ModuleDeployer implementation that spins off a new JVM process per
 * module instance.
 *
 * @author Eric Bottard
 */
public class OutOfProcessModuleDeployer implements ModuleDeployer {

	private String moduleLauncherPath;

	private static final Logger logger = LoggerFactory.getLogger(OutOfProcessModuleDeployer.class);

	private Map<ModuleDeploymentId, List<Instance>> running = new HashMap<>();

	@Autowired
	private OutOfProcessModuleDeployerProperties properties;

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {

		String module = request.getCoordinates().toString();

		ModuleDeploymentId moduleDeploymentId = ModuleDeploymentId.fromModuleDefinition(request.getDefinition());
		List<Instance> processes = new ArrayList<>();
		running.put(moduleDeploymentId, processes);


		HashMap<String, String> args = new HashMap<>();
		args.put("modules", request.getCoordinates().toString());
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, request.getDefinition().getParameters()));
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, request.getDeploymentProperties()));
		String jmxDomainName = String.format("%s.%s", request.getDefinition().getGroup(), request.getDefinition().getLabel());
		args.putAll(ModuleArgumentQualifier.qualifyArgs(0, Collections.singletonMap(JMX_DEFAULT_DOMAIN_KEY, jmxDomainName)));

		args.put("endpoints.shutdown.enabled", "true");
		args.put("endpoints.jmx.unique-names", "true");


		for (int i = 0; i < request.getCount(); i++) {
			int port = SocketUtils.findAvailableTcpPort(DEFAULT_SERVER_PORT);
			args.put(SERVER_PORT_KEY, String.valueOf(port));

			ProcessBuilder builder = new ProcessBuilder(properties.getJavaCmd(), "-jar", moduleLauncherPath);
			builder.environment().clear();
			builder.environment().putAll(args);

			try {
				Path workDir = Files.createTempDirectory(properties.getWorkingDirectoriesRoot(), moduleDeploymentId.toString() + "-" + i + "-");
				if (properties.isDeleteFilesOnExit()) {
					workDir.toFile().deleteOnExit();
				}

				processes.add(new Instance(moduleDeploymentId, i, builder, workDir));
				logger.info("deploying module {} instance {}\n   Logs will be in {}", module, i, workDir);
			}
			catch (IOException e) {
				throw new RuntimeException("Exception trying to deploy " + request, e);
			}

		}


		return moduleDeploymentId;
	}

	@Override
	public void undeploy(ModuleDeploymentId id) {
		List<Instance> processes = running.get(id);
		if (processes != null) {
			RestTemplate restTemplate = new RestTemplate();
			for (Instance instance : processes) {
				if (!isAlive(instance.process)) {
					continue;
				}
				try {
					restTemplate.postForObject(instance.moduleUrl + "/shutdown", null, String.class);
					instance.process.waitFor();
				}
				catch (InterruptedException e) {
					instance.process.destroy();
				}
			}
			running.remove(id);
		}
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		List<Instance> instances = running.get(id);
		if (instances == null) {
			return null;
		}
		ModuleStatus.Builder builder = ModuleStatus.of(id);
		for (Instance instance : instances) {
			builder.with(instance);
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
	 * Copies the module launcher jar somewhere at startup, so that it can be invoked later
	 * via {@literal java -jar}.
	 */
	@PostConstruct
	public void setup() throws IOException {
		File launcherFile = Files.createTempFile("spring-cloud-stream-module-launcher", ".jar").toFile();
		launcherFile.deleteOnExit();
		FileCopyUtils.copy(new ClassPathResource("spring-cloud-stream-module-launcher.jar").getInputStream(), new FileOutputStream(launcherFile));
		this.moduleLauncherPath = launcherFile.getAbsolutePath();
	}

	private static class Instance implements ModuleInstanceStatus {
		private final ModuleDeploymentId moduleDeploymentId;

		private final int instanceNumber;

		private final Process process;

		private final File workDir;

		private final File stdout;

		private final File stderr;

		private final URL moduleUrl;

		private Instance(ModuleDeploymentId moduleDeploymentId, int instanceNumber, ProcessBuilder builder, Path workDir) throws IOException {
			this.moduleDeploymentId = moduleDeploymentId;
			this.instanceNumber = instanceNumber;
			builder.directory(workDir.toFile());
			builder.redirectOutput(this.stdout = Files.createTempFile(workDir, "stdout_", ".log").toFile());
			builder.redirectError(this.stderr = Files.createTempFile(workDir, "stderr_", ".log").toFile());
			builder.environment().put("INSTANCE_INDEX", Integer.toString(instanceNumber));
			this.process = builder.start();
			this.workDir = workDir.toFile();
			int port = Integer.parseInt(builder.environment().get("server.port"));
			moduleUrl = new URL("http", Inet4Address.getLocalHost().getHostAddress(), port, "");

		}

		@Override
		public String getId() {
			return moduleDeploymentId + "-" + instanceNumber;
		}

		@Override
		public ModuleStatus.State getState() {
			return isAlive(process) ? ModuleStatus.State.deployed : ModuleStatus.State.failed;
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

	// Copy-pasting of JDK8+ isAlive method to retain JDK7 compat
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