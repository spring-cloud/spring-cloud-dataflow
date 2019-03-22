/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service;

import java.util.Map;

import org.springframework.cloud.dataflow.core.AllPlatformsTaskExecutionInformation;
import org.springframework.cloud.dataflow.server.service.impl.TaskExecutionInformation;

/**
 * Return the TaskExecutionInformation object given the task name and deployment
 * properties.
 *
 * @author Daniel Serleg
 * @author Mark Pollack
 * @author David Turanski
 */
public interface TaskExecutionInfoService {

	TaskExecutionInformation findTaskExecutionInformation(String taskName,
			Map<String, String> taskDeploymentProperties);

	AllPlatformsTaskExecutionInformation findAllPlatformTaskExecutionInformation();
}
