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

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;

/**
 * A {@link ModuleDeployer} which deploys modules as applications running in a space in CloudFoundry.
 *
 * @author Paul Harris
 * @author Steve Powell
 * @author Eric Bottard
 */
public class ApplicationModuleDeployer implements ModuleDeployer {

	private final CloudFoundryModuleDeploymentConverter cloudFoundryModuleDeploymentConverter;

	private final CloudFoundryApplicationOperations applicationOperations;

	private CloudFoundryModuleDeployerProperties properties;

	public ApplicationModuleDeployer(
			CloudFoundryModuleDeployerProperties properties,
			CloudFoundryModuleDeploymentConverter converter,
			CloudFoundryApplicationOperations applicationOperations) {
		this.properties = properties;
		this.cloudFoundryModuleDeploymentConverter = converter;
		this.applicationOperations = applicationOperations;
	}

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		ModuleDefinition definition = request.getDefinition();
		ModuleDeploymentId moduleDeploymentId = new ModuleDeploymentId(definition.getGroup(), definition.getLabel());
		String applicationName = this.cloudFoundryModuleDeploymentConverter.toApplicationName(moduleDeploymentId);

		PushBindAndStartApplicationResults response = this.applicationOperations.pushBindAndStartApplication(new PushBindAndStartApplicationParameters()
						.withEnvironment(this.cloudFoundryModuleDeploymentConverter.toModuleLauncherEnvironment(request))
						.withInstances(request.getCount())
						.withName(applicationName)
						.withResource(properties.getModuleLauncherLocation())
						.withServiceInstanceNames(this.properties.getServices())
		);
		if (!response.isCreateSucceeded()) {
			throw new IllegalStateException("Module " + moduleDeploymentId + " could not be deployed");
		}
		return moduleDeploymentId;
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		GetApplicationsStatusResults response = this.applicationOperations.getApplicationsStatus(
				new GetApplicationsStatusParameters());

		Map<ModuleDeploymentId, ModuleStatus> result = new HashMap<>();
		for (Map.Entry<String, ApplicationStatus> e : response.getApplications().entrySet()) {
			ModuleDeploymentId moduleId = this.cloudFoundryModuleDeploymentConverter.toModuleDeploymentId(e.getKey());
			if (null != moduleId) { // filter out non-modules
				result.put(moduleId,
						new ModuleStatusBuilder().withId(moduleId).withApplicationStatus(e.getValue()).build());
			}
		}
		return result;
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId moduleId) {
		String applicationName = this.cloudFoundryModuleDeploymentConverter.toApplicationName(moduleId);

		GetApplicationsStatusResults response = this.applicationOperations.getApplicationsStatus(
				new GetApplicationsStatusParameters().withName(applicationName));

		return new ModuleStatusBuilder().withId(moduleId).withApplicationStatus(response.getApplications().get(applicationName)).build();
	}

	@Override
	public void undeploy(ModuleDeploymentId moduleId) {
		DeleteApplicationResults response = this.applicationOperations.deleteApplication(
				new DeleteApplicationParameters()
						.withName(this.cloudFoundryModuleDeploymentConverter.toApplicationName(moduleId)));
		if (!response.isFound()) {
			throw new IllegalStateException("Module " + moduleId + " is not deployed");
		}
	}
}
