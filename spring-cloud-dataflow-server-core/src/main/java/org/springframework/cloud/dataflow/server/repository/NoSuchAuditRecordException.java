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
 * Thrown when an audit record of a given id was expected but did not exist.
 *
 * @author Gunnar Hillert
 */
public class NoSuchAuditRecordException extends RuntimeException {

	public NoSuchAuditRecordException(Long id) {
		super(String.format("Could not find audit record for id %s", id));
	}
}
