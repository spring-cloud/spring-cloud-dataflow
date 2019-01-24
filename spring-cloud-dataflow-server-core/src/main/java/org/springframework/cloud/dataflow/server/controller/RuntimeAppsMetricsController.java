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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Christian Tzolov
 */
@RestController
@RequestMapping("/metrics/streams")
public class RuntimeAppsMetricsController {

	public static final String ATTRIBUTE_SKIPPER_APPLICATION_NAME = "skipper.application.name";
	public static final String ATTRIBUTE_SKIPPER_RELEASE_VERSION = "skipper.release.version";
	public static final String ATTRIBUTE_GUID = "guid";

	private static Log logger = LogFactory.getLog(RuntimeAppsMetricsController.class);

	private final StreamDeployer streamDeployer;

	public RuntimeAppsMetricsController(StreamDeployer streamDeployer) {
		this.streamDeployer = streamDeployer;
	}

	@RequestMapping(method = RequestMethod.GET)
	public List<ApplicationsMetrics> list(@RequestParam("names") String[] names) {
		try {
			return Stream.of(names).map(this::toStreamMetrics).collect(Collectors.toList());
		}
		catch (Exception e) {
			logger.error("Failed to retrieve any metrics", e);
		}
		return Collections.emptyList();
	}

	private ApplicationsMetrics toStreamMetrics(String streamName) {
		ApplicationsMetrics streamMetrics = new ApplicationsMetrics();
		streamMetrics.setName(streamName);
		streamMetrics.setApplications(new ArrayList<>());

		List<AppStatus> streamAppStatuses = this.streamDeployer.getStreamStatuses(streamName);

		if (!CollectionUtils.isEmpty(streamAppStatuses)) {
			for (AppStatus appStatus : streamAppStatuses) {
				try {
					ApplicationsMetrics.Application appMetrics = new ApplicationsMetrics.Application();
					streamMetrics.getApplications().add(appMetrics);
					appMetrics.setInstances(new ArrayList<>());

					for (Map.Entry<String, AppInstanceStatus> instanceEntry : appStatus.getInstances().entrySet()) {
						AppInstanceStatus appInstanceStatus = instanceEntry.getValue();
						ApplicationsMetrics.Instance instanceMetrics = new ApplicationsMetrics.Instance();
						appMetrics.getInstances().add(instanceMetrics);

						instanceMetrics.setGuid(getAppInstanceGuid(appInstanceStatus));
						instanceMetrics.setState(appInstanceStatus.getState().name());
						instanceMetrics.setProperties(Collections.emptyMap());

						appMetrics.setName(appInstanceStatus.getAttributes().get(ATTRIBUTE_SKIPPER_APPLICATION_NAME));
						streamMetrics.setVersion(appInstanceStatus.getAttributes().get(ATTRIBUTE_SKIPPER_RELEASE_VERSION));
					}
				}
				catch (Throwable throwable) {
					logger.warn("Failed to retrieve runtime status for " + appStatus.getDeploymentId(), throwable);
				}
			}
		}
		return streamMetrics;
	}

	private String getAppInstanceGuid(AppInstanceStatus instance) {
		return instance.getAttributes().containsKey(ATTRIBUTE_GUID) ?
				instance.getAttributes().get(ATTRIBUTE_GUID) : instance.getId();
	}
}
