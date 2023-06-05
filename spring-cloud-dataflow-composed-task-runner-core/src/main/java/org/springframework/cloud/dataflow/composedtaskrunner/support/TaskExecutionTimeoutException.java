/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.composedtaskrunner.support;

import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;

/**
 * Exception thrown when a task execution exceeds the configured
 * {@link ComposedTaskProperties#getMaxWaitTime()}.
 *
 * @author Glenn Renfro
 */
public class TaskExecutionTimeoutException extends RuntimeException {

	public TaskExecutionTimeoutException(String message) {
		super(message);
	}

	public TaskExecutionTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
}
