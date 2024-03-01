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

package org.springframework.cloud.dataflow.server.repository;

import java.util.Set;

import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.util.StringUtils;

/**
 * This exception is used when requesting a {@link TaskExecution} that does not exist.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
public class NoSuchTaskExecutionException extends RuntimeException {

	private static final long serialVersionUID = -303914974762305117L;

	/**
	 * Create a new exception that can handle a single {@link TaskExecution} id.
	 *
	 * @param id the id of the {@link TaskExecution} that could not be found
	 */
	public NoSuchTaskExecutionException(long id) {
		super("Could not find TaskExecution with id " + id);
	}
	public NoSuchTaskExecutionException(String externalExecutionId, String platform) {
		super("Could not find TaskExecution with id " + externalExecutionId + " for platform " + platform);
	}

	/**
	 * Create a new exception that handles multiple {@link TaskExecution} ids.
	 *
	 * @param ids the ids of the {@link TaskExecution} that could not be found
	 */
	public NoSuchTaskExecutionException(Set<Long> ids) {
		super("Could not find TaskExecutions with the following ids: " + StringUtils.collectionToDelimitedString(ids, ", "));
	}
}
