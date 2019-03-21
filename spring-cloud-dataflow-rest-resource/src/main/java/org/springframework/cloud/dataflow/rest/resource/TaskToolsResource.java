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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.dsl.graph.Graph;
import org.springframework.hateoas.ResourceSupport;

/**
 * Represents a response from the tools endpoint. Depending on whether the request was to
 * parse a DSL string into a graph or convert a graph to DSL, the different fields of the
 * response will be filled in.
 *
 * @author Andy Clement
 */
public class TaskToolsResource extends ResourceSupport {

	private Graph graph;

	private String dsl;

	// A list of errors, each entry is a map with keys for position and message text
	private List<Map<String, Object>> errors;

	public TaskToolsResource() {
	}

	public TaskToolsResource(Graph graph, List<Map<String, Object>> errors) {
		this.graph = graph;
		this.dsl = null;
		this.errors = errors;
	}

	public TaskToolsResource(String dsl, List<Map<String, Object>> errors) {
		this.graph = null;
		this.dsl = dsl;
		this.errors = errors;
	}

	public Graph getGraph() {
		return graph;
	}

	public String getDsl() {
		return dsl;
	}

	public List<Map<String, Object>> getErrors() {
		return errors;
	}

}
