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

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Retrieves logs of task applications.
 *
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/tasks/logs")
public class TaskLogsController {

	private final TaskExecutionService taskExecutionService;

	/**
	 * Construct Task logs controller.
	 *
	 * @param taskExecutionService the task execution service that this controller uses to get the logs of
	 * launched task applications.
	 */
	public TaskLogsController(TaskExecutionService taskExecutionService) {
		Assert.notNull(taskExecutionService, "TaskExecutionService must not be null");
		this.taskExecutionService = taskExecutionService;
	}

	/**
	 * Retrieve logs for the task execution identified by the provided external execution ID
	 * @param executionId the execution ID used to retrieve the log.
	 * @param platformName the platform name
	 * @param idType identifies whether the log should be obtained using the {@code externalExecutionId} or the {@code taskExecutionId} created by SCDF.
	 * The default is "external" which means the method will use the {@code externalExecutionId}.  If you wish to use the {@code taskExecutionId} created by SCDF then set {@code idType} to "internal".
	 * @return the log content represented as String
	 */
	@RequestMapping(value = "/{executionId}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<String> getLog(@PathVariable String executionId, @RequestParam(required = false, defaultValue = "default") String platformName, @RequestParam(required = false, defaultValue = "external") String idType) {
		ResponseEntity<String> result;
		if(idType.equals("external")) {
			result = new ResponseEntity<>(this.taskExecutionService.getLog(platformName, executionId), HttpStatus.OK);
		} else if(idType.equals("internal")) {
			result = new ResponseEntity<>(this.taskExecutionService.getLog(Long.valueOf(executionId)), HttpStatus.OK);
		} else {
			throw new IllegalArgumentException("Invalid idType specified.");
		}
		return result;
	}
}
