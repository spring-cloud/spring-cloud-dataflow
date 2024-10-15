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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.ArgumentSanitizer;
import org.springframework.cloud.dataflow.core.StreamRuntimePropertyKeys;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.StreamStatusResource;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Helper class to retrieve runtime information form DataFlow server.
 *
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
public class RuntimeApplicationHelper {


	public static final String LOCAL_PLATFORM_TYPE = "local";

	public static final String KUBERNETES_PLATFORM_TYPE = "kubernetes";

	public static final String CLOUDFOUNDRY_PLATFORM_TYPE = "cloudfoundry";

	private final Logger logger = LoggerFactory.getLogger(RuntimeApplicationHelper.class);

	private final String platformType;

	private final DataFlowTemplate dataFlowTemplate;

	private final String platformName;

	private final Version dataflowServerVersion;

	private boolean httpsStreamApplications;

	public RuntimeApplicationHelper(DataFlowTemplate dataFlowTemplate, String platformName, boolean httpsStreamApplications) {
		this.httpsStreamApplications = httpsStreamApplications;
		Assert.notNull(dataFlowTemplate, "Valid dataFlowOperations is expected but was: " + dataFlowTemplate);
		Assert.hasText(platformName, "Empty platform name: " + platformName);
		logger.debug("platform Name: [" + platformName + "]");
		this.dataFlowTemplate = dataFlowTemplate;
		this.platformName = platformName;
		this.platformType = dataFlowTemplate.streamOperations().listPlatforms().stream()
				.filter(p -> p.getName().equalsIgnoreCase(platformName))
				.map(Deployer::getType).findFirst().get();

		dataflowServerVersion = Version.valueOf(dataFlowTemplate.aboutOperation().get()
				.getVersionInfo().getCore().getVersion());

		Assert.hasText(this.platformType, "Could not find platform type for: " + platformName);
	}

	public Version getDataflowServerVersion() {
		return dataflowServerVersion;
	}

	public boolean dataflowServerVersionEqualOrGreaterThan(String version) {
		return dataflowServerVersion.compareTo(Version.valueOf(version)) >= 0;
	}

	public boolean dataflowServerVersionLowerThan(String version) {
		return dataflowServerVersion.compareTo(Version.valueOf(version)) < 0;
	}

	public String getPlatformName() {
		return platformName;
	}

	public String getPlatformType() {
		return platformType;
	}

	/**
	 * Return the attributes for each application instance for all applications in all deployed streams.
	 *
	 * @return Returns map of map with following structure: (appInstanceId, (propertyName, propertyValue))
	 */
	public Map<String, Map<String, String>> appInstanceAttributes() {
		Map<String, Map<String, String>> appInstanceAttributes = new HashMap<>();
		Iterable<AppStatusResource> apps = dataFlowTemplate.runtimeOperations().status();
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
	 * @param streamName DataFlow stream for which the log is retrieved.
	 * @param appName    Application inside the stream name for which logs are trieved.
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
	 * @param appName    Application inside the stream name for which logs are trieved.
	 * @return Returns a map of app instance GUIDs and their Log content. A single entry per app instance.
	 */
	public Map<String, String> applicationInstanceLogs(String streamName, String appName) {
		// For K8s platforms the availability of the app's external URI is not dependent on the application state but
		// on the availability of the configured Load Balancer. So we need to wait until valid URI is returned.
		Awaitility.await().until(() -> getApplicationInstances(streamName, appName).values().stream()
				.allMatch(instanceAttributes -> isUrlAccessible(getApplicationInstanceUrl(instanceAttributes) + "/actuator/info")));

		return this.appInstanceAttributes().values().stream()
				.filter(v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_RELEASE_NAME).equals(streamName))
				.filter(v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_APPLICATION_NAME).equals(appName))
				.collect(Collectors.toMap(
						v -> v.get(StreamRuntimePropertyKeys.ATTRIBUTE_GUID),
						v -> getAppInstanceLogContent(getApplicationInstanceUrl(v))));
	}

	/**
	 * Retrieve application's REST url for Stream Application instance
	 *
	 * @param streamName stream holding the application instance.
	 * @param appName    application name to retrieve the URL for.
	 * @return Application URL
	 */
	public String getApplicationInstanceUrl(String streamName, String appName) {
		return getApplicationInstanceUrl(getApplicationInstances(streamName, appName)
				.values().iterator().next());
	}

	/**
	 * Retrieve application's REST url from the runtime attributes.
	 *
	 * @param instanceAttributes Application runtime attributes.
	 * @return Application URL
	 */
	public String getApplicationInstanceUrl(Map<String, String> instanceAttributes) {
		switch (this.platformType) {
			case LOCAL_PLATFORM_TYPE:
				return localApplicationInstanceUrl(instanceAttributes);
			case KUBERNETES_PLATFORM_TYPE:
				return kubernetesApplicationInstanceUrl(instanceAttributes);
			case CLOUDFOUNDRY_PLATFORM_TYPE:
				return cloudFoundryApplicationInstanceUrl(instanceAttributes);
		}

		throw new IllegalStateException("Unknown platform type:" + platformType);
	}

	private String localApplicationInstanceUrl(Map<String, String> instanceAttributes) {
		return String.format("%s://localhost:%s", httpsStreamApplications ? "https" : "http",
				instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_PORT)); // Local Platform only
	}

	private String cloudFoundryApplicationInstanceUrl(Map<String, String> instanceAttributes) {
		return instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_URL).replace("http:", "https:").toLowerCase();
	}

	/**
	 * @param instanceAttributes runtime attributes of the app instance.
	 * @return Externally accessible app instance URL
	 */
	private String kubernetesApplicationInstanceUrl(Map<String, String> instanceAttributes) {
		String appInstanceUrl = instanceAttributes.get(StreamRuntimePropertyKeys.ATTRIBUTE_URL);
		if (httpsStreamApplications) {
			appInstanceUrl = appInstanceUrl.replace("http:", "https:").toLowerCase();
		}
		return appInstanceUrl;
	}

	/**
	 * Retrieve the log for an app.
	 *
	 * @param instanceUrl URL of the application as exposed.
	 * @return String containing the contents of the log or 'null' if not found.
	 */
	private String getAppInstanceLogContent(String instanceUrl) {
		String logContent = "";
		String logFileUrl = String.format("%s/actuator/logfile", instanceUrl);
		try {
			logContent = dataFlowTemplate.getRestTemplate().getForObject(logFileUrl, String.class);
			if (logContent == null) {
				logger.warn("Unable to retrieve logfile from '" + logFileUrl);
				logContent = "empty";
			}
		} catch (Exception e) {
			logger.warn("Error while trying to access logfile from '" + logFileUrl + "' due to : ", e);
		}
		return logContent;
	}

	/**
	 * Performs serviceUrl to determine if a service is running at this URL
	 *
	 * @param serviceUrl The full URL of the service to test.
	 * @return Return ture if the response is not HTTP error and false otherwise.
	 */
	public boolean isServicePresent(String serviceUrl) {
		if (null != serviceUrl) {
			try {
				dataFlowTemplate.getRestTemplate().getForObject(serviceUrl, String.class);
				return true;
			} catch (Exception e) {
				logger.trace("isServicePresent:"+ serviceUrl + ":exception:" + e);
			}
		}
		return false;
	}

	private boolean isUrlAccessible(String serviceUrl) {
		if (null != serviceUrl) {
			try {
				Set<HttpMethod> optionsForAllow = dataFlowTemplate.getRestTemplate().optionsForAllow(serviceUrl);
				return !CollectionUtils.isEmpty(optionsForAllow);
			} catch (Exception e) {
				logger.trace("isUrlAccessible:"+ serviceUrl + ":exception:" + e);
			}
		}
		return false;
	}

	public void httpPost(String streamName, String appName, String message) {
		Assert.notNull(message, "message required");
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain");
		httpPost(streamName, appName, message.getBytes(StandardCharsets.UTF_8), headers);
	}

	public void httpPostJson(String streamName, String appName, Object obj) throws JsonProcessingException {
		Assert.notNull(obj, "obj required");
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		String message = obj instanceof String s ? s : new ObjectMapper().writeValueAsString(obj);
		httpPost(streamName, appName, message.getBytes(StandardCharsets.UTF_8), headers);
	}


	public void httpPost(String url, String message) {
		ResponseEntity<String> response = dataFlowTemplate.getRestTemplate().postForEntity(url, message, String.class);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new RuntimeException("Exception posting:" + response.getStatusCode() + ":" + response.getBody());
		}
	}

	public void httpPost(String streamName, String appName, byte[] message, HttpHeaders headers) {
		AppInstanceStatusResource instance = null;
		AppStatusResource app = null;
		StreamStatusResource streamStatusResource = this.dataFlowTemplate.runtimeOperations()
				.streamStatus(streamName)
				.getContent()
				.stream()
				.filter(stream -> stream.getName().equals(streamName))
				.findFirst()
				.orElse(null);
		if (streamStatusResource != null) {
			app = streamStatusResource.getApplications()
					.getContent()
					.stream()
					.filter(appStatusResource -> appStatusResource.getName().equals(appName))
					.findFirst()
					.orElse(null);
			if (app != null) {
				instance = app.getInstances()
						.getContent()
						.stream()
						.filter(appInstanceStatusResource -> "deployed".equals(appInstanceStatusResource.getState()))
						.findFirst()
						.orElse(null);
			}
		}

		if (instance == null) {
			logger.error("Cannot find instance of " + streamName + ":" + appName);
			throw new RuntimeException("Cannot find instance of " + streamName + ":" + appName);
		}
		try {
			logger.debug("postToUrl:{}:{}:{}", app.getDeploymentId(), instance.getInstanceId(), headers);
			this.dataFlowTemplate.runtimeOperations().postToUrl(app.getDeploymentId(), instance.getInstanceId(), message, headers);
		} catch (Throwable x) {
			if (x.toString().contains("post endpoint not found")) {
				logger.warn("Try with url only:" + x);
				final String url = instance.getAttributes().get("url");
				if (StringUtils.hasText(url)) {
					Awaitility.await(url + " available").atMost(1, TimeUnit.MINUTES).until(() -> isUrlAccessible(url));
					HttpEntity<byte[]> httpEntity = new HttpEntity<>(message, headers);
					this.dataFlowTemplate.getRestTemplate().exchange(url, HttpMethod.POST, httpEntity, String.class);
				} else {
					String error = "Cannot find url for " + streamStatusResource.getName() + ":" + app.getName();
					ArgumentSanitizer sanitizer = new ArgumentSanitizer();
					logger.error(error + ":" + sanitizer.sanitizeProperties(instance.getAttributes()));
					throw new RuntimeException(error);
				}
			} else {
				logger.error("postToUrl:" + streamName + ":" + appName + ":" + x, x);
				throw x;
			}
		}
	}

	public String httpGet(String url) {
		return dataFlowTemplate.getRestTemplate().getForObject(url, String.class);
	}

	/**
	 * Check if an application is registered with DataFlow
	 *
	 * @param name    application name
	 * @param type    application type
	 * @param version application version
	 * @return Returns true if the application with provided coordinates is registered with DataFlow server and false otherwise.
	 */
	public boolean isAppRegistered(String name, ApplicationType type, String version) {
		try {
			DetailedAppRegistrationResource registration =
					dataFlowTemplate.appRegistryOperations().info(name, type, version, false);
			return registration != null;
		} catch (Exception e) {
			logger.trace("isAppRegistered:" + name + ":" + type + ":" + version + ":exception:" + e);
			return false;
		}
	}

	public boolean isAppRegistered(String name, ApplicationType type) {
		try {
			DetailedAppRegistrationResource registration =
					dataFlowTemplate.appRegistryOperations().info(name, type, false);
			return registration != null;
		} catch (Exception e) {
			logger.trace("isAppRegistered:" + name + ":" + type + ":exception:" + e);
			return false;
		}
	}

}
