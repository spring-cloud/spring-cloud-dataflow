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
 * @author Gunnar Hillert
 */
public class CannotDeleteNonParentTaskExecutionException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new exception.
	 *
	 * @param id the id of the {@link TaskExecution} that could not be found
	 */
	public CannotDeleteNonParentTaskExecutionException(long id) {
		super("Cannot delete non-parent TaskExecution with id " + id);
	}

	/**
	 * Create a new exception.
	 *
	 * @param ids the ids of the {@link TaskExecution} that could not be found
	 */
	public CannotDeleteNonParentTaskExecutionException(Set<Long> ids) {
		super("Cannot delete non-parent TaskExecutions with the following ids: " + StringUtils.collectionToDelimitedString(ids, ", "));
	}
}
