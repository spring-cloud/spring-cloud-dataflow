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

package org.springframework.cloud.data.module.deployer.lattice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.receptor.actions.RunAction;
import org.cloudfoundry.receptor.client.ReceptorClient;
import org.cloudfoundry.receptor.commands.TaskCreateRequest;
import org.cloudfoundry.receptor.commands.TaskResponse;
import org.cloudfoundry.receptor.support.EnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.data.core.ModuleDeploymentId;
import org.springframework.cloud.data.core.ModuleDeploymentRequest;
import org.springframework.cloud.data.module.ModuleStatus;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;

/**
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Michael Minella
 */
public class TaskModuleDeployer implements ModuleDeployer {

	private static final Logger logger = LoggerFactory.getLogger(TaskModuleDeployer.class);

	public static final String DOCKER_PATH = "docker:///springcloud/stream-module-launcher";

	private final ReceptorClient receptorClient = new ReceptorClient();

	private final StatusMapper statusMapper = new StatusMapper();

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		ModuleDeploymentId id =
				ModuleDeploymentId.fromModuleDefinition(request.getDefinition());
		String guid = guid(id);

		TaskCreateRequest task = new TaskCreateRequest();
		task.setLogGuid(guid);
		task.setTaskGuid(guid);
		task.setRootfs(DOCKER_PATH);

		RunAction runAction = task.getAction().get("run");
		runAction.setPath("java");
		runAction.addArg("-Djava.security.egd=file:/dev/./urandom");
		runAction.addArg("-jar");
		runAction.addArg("/module-launcher.jar");

		List<EnvironmentVariable> environmentVariables = new ArrayList<EnvironmentVariable>();
		Collections.addAll(environmentVariables, task.getEnv());
		environmentVariables.add(new EnvironmentVariable("MODULES", request.getCoordinates().toString()));
		environmentVariables.add(new EnvironmentVariable("SPRING_PROFILES_ACTIVE", "cloud"));
		for (Map.Entry<String, String> entry : request.getDefinition().getParameters().entrySet()) {
			environmentVariables.add(new EnvironmentVariable(entry.getKey(), entry.getValue()));
		}

		task.setEnv(environmentVariables.toArray(new EnvironmentVariable[environmentVariables.size()]));
		task.setMemoryMb(512);

		logger.debug("Desired Task: {}", task);
		for (EnvironmentVariable e : environmentVariables) {
			logger.debug("{}={}", e.getName(), e.getValue());
		}

		receptorClient.createTask(task);
		return id;
	}

	/**
	 * Create a Diego process guid for the given {@link ModuleDeploymentId}.
	 *
	 * @param id the module deployment id
	 * @return string containing a Diego process guid
	 */
	// todo: this should be encapsulated in an interface implemented by each SPI implementation
	private String guid(ModuleDeploymentId id) {
		return id.toString().replace(".", "_");
	}

	@Override
	public void undeploy(ModuleDeploymentId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		ModuleStatus.Builder builder = ModuleStatus.of(id);

		// todo: if the actual Task is not found, search for the desired Task to verify
		// that the Task is known to Lattice
		TaskResponse task = receptorClient.getTask(guid(id));

		if(task != null) {
			Map<String, String> attributes = new HashMap<>();
			attributes.put("failureReason", task.getFailureReason());
			attributes.put("result", task.getResult());
			attributes.put("annotation", task.getAnnotation());
			attributes.put("completionCallbackUrl", task.getCompletionCallbackUrl());
			attributes.put("resultFile", task.getResultFile());
			attributes.put("cellId", task.getCellId());
			attributes.put("domain", task.getDomain());
			builder.with(new ReceptorModuleInstanceStatus(task.getTaskGuid(), statusMapper.map(task), attributes));

			return builder.build();
		}
		else {
			return null;
		}
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		throw new UnsupportedOperationException();
	}

	private static class StatusMapper {

		public ModuleStatus.State map(TaskResponse taskResponse) {
			ModuleStatus.State state;

			switch (taskResponse.getState()) {
				case "PENDING":
					state = ModuleStatus.State.deploying;
					break;
				case "CLAIMED":
					state = ModuleStatus.State.deploying;
					break;
				case "RUNNING":
					//TODO: Add support for canceling
					state = ModuleStatus.State.deployed;
					break;
				case "COMPLETED":
					//TODO: Add support for canceled
					if(taskResponse.isFailed()) {
						state = ModuleStatus.State.failed;
					}
					else {
						state = ModuleStatus.State.complete;
					}
					break;
				default:
					state = ModuleStatus.State.unknown;
			}

			return state;
		}
	}

}
