/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

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
	
	private Map<String,Object> error;

	public TaskToolsResource() {
	}

	public TaskToolsResource(Graph graph, Map<String,Object> error) {
		this.graph = graph;
		this.dsl = null;
		this.error = error;
	}

	public TaskToolsResource(String dsl, Map<String,Object> error) {
		this.graph = null;
		this.dsl = dsl;
		this.error = error;
	}

	public Graph getGraph() {
		return graph;
	}
	
	public String getDsl() {
		return dsl;
	}
	
	public Map<String,Object> getError() {
		return error;
	}

}
