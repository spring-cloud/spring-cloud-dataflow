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
package org.springframework.cloud.dataflow.rest.client.dsl;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;

/**
 * Represents the entry point into the Java DSL to set the name of the stream.
 *
 * @author Vinicius Carvalho
 */
public class StreamBuilder {

	private DataFlowOperations dataFlowOperations;

	/**
	 * Construct a new StreamBuilder given a {@link DataFlowOperations} instance.
	 * @param dataFlowOperations the dataflowOperations instance used to communicate to the
	 *     Data Flow server
	 */
	public StreamBuilder(DataFlowOperations dataFlowOperations) {
		this.dataFlowOperations = dataFlowOperations;
	}

	/**
	 * Fluent API method to set the name of the stream.
	 * @param name - The unique identifier of a Stream with the server
	 * @return A {@link Stream.StreamNameBuilder} that provides the next navigation step in
	 * the DSL.
	 */
	public Stream.StreamNameBuilder name(String name) {
		return new Stream.StreamNameBuilder(name, dataFlowOperations);
	}

}
