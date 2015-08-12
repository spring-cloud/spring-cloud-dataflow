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

package org.springframework.cloud.data.module.deployer.yarn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.data.core.ModuleCoordinates;
import org.springframework.cloud.data.core.ModuleDefinition;
import org.springframework.cloud.data.core.ModuleDeploymentId;
import org.springframework.cloud.data.core.ModuleDeploymentRequest;
import org.springframework.cloud.data.module.ModuleStatus;
import org.springframework.cloud.data.module.deployer.ModuleDeployer;
import org.springframework.util.StringUtils;
import org.springframework.yarn.boot.app.YarnContainerClusterApplication;
import org.springframework.yarn.boot.app.YarnInfoApplication;

/**
 * {@link ModuleDeployer} which communicates with a Yarn app running
 * on a Hadoop cluster waiting for deployment requests. This app
 * uses Spring Yarn's container grouping functionality to create a
 * new group per module type. This allows all modules to share the
 * same settings and the group itself can controlled, i.e. ramp up/down
 * or shutdown/destroy a whole group.
 *
 * @author Janne Valkealahti
 */
public class YarnModuleDeployer implements ModuleDeployer {

	private static final Logger logger = LoggerFactory.getLogger(YarnModuleDeployer.class);
	private static final String PREFIX = "spring.yarn.internal.ContainerClusterApplication.";

	public YarnModuleDeployer() {
	}

	@Override
	public ModuleDeploymentId deploy(ModuleDeploymentRequest request) {
		int count = request.getCount();
		ModuleCoordinates coordinates = request.getCoordinates();
		ModuleDefinition definition = request.getDefinition();
		logger.info("deploying request for definition: " + definition);

		ModuleDeploymentId moduleDeploymentId = ModuleDeploymentId.fromModuleDefinition(definition);
		String clusterId = sanitizeClusterId(convertFromModuleDeploymentId(moduleDeploymentId));

		String yarnApplicationId = findRunningCloudDataYarnApp();
		logger.info("Using application id " + yarnApplicationId);
		String module = coordinates.toString();
		logger.info("deploying module: " + module);

		Map<String, String> definitionParameters = definition.getParameters();
		Map<String, String> deploymentProperties = request.getDeploymentProperties();
		logger.info("definitionParameters: " + definitionParameters);
		logger.info("deploymentProperties: " + deploymentProperties);

		// Using same app instance yarn boot cli is using to
		// communicate with an app running on yarn via its boot actuator
		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX + "operation", "CLUSTERCREATE");
		appProperties.setProperty(PREFIX + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX + "clusterId", clusterId);
		appProperties.setProperty(PREFIX + "clusterDef", "module-template");
		appProperties.setProperty(PREFIX + "projectionType", "default");
		appProperties.setProperty(PREFIX + "projectionData.any", Integer.toString(count));
		appProperties.setProperty(PREFIX + "extraProperties.containerModules", module);
		app.appProperties(appProperties);
		String output = app.run();
		logger.info("Output from YarnContainerClusterApplication run for CLUSTERCREATE: " + output);

		app = new YarnContainerClusterApplication();
		appProperties = new Properties();
		appProperties.setProperty(PREFIX + "operation", "CLUSTERSTART");
		appProperties.setProperty(PREFIX + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX + "clusterId", clusterId);
		app.appProperties(appProperties);
		output = app.run();
		logger.info("Output from YarnContainerClusterApplication run for CLUSTERSTART: " + output);

		return moduleDeploymentId;
	}

	@Override
	public void undeploy(ModuleDeploymentId id) {
		String clusterId = convertFromModuleDeploymentId(id);
		String yarnApplicationId = findRunningCloudDataYarnApp();
		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX + "operation", "CLUSTERSTOP");
		appProperties.setProperty(PREFIX + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX + "clusterId", clusterId);
		app.appProperties(appProperties);
		String output = app.run();
		logger.info("Output from YarnContainerClusterApplication run for CLUSTERSTOP: " + output);
	}

	@Override
	public ModuleStatus status(ModuleDeploymentId id) {
		String yarnApplicationId = findRunningCloudDataYarnApp();
		String clusterId = convertFromModuleDeploymentId(id);

		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX + "operation", "CLUSTERINFO");
		appProperties.setProperty(PREFIX + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX + "verbose", "false");
		appProperties.setProperty(PREFIX + "clusterId", clusterId);
		app.appProperties(appProperties);
		String info = app.run();
		logger.info("Output from YarnContainerClusterApplication run for CLUSTERINFO: " + info);

		boolean deployed = false;
		String[] lines = info.trim().split("\\r?\\n");
		if (lines.length == 3 && lines[2].contains("RUNNING")) {
			deployed = true;
		}

		YarnModuleInstanceStatus status = new YarnModuleInstanceStatus(id.toString(), deployed, null);
		return ModuleStatus.of(id).with(status).build();
	}

	@Override
	public Map<ModuleDeploymentId, ModuleStatus> status() {
		HashMap<ModuleDeploymentId, ModuleStatus> statuses = new HashMap<ModuleDeploymentId, ModuleStatus>();
		for (String clusterId : findRunningContainerClusters()) {
			ModuleDeploymentId moduleDeploymentId = convertToModuleDeploymentId(clusterId);
			statuses.put(moduleDeploymentId, status(moduleDeploymentId));
		}
		return statuses;
	}

	private static String findRunningCloudDataYarnApp() {
		YarnInfoApplication app = new YarnInfoApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty("spring.yarn.internal.YarnInfoApplication.operation", "SUBMITTED");
		appProperties.setProperty("spring.yarn.internal.YarnInfoApplication.verbose", "false");
		appProperties.setProperty("spring.yarn.internal.YarnInfoApplication.type", "CLOUDDATA");
		app.appProperties(appProperties);
		String info = app.run();
		logger.info("Full status response for SUBMITTED app " + info);

		// TODO: either make this easier in YarnInfoApplication
		//       or use rest api directly
		String[] lines = info.split("\\r?\\n");
		logger.info("Parsing application id from " + StringUtils.arrayToCommaDelimitedString(lines));
		if (lines.length == 3) {
			return lines[2].trim().split("\\s+")[0].trim();
		} else {
			return null;
		}
	}

	private static Collection<String> findRunningContainerClusters() {
		String yarnApplicationId = findRunningCloudDataYarnApp();

		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX + "operation", "CLUSTERSINFO");
		appProperties.setProperty(PREFIX + "applicationId", yarnApplicationId);
		app.appProperties(appProperties);
		String info = app.run();
		logger.info("Output from YarnContainerClusterApplication run for CLUSTERSINFO: " + info);

		ArrayList<String> ids = new ArrayList<String>();
		String[] lines = info.split("\\r?\\n");
		if (lines.length > 2) {
			for (int i = 2; i < lines.length; i++) {
				ids.add(lines[i].trim());
			}
		}
		return ids;
	}

	private static String convertFromModuleDeploymentId(ModuleDeploymentId moduleDeploymentId) {
		return moduleDeploymentId.getGroup() + ":" + moduleDeploymentId.getLabel();
	}

	private static ModuleDeploymentId convertToModuleDeploymentId(String containerClusterName) {
		String[] split = containerClusterName.split(":");
		if (split.length == 2) {
			return new ModuleDeploymentId(split[0], split[1]);
		} else {
			return null;
		}
	}

	private static String sanitizeClusterId(String clusterId) {
		// spring yarn bug which treats postfix after dot
		// as file delimiter and removes it per default mvc
		// feature. need to handle this in shdp
		return clusterId.replaceAll("\\.", "_");
	}

}
