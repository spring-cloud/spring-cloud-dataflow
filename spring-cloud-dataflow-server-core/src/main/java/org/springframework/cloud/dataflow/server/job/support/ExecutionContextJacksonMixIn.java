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

package org.springframework.cloud.dataflow.server.job.support;

import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson MixIn for the JSON serialization of the {@link ExecutionContext} class, which
 * is used by the {@link StepExecution} class. By default, meaning without the
 * {@link ExecutionContextJacksonMixIn} applied, Jackson will not render the values of the
 * {@link ExecutionContext}.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public abstract class ExecutionContextJacksonMixIn {

	@JsonProperty("values")
	abstract Set<Map.Entry<String, Object>> entrySet();
}
