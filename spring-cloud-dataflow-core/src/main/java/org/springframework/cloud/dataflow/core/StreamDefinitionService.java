/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.LinkedList;

import org.springframework.cloud.dataflow.core.dsl.StreamNode;

/**
 * Contract for the core operations against {@link StreamDefinition}.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface StreamDefinitionService {

	/**
	 * Parse the given stream definition and return the AST representation of the stream DSL in {@link StreamNode}.
	 *
	 * @param streamDefinition the stream definition
	 * @return the StreamNode representation of the stream definition
	 */
	StreamNode parse(StreamDefinition streamDefinition);

	/**
	 * Return the linked list of {@link StreamAppDefinition}s associated with the stream definition.
	 *
	 * @param streamDefinition the stream definition
	 * @return the linked list of stream app definitions
	 */
	LinkedList<StreamAppDefinition> getAppDefinitions(StreamDefinition streamDefinition);
}
