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
package org.springframework.cloud.dataflow.audit.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.util.Assert;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class AuditServiceUtils {
	public static final String APP_NAME = "APP_NAME";

	public static final String APP_TYPE = "APP_TYPE";

	public static final String APP_VERSION = "APP_VERSION";

	public static final String APP_IS_DEFAULT = "APP_IS_DEFAULT";

	public static final String APP_URI = "uri";

	public static final String APP_META_DATA_URI = "metaDataUri";

	public static final String STREAM_DEFINITION_DSL_TEXT = "streamDefinitionDslText";

	public static final String TASK_DEFINITION_NAME = "taskDefinitionName";

	public static final String TASK_DEFINITION_PROPERTIES = "taskDefinitionProperties";

	public static final String DEPLOYMENT_PROPERTIES = "deploymentProperties";

	public static final String COMMANDLINE_ARGUMENTS = "commandlineArguments";

	private final ArgumentSanitizer argumentSanitizer = new ArgumentSanitizer();

	public AuditServiceUtils() {
	}

	public Map<String, Object> convertAppRegistrationToAuditData(AppRegistration appRegistration) {
		final Map<String, Object> auditData = new HashMap<>(4);

		auditData.put(APP_NAME, appRegistration.getName());
		auditData.put(APP_TYPE, appRegistration.getType());
		auditData.put(APP_VERSION, appRegistration.getVersion());
		auditData.put(APP_IS_DEFAULT, appRegistration.isDefaultVersion());
		auditData.put(APP_URI, appRegistration.getUri());
		auditData.put(APP_META_DATA_URI, appRegistration.getMetadataUri());

		return auditData;
	}

	public Map<String, String> sanitizeProperties(Map<String, String> properties) {
		return this.argumentSanitizer.sanitizeProperties(properties);
	}

	public Map<String, Object> convertStreamDefinitionToAuditData(
			String streamDefinition,
			Map<String, String> deploymentProperties) {

		final Map<String, Object> auditedData = new HashMap<>(2);
		auditedData.put(STREAM_DEFINITION_DSL_TEXT, streamDefinition);
		auditedData.put(DEPLOYMENT_PROPERTIES, this.argumentSanitizer.sanitizeProperties(deploymentProperties));
		return auditedData;
	}

	public Map<String, Object> convertScheduleRequestToAuditData(ScheduleRequest scheduleRequest) {
		Assert.notNull(scheduleRequest, "scheduleRequest must not be null");
		Assert.hasText(scheduleRequest.getScheduleName(),
				"The scheduleName of the scheduleRequest must not be null or empty");
		Assert.notNull(scheduleRequest.getDefinition(), "The task definition of the scheduleRequest must not be null");

		final Map<String, Object> auditedData = new HashMap<>(3);
		auditedData.put(TASK_DEFINITION_NAME, scheduleRequest.getDefinition().getName());

		if (scheduleRequest.getDefinition().getProperties() != null) {
			auditedData.put(TASK_DEFINITION_PROPERTIES,
					argumentSanitizer.sanitizeProperties(scheduleRequest.getDefinition().getProperties()));
		}

		if (scheduleRequest.getDeploymentProperties() != null) {
			auditedData.put(DEPLOYMENT_PROPERTIES,
					argumentSanitizer.sanitizeProperties(scheduleRequest.getDeploymentProperties()));
		}

		if (scheduleRequest.getCommandlineArguments() != null) {
			auditedData.put(COMMANDLINE_ARGUMENTS,
					argumentSanitizer.sanitizeArguments(scheduleRequest.getCommandlineArguments()));
		}
		return auditedData;
	}
}
