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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.dataflow.core.dsl.StreamNode;

/**
 * Contract for the core operations against {@link StreamDefinition}.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface StreamDefinitionService {

	public final static List<String> dataFlowAddedProperties = Arrays.asList(
			DataFlowPropertyKeys.STREAM_APP_TYPE,
			DataFlowPropertyKeys.STREAM_APP_LABEL,
			DataFlowPropertyKeys.STREAM_NAME,
			BindingPropertyKeys.INPUT_GROUP,
			BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS,
			BindingPropertyKeys.OUTPUT_DESTINATION,
			BindingPropertyKeys.INPUT_DESTINATION);

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

	/**
	 * Return the updated stream DSL for the given stream definition with the associated properties.
	 *
	 * @param streamAppDefinitions the linked list of {@link StreamAppDefinition}s associated with the stream with some of app properties modified
	 * @return the updated stream DSL
	 */
	String constructDsl(String originalDslText, LinkedList<StreamAppDefinition> streamAppDefinitions);

	String redactDsl(StreamDefinition streamDefinition);
}
