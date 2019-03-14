/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.rest.support.jackson;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.springframework.batch.item.ExecutionContext;

/**
 * Jackson MixIn for {@link ExecutionContext} de-serialization.
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
@JsonDeserialize(using = ExecutionContextDeserializer.class)
public abstract class ExecutionContextJacksonMixIn {

	@JsonProperty
	abstract boolean isEmpty();

	@JsonProperty("values")
	abstract Set<Map.Entry<String, Object>> entrySet();
}
