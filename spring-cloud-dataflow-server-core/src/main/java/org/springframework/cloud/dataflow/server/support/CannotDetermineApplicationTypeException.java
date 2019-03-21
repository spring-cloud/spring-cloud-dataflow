/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.support;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.server.DataFlowServerUtil;

/**
 * Exception is thrown by {@link DataFlowServerUtil} to indicate that the
 * {@link ApplicationType} for a provided {@link StreamAppDefinition} cannot be
 * determined.
 *
 * @author Gunnar Hillert
 * @deprecated as of 1.7.  ApplicationType determined at parse time.
 */
@Deprecated
public class CannotDetermineApplicationTypeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CannotDetermineApplicationTypeException(String message) {
		super(message);
	}
}
