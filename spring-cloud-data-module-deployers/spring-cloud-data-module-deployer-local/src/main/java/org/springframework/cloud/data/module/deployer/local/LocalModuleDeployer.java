/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.module.deployer.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.data.core.ModuleDeploymentId;
import org.springframework.cloud.data.core.ModuleDeploymentRequest;
import org.springframework.cloud.data.module.ModuleStatus;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.cloud.stream.module.launcher.ModuleLauncher;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class LocalModuleDeployer implements ModuleDeployer {

	private static final Logger logger = LoggerFactory.getLogger(LocalModuleDeployer.class);

	private final ModuleLauncher launcher;

	private final Set<ModuleDeploymentId> deployedModules = new HashSet<>();

	public LocalModuleDeployer(ModuleLauncher launcher) {
		Assert.notNull(launcher, "Module launcher cannot be null");
		this.launcher = launcher;
	}

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		String module = request.getCoordinates().toString();
		List<String> args = new ArrayList<>();
		for (Map.Entry<String, String> entry : request.getDefinition().getParameters().entrySet()) {
			args.add(String.format("--%s.%s=%s", module, entry.getKey(), entry.getValue()));
		}
		for (Map.Entry<String, String> entry : request.getDefinition().getBindings().entrySet()) {
			args.add(String.format("--%s.spring.cloud.stream.bindings.%s=%s",
					module, entry.getKey(), entry.getValue()));
		}
		logger.info("deploying module: " + module);
		launcher.launch(new String[] { module }, args.toArray(new String[args.size()]));
		ModuleDeploymentId id = new ModuleDeploymentId(request.getDefinition().getGroup(),
				request.getDefinition().getLabel());
		this.deployedModules.add(id);
		return id;
	}

	@Override
	public void undeploy(ModuleDeploymentId id) {
		throw new UnsupportedOperationException("todo");		
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		boolean deployed = this.deployedModules.contains(id);
		LocalModuleInstanceStatus status = new LocalModuleInstanceStatus(id.toString(), deployed, null);
		return ModuleStatus.of(id).with(status).build();
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		Map<ModuleDeploymentId, ModuleStatus> statusMap = new HashMap<>();
		for (ModuleDeploymentId id : this.deployedModules) {
			statusMap.put(id, status(id));
		}
		return statusMap;
	}
}
