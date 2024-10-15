/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller;

import java.net.URI;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.server.service.impl.ComposedTaskRunnerConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to expose boot configuration metadata from an ctr app as it's now
 * just defined as an property and thus its metadata cannot be resolved via
 * registered apps. This is essentially a support endpoint for UI features.
 *
 * @author Janne Valkealahti
 */
@RestController
@RequestMapping("/tasks/ctr")
public class TaskCtrController {

	private final ApplicationConfigurationMetadataResolver metadataResolver;
	private final TaskConfigurationProperties taskConfigurationProperties;
	private final ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;
	private final AppResourceCommon appResourceCommon;

	public TaskCtrController(ApplicationConfigurationMetadataResolver metadataResolver,
			TaskConfigurationProperties taskConfigurationProperties,
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties,
			AppResourceCommon appResourceCommon) {
		this.metadataResolver = metadataResolver;
		this.taskConfigurationProperties = taskConfigurationProperties;
		this.composedTaskRunnerConfigurationProperties = composedTaskRunnerConfigurationProperties;
		this.appResourceCommon = appResourceCommon;
	}

	@GetMapping("/options")
	@ResponseStatus(HttpStatus.OK)
	public List<ConfigurationMetadataProperty> options() {
		URI ctrUri = null;
		try {
			ctrUri = new URI(composedTaskRunnerConfigurationProperties.getUri());
		} catch (Exception e) {
			throw new IllegalStateException("Invalid Compose Task Runner Resource", e);
		}

		Resource resource = this.appResourceCommon.getMetadataResource(ctrUri, null);
		List<ConfigurationMetadataProperty> properties = this.metadataResolver.listProperties(resource);
		return properties;
	}
}
