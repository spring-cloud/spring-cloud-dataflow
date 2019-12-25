/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.integration.test.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.StreamRuntimePropertyKeys;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Helper class to retrieve runtime information form DataFlow server.
 */
public class RuntimeApplicationHelper {

	private final Logger logger = LoggerFactory.getLogger(RuntimeApplicationHelper.class);

	private final String platformType;

	private RestTemplate restTemplate = new RestTemplate();

	private DataFlowTemplate dataFlowOperations;

	private final String platformName;

	public RuntimeApplicationHelper(DataFlowTemplate dataFlowOperations, String platformName) {
		Assert.notNull(dataFlowOperations, "Valid dataFlowOperations is expected but was: " + dataFlowOperations);
		Assert.hasText(platformName, "Empty platform name: " + platformName);

		this.dataFlowOperations = dataFlowOperations;
		this.platformName = platformName;
		this.platformType = dataFlowOperations.streamOperations().listPlatforms().stream()
				.filter(p -> p.getName().equalsIgnoreCase(platformName))
				.map(d -> d.getType()).findFirst().get();
		Assert.hasText(this.platformType, "Could not find platform type for: " + platformName);
		logger.info(String.format("Selected platform: [%s:%s]", platformName, platformType));
	}

	public String getPlatformName() {
		return platformName;
	}

	public String getPlatformType() {
		return platformType;
	}

	/**
	 * Return the attributes for each application instance for all applications in all deployed streams.
	 * @return Returns map of map with following structure: (appInstanceId, (propertyName, propertyValue))
	 */
	public Map<String, Map<String, String>> appInstanceAttributes() {
		Map<String, Map<String, String>> appInstanceAttributes = new HashMap<>();
		Iterable<AppStatusResource> apps = dataFlowOperations.runtimeOperations().status();
		for (AppStatusResource app : apps) {
			Iterable<AppInstanceStatusResource> instances = app.getInstances();
			for (AppInstanceStatusResource instance : instances) {
				Map<String, String> attrs = instance.getAttributes();
				appInstanceAttributes.put(instance.getInstanceId(), attrs);
			}
		}
		return appInstanceAttributes;
	}

	/**
	 * Extract the Logs from the first instance of an application in a stream.
	 * @param streamName Name of the stream where the application is defined.
	 * @param appName Name of the applications to retrieve the applicationInstanceLogs for.
	 * @return Returns the applicationInstanceLogs of the first instance of the specified application.
	 */
	public String getFirstInstanceLog(String streamName, String appName) {
		return this.applicationInstanceLogs(streamName, appName).values().iterator().next();
	}

	/**
	 *
	 * @param streamName DataFlow stream for which the log is retrieved.
	 * @param appName Application inside the stream name for which logs are trieved.
	 * @return Returns a map of app instance GUIDs and their properties as map.
	 */
	public Map<String, Map<String, String>> getApplicationInstances(String streamName, String appName) {
		return this.appInstanceAttributes().values().stream()
				.filter(v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_RELEASE_NAME).equals(streamName))
				.filter(v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_APPLICATION_NAME).equals(appName))
				.collect(Collectors.toMap(v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_GUID), v -> v));
	}

	/**
	 * For given stream name and application name retrieves the logs for application instances (if more then one)
	 * belonging to this application.
	 *
	 * @param streamName DataFlow stream for which the log is retrieved.
	 * @param appName Application inside the stream name for which logs are trieved.
	 * @return Returns a map of app instance GUIDs and their Log content. A single entry per app instance.
	 */
	public Map<String, String> applicationInstanceLogs(String streamName, String appName) {
		return this.appInstanceAttributes().values().stream()
				.filter(v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_RELEASE_NAME).equals(streamName))
				.filter(v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_APPLICATION_NAME).equals(appName))
				.collect(Collectors.toMap(
						v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_GUID),
						v -> getAppInstanceLogContent(getApplicationInstanceUrl(v))));
	}

	/**
	 * Retrieve application's REST url for Stream Application instance
	 * @param streamName stream holding the application instance.
	 * @param appName application name to retrieve the URL for.
	 * @return Application URL
	 */
	public String getApplicationInstanceUrl(String streamName, String appName) {
		Map<String, String> instanceAttributes = getApplicationInstances(streamName, appName)
				.values().iterator().next();
		return getApplicationInstanceUrl(instanceAttributes);
	}

	/**
	 * Retrieve application's REST url from the runtime attributes.
	 * @param instanceAttributes Application runtime attributes.
	 * @return Application URL
	 */
	public String getApplicationInstanceUrl(Map<String, String> instanceAttributes) {
		switch (this.platformType) {
		case "local":
			return localApplicationInstanceUrl(instanceAttributes);
		case "kubernetes":
			return kubernetesApplicationInstanceUrl(instanceAttributes);
		case "cloudfoundry":
			return cloudFoundryApplicationInstanceUrl(instanceAttributes);
		}

		throw new IllegalStateException("Unknown platform type:" + platformType);
	}

	private String localApplicationInstanceUrl(Map<String, String> instanceAttributes) {
		return String.format("http://localhost:%s",
				instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_PORT)); // Local Platform only
	}

	private String cloudFoundryApplicationInstanceUrl(Map<String, String> instanceAttributes) {
		return instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_URL).replace("http:", "https:").toLowerCase();
	}

	private String kubernetesApplicationInstanceUrl(Map<String, String> instanceAttributes) {

		if (instanceAttributes.containsKey(StreamRuntimePropertyKeys.ATTRIBUTE_URL)) {
			return instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_URL);
		}

		// Wait until all apps External-IP are exposed (K8s specific)
		String streamName = instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_RELEASE_NAME);
		String appName = instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_APPLICATION_NAME);
		String guid = instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_GUID);
		Wait.on(streamName)
				.withDescription("Wait for " + streamName + ":" + appName + " ExternalIP")
				.until(s -> this.appInstanceAttributes().values().stream()
						.filter(m -> m.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_RELEASE_NAME).equals(streamName))
						.filter(m -> m.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_APPLICATION_NAME).equals(appName))
						.allMatch(m -> m.containsKey(StreamRuntimePropertyKeys.ATTRIBUTE_URL)));

		return this.getApplicationInstances(streamName, appName).get(guid).get(StreamRuntimePropertyKeys.ATTRIBUTE_URL);
	}

	/**
	 * Retrieve the log for an app.
	 * @param instanceUrl URL of the application as exposed.
	 * @return String containing the contents of the log or 'null' if not found.
	 */
	private String getAppInstanceLogContent(String instanceUrl) {
		String logContent = null;
		String logFileUrl = String.format("%s/actuator/logfile", instanceUrl);
		try {
			logContent = restTemplate.getForObject(logFileUrl, String.class);
			if (logContent == null) {
				logger.warn("Unable to retrieve logfile from '" + logFileUrl);
				logContent = "empty";
			}
		}
		catch (Exception e) {
			logger.warn("Error while trying to access logfile from '" + logFileUrl + "' due to : " + e);
		}
		return logContent;
	}

	/**
	 * Performs serviceUrl to determine if a service is running at this URL
	 * @param serviceUrl The full URL of the service to test.
	 * @return Return ture if the response is not HTTP error and false otherwise.
	 */
	public boolean isServicePresent(String serviceUrl) {
		try {
			restTemplate.getForObject(serviceUrl, String.class);
			return true;
		}
		catch (Exception e) {
			//do nothing
		}
		return false;
	}
}
