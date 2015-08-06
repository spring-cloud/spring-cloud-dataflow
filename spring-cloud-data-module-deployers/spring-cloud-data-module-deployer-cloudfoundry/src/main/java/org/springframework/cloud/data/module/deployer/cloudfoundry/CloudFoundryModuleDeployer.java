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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

import java.util.Map;

import org.springframework.cloud.data.core.ModuleDeploymentId;
import org.springframework.cloud.data.core.ModuleDeploymentRequest;
import org.springframework.cloud.data.module.ModuleStatus;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;

public class CloudFoundryModuleDeployer implements ModuleDeployer {

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void undeploy(ModuleDeploymentId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		throw new UnsupportedOperationException();
	}
}
