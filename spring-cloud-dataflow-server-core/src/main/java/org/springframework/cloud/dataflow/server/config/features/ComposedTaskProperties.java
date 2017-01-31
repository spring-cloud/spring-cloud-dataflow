/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config.features;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Glenn Renfro
 */
@ConfigurationProperties(prefix = ComposedTaskProperties.COMPOSED_TASK_PREFIX)
public class ComposedTaskProperties {

	public static final String COMPOSED_TASK_PREFIX = "spring.cloud.dataflow.composed.task";

	private URI composedTaskRunnerUri;

	private String taskName = "composed-task-runner";

	public ComposedTaskProperties() {
		try {
			composedTaskRunnerUri = new URI("maven://org.springframework.cloud.task.app:composedtaskrunner-task:1.0.0.BUILD-SNAPSHOT");
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(
					"Invalid URI specified for composedTaskRunnerUri");
		}
	}

	public URI getComposedTaskRunnerUri() {
		return composedTaskRunnerUri;
	}

	public void setComposedTaskRunnerUri(URI composedTaskRunnerUri) {
			this.composedTaskRunnerUri = composedTaskRunnerUri;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

}
