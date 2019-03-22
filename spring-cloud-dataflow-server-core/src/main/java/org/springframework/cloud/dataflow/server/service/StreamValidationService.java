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
package org.springframework.cloud.dataflow.server.service;

/**
 * Validate Streams
 *
 * @author Mark Pollack
 */
public interface StreamValidationService extends ValidationService {

	/**
	 * Validate the stream given the registered name
	 * @param name then name of the stream
	 * @return the validation status
	 */
	ValidationStatus validateStream(String name);

}
