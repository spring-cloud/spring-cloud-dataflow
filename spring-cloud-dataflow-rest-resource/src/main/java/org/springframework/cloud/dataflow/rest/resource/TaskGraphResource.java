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
 * Represents a parsed task definition converted to a graph representation for front end tools.
 *
 * @author Andy Clement
 */
public class TaskGraphResource extends ResourceSupport {

	private Graph graph;
	
	private Map<String,Object> error;

	public TaskGraphResource() {
	}

	public TaskGraphResource(Graph graph, Map<String,Object> error) {
		this.graph = graph;
		this.error = error;
	}

	public Graph getGraph() {
		return graph;
	}
	
	public Map<String,Object> getError() {
		return error;
	}

}
