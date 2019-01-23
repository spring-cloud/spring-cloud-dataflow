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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.stream.SkipperStreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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

			Iterable<StreamDefinition> streamDefinitions = this.streamDefinitionRepository.findAll();

			// For every stream definition retrieve its runtime apps
			return StreamSupport.stream(streamDefinitions.spliterator(), false)
					.map(this::toStreamMetrics)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		}
		catch (Exception e) {
			logger.error(e);
		}
		return Collections.emptyList();
	}

	private ApplicationsMetrics toStreamMetrics(StreamDefinition streamDefinition) {

		String streamName = streamDefinition.getName();

		List<AppStatus> streamAppStatuses = runtimeStatus(streamName);

		if (CollectionUtils.isEmpty(streamAppStatuses)) {
			return null;
		}

		try {

			ApplicationsMetrics streamMetrics = new ApplicationsMetrics();
			streamMetrics.setName(streamName);
			streamMetrics.setApplications(new ArrayList<>());

			for (AppStatus appStatus : streamAppStatuses) {

				StreamAppDefinition streamAppDefinition =
						this.resolveApplicationDefinitionFromAppStatus(streamDefinition, appStatus);

				String applicationName = streamAppDefinition.getName();

				ApplicationsMetrics.Application appMetrics = new ApplicationsMetrics.Application();
				streamMetrics.getApplications().add(appMetrics);

				appMetrics.setName(applicationName);
				appMetrics.setInstances(new ArrayList<>());

				for (Map.Entry<String, AppInstanceStatus> instance : appStatus.getInstances().entrySet()) {

					ApplicationsMetrics.Instance instanceMetrics = new ApplicationsMetrics.Instance();
					appMetrics.getInstances().add(instanceMetrics);

					//TODO (WARNING): The Guid attribute is not universal across all deployers.
					String appInstanceGuid = instance.getValue().getAttributes().get("guid");
					instanceMetrics.setGuid(appInstanceGuid);

					//int instanceIndex = resolveInstanceIndexFromInstanceId(instance.getValue());
					//instanceMetrics.setIndex(instanceIndex); // TODO: Looks like the index is not required

					instanceMetrics.setProperties(Collections.emptyMap());

					instanceMetrics.setState(instance.getValue().getState().name());
				}
			}
			return streamMetrics;
		}
		catch (Throwable throwable) {
			logger.warn(throwable);
		}

		return null;
	}

	private List<AppStatus> runtimeStatus(String streamName) {
		try {
			Info info = this.skipperClient.status(streamName);
			return SkipperStreamDeployer.deserializeAppStatus(info.getStatus().getPlatformStatus());
		}
		catch (Exception e) {
			// ignore as we query status for all the streams.
		}
		return Collections.emptyList();
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
	private StreamAppDefinition resolveApplicationDefinitionFromAppStatus(StreamDefinition streamDefinition,
			AppStatus appStatus) {

		String deploymentId = appStatus.getDeploymentId();

		Assert.hasText(deploymentId, "Valid deployer ID can not be empty!");

		for (StreamAppDefinition sd : streamDefinition.getAppDefinitions()) {

			if (deploymentId.startsWith(sd.getStreamName() + "." + sd.getName())) { // Local Deployer: <stream name>.<app name/label>-vXX
				return sd;
			}
			else if (deploymentId.startsWith(sd.getStreamName() + "-" + sd.getName())) { // K8s Deployer: <stream name>-<app name/label>-vXX
				return sd;
			}
			else {
				String boza = deploymentId.substring(deploymentId.indexOf("-"));
				if (boza.startsWith("-" + sd.getStreamName() + "-" + sd.getName())) { // CF Deployer: <random string>-<stream name>-<app name/label>-vXX
					return sd;
				}
			}
		}
		return null;
	}

	///**
	// *
	// * WARNING: Makes assumption about the Instance ID's naming convention, which may not be consistent across the
	// * deployment platforms.
	// *
	// * @param instanceStatus
	// * @return Returns the Instance's index
	// */
	//private int resolveInstanceIndexFromInstanceId(AppInstanceStatus instanceStatus) {
	//	String instanceId = instanceStatus.getId();
	//	String indexId = instanceId.substring(instanceId.lastIndexOf("-") + 1);
	//	return Integer.valueOf(indexId);
	//}
}
