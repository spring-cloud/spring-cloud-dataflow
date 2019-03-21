/*
 * Copyright 2018 the original author or authors.
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

/**
 * Exception is thrown if there is no mapping between a batch job execution and
 * a task execution.
 *
 * @author Glenn Renfro
 */
public class NoSuchTaskBatchException extends RuntimeException {

	public NoSuchTaskBatchException(String message) {
		super(message);
	}

}
