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

import java.net.Inet4Address;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.stream.module.launcher.ModuleLaunchRequest;
import org.springframework.cloud.stream.module.launcher.ModuleLauncher;
import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;

/**
 * A {@link ModuleDeployer} that will launch a module in-process.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Eric Bottard
 */
public class LocalModuleDeployer implements ModuleDeployer {

	private static final Logger logger = LoggerFactory.getLogger(LocalModuleDeployer.class);

	private final ModuleLauncher launcher;

	private final Map<ModuleDeploymentId, URL> deployedModules = new HashMap<>();

	private final RestTemplate restTemplate = new RestTemplate();

	public LocalModuleDeployer(ModuleLauncher launcher) {
		Assert.notNull(launcher, "Module launcher cannot be null");
		this.launcher = launcher;
	}

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		String module = request.getCoordinates().toString();
		if (request.getCount() != 1) {
			logger.warn("{} only supports a single instance per module; ignoring count={} for {}",
					this.getClass().getSimpleName(), request.getCount(), request.getDefinition().getLabel());
		}
		Map<String, String> args = new HashMap<>();
		args.putAll(request.getDefinition().getParameters());
		args.putAll(request.getDeploymentProperties());

		logger.info("deploying module: {}", module);
		int port;
		if (args.containsKey(SERVER_PORT_KEY)) {
			port = Integer.parseInt(args.get(SERVER_PORT_KEY));
		}
		else {
			port = SocketUtils.findAvailableTcpPort(DEFAULT_SERVER_PORT);
			args.put(SERVER_PORT_KEY, String.valueOf(port));
		}
		URL moduleUrl;
		try {
			moduleUrl = new URL("http", Inet4Address.getLocalHost().getHostAddress(), port, "");
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to determine URL for module: " + module, e);
		}
		args.put("endpoints.shutdown.enabled", "true");
		args.put("spring.main.show_banner", "false");
		args.put(JMX_DEFAULT_DOMAIN_KEY, String.format("%s.%s",
				request.getDefinition().getGroup(), request.getDefinition().getLabel()));
		args.put("endpoints.jmx.unique-names", "true");
		ModuleLaunchRequest moduleLaunchRequest = new ModuleLaunchRequest(module, args);
		launcher.launch(Collections.singletonList(moduleLaunchRequest));
		ModuleDeploymentId id = new ModuleDeploymentId(request.getDefinition().getGroup(),
				request.getDefinition().getLabel());
		this.deployedModules.put(id, moduleUrl);
		return id;
	}

	@Override
	public void undeploy(ModuleDeploymentId id) {
		URL url = this.deployedModules.get(id);
		if (url != null) {
			logger.info("undeploying module: {}", id);
			this.restTemplate.postForObject(url + "/shutdown", null, String.class);
			this.deployedModules.remove(id);
		}
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		boolean deployed = this.deployedModules.containsKey(id);
		LocalModuleInstanceStatus status = new LocalModuleInstanceStatus(id.toString(), deployed, null);
		return ModuleStatus.of(id).with(status).build();
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		Map<ModuleDeploymentId, ModuleStatus> statusMap = new HashMap<>();
		for (ModuleDeploymentId id : this.deployedModules.keySet()) {
			statusMap.put(id, status(id));
		}
		return statusMap;
	}
}
