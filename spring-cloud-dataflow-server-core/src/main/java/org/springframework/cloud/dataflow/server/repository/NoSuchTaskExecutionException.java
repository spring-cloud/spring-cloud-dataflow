/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.cloud.task.repository.TaskExecution;

/**
 * @author Glenn Renfro
 */
public class NoSuchTaskExecutionException extends RuntimeException{

	/**
	 * Create a new exception.
	 *
	 * @param id the id of the {@link TaskExecution} that could not be found
	 */
	public NoSuchTaskExecutionException(long id) {
		super("Could not find TaskExecution with id " + id);
	}
}
