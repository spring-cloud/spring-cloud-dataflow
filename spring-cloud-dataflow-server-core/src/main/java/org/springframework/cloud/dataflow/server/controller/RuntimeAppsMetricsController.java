/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

/**
 *
 * @author Christian Tzolov
 */
@RestController
@RequestMapping("/metrics/streams")
public class RuntimeAppsMetricsController {

	private static Log logger = LogFactory.getLog(RuntimeAppsMetricsController.class);

	private final StreamDefinitionRepository streamDefinitionRepository;
	private final SkipperClient skipperClient;

	private final static List<ApplicationsMetrics> EMPTY_RESPONSE = new ArrayList<>();

	/**
	 * Instantiates a new metrics controller.*
	 */
	public RuntimeAppsMetricsController(StreamDefinitionRepository streamDefinitionRepository,
			SkipperClient skipperClient) {
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.skipperClient = skipperClient;
	}

	@RequestMapping(method = RequestMethod.GET)
	public List<ApplicationsMetrics> list() {
		try {
			List<ApplicationsMetrics> metrics = new ArrayList<>();
			Iterable<StreamDefinition> streamDefinitions = this.streamDefinitionRepository.findAll();

			for (StreamDefinition streamDefinition : streamDefinitions) {
				List<AppStatus> appStatuses = skipperStatus(streamDefinition.getName());
				ApplicationsMetrics appMetrics = toStreamMetrics(streamDefinition, appStatuses);
				if (appMetrics != null) {
					metrics.add(appMetrics);
				}
			}
			return metrics;
		}
		catch (ResourceAccessException e) {
			logger.warn(e);
		}
		catch (Exception e) {
			logger.error(e);
		}
		return EMPTY_RESPONSE;
	}

	private List<AppStatus> skipperStatus(String streamName) {
		List<AppStatus> appStatuses = new ArrayList<>();
		try {
			Info info = this.skipperClient.status(streamName);
			appStatuses.addAll(SkipperStreamDeployer.deserializeAppStatus(info.getStatus().getPlatformStatus()));
		}
		catch (Exception e) {
			// ignore as we query status for all the streams.
		}
		return appStatuses;
	}

	private ApplicationsMetrics toStreamMetrics(StreamDefinition streamDefinition, List<AppStatus> streamAppStatuses) {

		if (CollectionUtils.isEmpty(streamAppStatuses)) {
			return null;
		}

		try {

			String streamName = streamDefinition.getName();

			ApplicationsMetrics streamMetrics = new ApplicationsMetrics();
			streamMetrics.setName(streamName);
			streamMetrics.setApplications(new ArrayList<>());

			for (AppStatus appStatus : streamAppStatuses) {

				StreamAppDefinition streamAppDefinition =
						this.findApplicationDefinitionFromAppStatus(streamDefinition, appStatus);

				String applicationName = streamAppDefinition.getName();

				ApplicationsMetrics.Application appMetrics = new ApplicationsMetrics.Application();
				streamMetrics.getApplications().add(appMetrics);

				appMetrics.setName(applicationName);
				appMetrics.setInstances(new ArrayList<>());

				for (Map.Entry<String, AppInstanceStatus> instance : appStatus.getInstances().entrySet()) {

					int instanceIndex = resolveInstanceIndexFromInstanceId(instance.getValue());
					String appInstanceGuid = instance.getValue().getAttributes().get("guid");
					String appType = streamAppDefinition.getApplicationType().name();

					ApplicationsMetrics.Instance instanceMetrics = new ApplicationsMetrics.Instance();
					appMetrics.getInstances().add(instanceMetrics);

					instanceMetrics.setGuid(appInstanceGuid);
					instanceMetrics.setIndex(instanceIndex);
					instanceMetrics.setProperties(createInstanceProperties(instanceIndex, appInstanceGuid, streamName,
							applicationName, appType, instance.getValue().getState()));
				}
			}
			return streamMetrics;
		}
		catch (Throwable throwable) {
			logger.warn(throwable);
		}

		return null;
	}

	/**
	 * Reverse engineer to find the AppDefinition from the reported AppStatus.
	 *
	 * WARNING: This approach assumes certain name convention for the appState deployment id, which not be the same
	 * across the various deployment platforms !!!!
	 *
	 * @param streamDefinition stream with all application definitions.
	 * @param appStatus status for one of the applications in the streamDefinition.
	 * @return
	 */
	private StreamAppDefinition findApplicationDefinitionFromAppStatus(StreamDefinition streamDefinition,
			AppStatus appStatus) {

		String deploymentId = appStatus.getDeploymentId();

		for (StreamAppDefinition sd : streamDefinition.getAppDefinitions()) {
			if (deploymentId.startsWith(sd.getStreamName() + "." + sd.getName())) {
				return sd;
			}
		}
		return null;
	}

	/**
	 *
	 * WARNING: Makes assumption about the Instance ID's naming convention, which may not be consistent across the
	 * deployment platforms.
	 *
	 * @param instanceStatus
	 * @return Returns the Instance's index
	 */
	private int resolveInstanceIndexFromInstanceId(AppInstanceStatus instanceStatus) {
		String instanceId = instanceStatus.getId();
		String indexId = instanceId.substring(instanceId.lastIndexOf("-") + 1);
		return Integer.valueOf(indexId);
	}

	private Map<String, Object> createInstanceProperties(int instanceIndex, String applicationGuid, String stream,
			String applicationName, String applicationType, DeploymentState state) {

		Map<String, Object> properties = new HashMap<>();
		properties.put("spring.application.index", instanceIndex);
		properties.put("spring.cloud.application.guid", applicationGuid);
		properties.put("spring.cloud.dataflow.stream.app.type", applicationType);
		properties.put("spring.cloud.dataflow.stream.name", stream);
		properties.put("spring.cloud.application.group", stream);
		properties.put("spring.cloud.dataflow.stream.app.label", applicationName);

		properties.put("spring.cloud.dataflow.stream.app.state", state.name()); // new

		return properties;
	}
}
