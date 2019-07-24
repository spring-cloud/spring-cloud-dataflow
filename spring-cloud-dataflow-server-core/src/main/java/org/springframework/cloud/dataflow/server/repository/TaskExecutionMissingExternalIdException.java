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

package org.springframework.cloud.dataflow.server.repository;

import java.util.Set;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.StringUtils;

/**
 * This exception is used when {@link TaskExecution}s do not have external
 * execution ids and they are required by Spring Cloud Data Flow.
 *
 * @author Glenn Renfro
 */
public class TaskExecutionMissingExternalIdException extends RuntimeException {

	private static final long serialVersionUID = -303914974762305117L;

	/**
	 * Create a new exception that can handle a single {@link TaskExecution} id.
	 *
	 * @param id the id of the {@link TaskExecution} that is missing external execution id.
	 */
	public TaskExecutionMissingExternalIdException(long id) {
		super(String.format("The TaskExecution with id %s is missing the external execution id", id));
	}

	/**
	 * Create a new exception that handles multiple {@link TaskExecution} ids.
	 *
	 * @param ids the ids of the {@link TaskExecution} that are missing external execution ids.
	 */
	public TaskExecutionMissingExternalIdException(Set<Long> ids) {
		super("The TaskExecutions with the following ids: " + StringUtils.collectionToDelimitedString(ids, ", ") + " do not have external execution ids.");
	}
}
