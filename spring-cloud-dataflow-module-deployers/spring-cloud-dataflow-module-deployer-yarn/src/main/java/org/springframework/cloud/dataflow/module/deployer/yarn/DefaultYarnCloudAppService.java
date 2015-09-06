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

package org.springframework.cloud.dataflow.module.deployer.yarn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;
import org.springframework.yarn.boot.app.AbstractClientApplication;
import org.springframework.yarn.boot.app.YarnContainerClusterApplication;
import org.springframework.yarn.boot.app.YarnInfoApplication;
import org.springframework.yarn.boot.app.YarnPushApplication;
import org.springframework.yarn.boot.app.YarnSubmitApplication;

/**
 * Default implementation of {@link YarnCloudAppService} which talks to
 * rest api's exposed by specific yarn controlling container clusters.
 *
 * @author Janne Valkealahti
 * @author Mark Fisher
 */
public class DefaultYarnCloudAppService implements YarnCloudAppService {

	private static final Logger logger = LoggerFactory.getLogger(DefaultYarnCloudAppService.class);

	private static final String PREFIX_YIA = "spring.yarn.internal.YarnInfoApplication.";

	private static final String PREFIX_CCA = "spring.yarn.internal.ContainerClusterApplication.";

	private static final String SPRING_CONFIG_NAME_OPTION = "--spring.config.name=";

	private final String bootstrapName;

	public DefaultYarnCloudAppService(String bootstrapName) {
		this.bootstrapName = bootstrapName;
	}

	@Override
	public Collection<CloudAppInfo> getApplications() {
		ArrayList<CloudAppInfo> infos = new ArrayList<CloudAppInfo>();

		YarnInfoApplication app = new YarnInfoApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX_YIA + "operation", "PUSHED");
		app.appProperties(appProperties);
		String info = runApp(app);
		logger.debug("Full response for PUSHED: {}", info);

		String[] lines = info.split("\\r?\\n");
		if (lines.length > 2) {
			for (int i = 2; i < lines.length; i++) {
				String[] fields = lines[i].trim().split("\\s+");
				if (fields.length > 1) {
					infos.add(new CloudAppInfo(fields[0].trim()));
				}
			}
		}
		return infos;
	}

	@Override
	public Collection<CloudAppInstanceInfo> getInstances() {
		ArrayList<CloudAppInstanceInfo> infos = new ArrayList<CloudAppInstanceInfo>();

		YarnInfoApplication app = new YarnInfoApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX_YIA + "operation", "SUBMITTED");
		appProperties.setProperty(PREFIX_YIA + "verbose", "false");
		appProperties.setProperty(PREFIX_YIA + "type", "DATAFLOW");
		app.appProperties(appProperties);
		String info = runApp(app);
		logger.debug("Full response for SUBMITTED: {}", info);

		String[] lines = info.split("\\r?\\n");
		if (lines.length > 2) {
			for (int i = 2; i < lines.length; i++) {
				String[] fields = lines[i].trim().split("\\s+");
				if (fields.length > 10) {
					infos.add(new CloudAppInstanceInfo(fields[0].trim(), fields[2].trim(), fields[fields.length - 1]
							.trim()));
				}
			}
		}
		return infos;
	}

	@Override
	public void pushApplication(String appVersion) {
		YarnPushApplication app = new YarnPushApplication();
		app.applicationVersion(appVersion);
		Properties instanceProperties = new Properties();
		instanceProperties.setProperty("spring.yarn.applicationVersion", appVersion);
		app.configFile("application.properties", instanceProperties);
		runApp(app);
	}

	@Override
	public String submitApplication(String appVersion) {
		String appName = "spring-cloud-dataflow-yarn-app_" + appVersion;
		YarnSubmitApplication app = new YarnSubmitApplication();
		if (StringUtils.hasText(appName)) {
			app.applicationName(appName);
		}
		app.applicationVersion(appVersion);
		return runApp(app);
	}

	@Override
	public void createCluster(String yarnApplicationId, String clusterId, int count, String module,
			Map<String, String> definitionParameters) {
		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX_CCA + "operation", "CLUSTERCREATE");
		appProperties.setProperty(PREFIX_CCA + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX_CCA + "clusterId", clusterId);
		appProperties.setProperty(PREFIX_CCA + "clusterDef", "module-template");
		appProperties.setProperty(PREFIX_CCA + "projectionType", "default");
		appProperties.setProperty(PREFIX_CCA + "projectionData.any", Integer.toString(count));
		appProperties.setProperty(PREFIX_CCA + "extraProperties.containerModules", module);

		int i = 0;
		for (Map.Entry<String, String> entry : definitionParameters.entrySet()) {
			appProperties.setProperty(PREFIX_CCA + "extraProperties.containerArg" + i++ ,
					entry.getKey() + "=" + entry.getValue());
		}

		app.appProperties(appProperties);
		String output = runApp(app);
		logger.debug("Output from YarnContainerClusterApplication run for CLUSTERCREATE: {}", output);
	}

	@Override
	public void startCluster(String yarnApplicationId, String clusterId) {
		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX_CCA + "operation", "CLUSTERSTART");
		appProperties.setProperty(PREFIX_CCA + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX_CCA + "clusterId", clusterId);
		app.appProperties(appProperties);
		String output = runApp(app);
		logger.debug("Output from YarnContainerClusterApplication run for CLUSTERSTART: {}", output);
	}

	@Override
	public void stopCluster(String yarnApplicationId, String clusterId) {
		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX_CCA + "operation", "CLUSTERSTOP");
		appProperties.setProperty(PREFIX_CCA + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX_CCA + "clusterId", clusterId);
		app.appProperties(appProperties);
		String output = runApp(app);
		logger.debug("Output from YarnContainerClusterApplication run for CLUSTERSTOP: {}", output);
	}

	@Override
	public void destroyCluster(String yarnApplicationId, String clusterId) {
		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX_CCA + "operation", "CLUSTERDESTROY");
		appProperties.setProperty(PREFIX_CCA + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX_CCA + "clusterId", clusterId);
		app.appProperties(appProperties);
		String output = runApp(app);
		logger.debug("Output from YarnContainerClusterApplication run for CLUSTERDESTROY: {}", output);
	}

	@Override
	public Map<String, String> getClustersStates() {
		HashMap<String, String> states = new HashMap<String, String>();
		for (CloudAppInstanceInfo instanceInfo : getInstances()) {
			for (String cluster : getClusters(instanceInfo.getApplicationId())) {
				states.putAll(getInstanceClustersStates(instanceInfo.getApplicationId(), cluster));
			}
		}
		return states;
	}

	@Override
	public Collection<String> getClusters(String yarnApplicationId) {
		ArrayList<String> clusters = new ArrayList<String>();
		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX_CCA + "operation", "CLUSTERSINFO");
		appProperties.setProperty(PREFIX_CCA + "applicationId", yarnApplicationId);
		app.appProperties(appProperties);
		try {
			String output = runApp(app);
			logger.debug("Output from YarnContainerClusterApplication run for CLUSTERSINFO: {}", output);
			String[] lines = output.split("\\r?\\n");
			for (int i = 2; i < lines.length; i++) {
				String[] fields = lines[i].trim().split("\\s+");
				clusters.add(fields[0]);
			}
		} catch (Exception e) {
			logger.warn("CLUSTERSINFO resulted an error", e);
		}
		return clusters;
	}

	private Map<String, String> getInstanceClustersStates(String yarnApplicationId, String clusterId) {
		HashMap<String, String> states = new HashMap<String, String>();
		YarnContainerClusterApplication app = new YarnContainerClusterApplication();
		Properties appProperties = new Properties();
		appProperties.setProperty(PREFIX_CCA + "operation", "CLUSTERINFO");
		appProperties.setProperty(PREFIX_CCA + "applicationId", yarnApplicationId);
		appProperties.setProperty(PREFIX_CCA + "verbose", "false");
		appProperties.setProperty(PREFIX_CCA + "clusterId", clusterId);
		app.appProperties(appProperties);
		try {
			String output = runApp(app);
			logger.debug("Output from YarnContainerClusterApplication run for CLUSTERINFO: {}", output);

			String[] lines = output.trim().split("\\r?\\n");
			if (lines.length == 3) {
				String[] fields = lines[2].trim().split("\\s+");
				states.put(clusterId, fields[0].trim());
			}
		}
		catch (Exception e) {
			logger.warn("CLUSTERINFO resulted an error", e);
		}
		return states;
	}

	private <R> String runApp(AbstractClientApplication<R,?> app) {
		R result = app.run(SPRING_CONFIG_NAME_OPTION + this.bootstrapName);
		return result != null ? result.toString() : null;
	}

}
