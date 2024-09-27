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
package org.springframework.cloud.dataflow.server.controller.support;

import java.util.Set;

import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;

/**
 * This enum is used by the {@link TaskExecutionController#cleanup(Set, TaskExecutionControllerDeleteAction[])}.
 *
 * @author Gunnar Hillert
 *
 */
public enum TaskExecutionControllerDeleteAction {

	/**
	 * Cleanup resources associated with one or more task executions.
	 */
	CLEANUP,

	/**
	 * Removed task execution and job execution data from the persistence store.
	 */
	REMOVE_DATA
}
