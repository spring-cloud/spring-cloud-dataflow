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

package org.springframework.cloud.data.module.deployer.lattice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.receptor.client.ReceptorClient;
import org.cloudfoundry.receptor.commands.ActualLRPResponse;
import org.cloudfoundry.receptor.commands.DesiredLRPCreateRequest;
import org.cloudfoundry.receptor.support.EnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.data.core.ModuleDeploymentId;
import org.springframework.cloud.data.core.ModuleDeploymentRequest;
import org.springframework.cloud.data.module.ModuleStatus;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.util.StringUtils;

/**
 * @author Patrick Peralta
 */
public class ReceptorModuleDeployer implements ModuleDeployer {
	private static final Logger logger = LoggerFactory.getLogger(ReceptorModuleDeployer.class);

	public static final String DOCKER_PATH = "docker:///springcloud/stream-module-launcher";

	public static final String BASE_ADDRESS = "192.168.11.11.xip.io";

	private final ReceptorClient receptorClient = new ReceptorClient();

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		ModuleDeploymentId id = ModuleDeploymentId.fromModuleDefinition(request.getDefinition());
		String guid = guid(id);

		DesiredLRPCreateRequest lrp = new DesiredLRPCreateRequest();
		lrp.setProcessGuid(guid);
		lrp.setRootfs(DOCKER_PATH);
		lrp.runAction().setPath("java");
		lrp.runAction().addArg("-Djava.security.egd=file:/dev/./urandom");
		lrp.runAction().addArg("-jar");
		lrp.runAction().addArg("/module-launcher.jar");

		List<EnvironmentVariable> environmentVariables = new ArrayList<EnvironmentVariable>();
		Collections.addAll(environmentVariables, lrp.getEnv());
		environmentVariables.add(new EnvironmentVariable("MODULES", request.getCoordinates().toString()));
		environmentVariables.add(new EnvironmentVariable("SPRING_PROFILES_ACTIVE", "cloud"));

		lrp.setEnv(environmentVariables.toArray(new EnvironmentVariable[environmentVariables.size()]));
		lrp.setMemoryMb(512);
		lrp.setPorts(new int[] {8080, 9000});

		lrp.addRoute(8080, new String[] {guid + "." + BASE_ADDRESS, guid + "-8080." + BASE_ADDRESS});
		lrp.addRoute(9000, new String[] {guid + "-9000." + BASE_ADDRESS});

		logger.debug("Desired LRP: {}", lrp);
		for (EnvironmentVariable e : environmentVariables) {
			logger.debug("{}={}", e.getName(), e.getValue());
		}

		receptorClient.createDesiredLRP(lrp);
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
		receptorClient.deleteDesiredLRP(guid(id));
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		ModuleStatus.Builder builder = ModuleStatus.of(id);

		// todo: if the actual LRP is not found, search for the desired LRP to verify
		// that the LRP is known to Lattice
		for (ActualLRPResponse lrp : receptorClient.getActualLRPsByProcessGuid(guid(id))) {
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put("address", lrp.getAddress());
			attributes.put("cellId", lrp.getCellId());
			attributes.put("domain", lrp.getDomain());
			attributes.put("processGuid", lrp.getProcessGuid());
			attributes.put("index", Integer.toString(lrp.getIndex()));
			attributes.put("ports", StringUtils.arrayToCommaDelimitedString(lrp.getPorts()));
			attributes.put("since", Long.toString(lrp.getSince()));
			builder.with(new ReceptorModuleInstanceStatus(lrp.getInstanceGuid(), lrp.getState(), attributes));
		}
		return builder.build();
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		throw new UnsupportedOperationException();
	}
}