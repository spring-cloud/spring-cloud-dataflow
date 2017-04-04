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
 *
 *
 */
package org.springframework.cloud.dataflow.server.controller;

import java.util.Map;

import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.core.dsl.graph.Graph;
import org.springframework.cloud.dataflow.rest.resource.TaskToolsResource;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller for integrating with frontend tools.
 *
 * @author Andy Clement
 */
@RestController
@RequestMapping("/tools")
@Validated
@ExposesResourceFor(TaskToolsResource.class)
public class ToolsController {
	
	private TaskToolsAssembler taskGraphAssembler = new TaskToolsAssembler();
	
	private TaskDslAssembler taskDslAssembler = new TaskDslAssembler();
	
	/**
	 * Parse a task definition into a graph structure. The definition map is expected to have
	 * a 'dsl' key containing the composed task DSL and optionally a 'name' key indicating the name of the
	 * composed task.
	 * 
	 * @return a map with either a 'graph' or 'error' key set 
	 */
	@RequestMapping(value = "/parseTaskTextToGraph", method = RequestMethod.GET)
	public TaskToolsResource parseTaskTextToGraph(@RequestParam(value = "name", defaultValue = "__dummy") String name,
			@RequestParam(value = "definition", required = true) String dsl) {
		Graph graph = null;
		Map<String,Object> error = null;
		try {
			TaskParser taskParser = new TaskParser(name, dsl, true, true);
			graph = taskParser.parse().toGraph();
		}
		catch (ParseException pe) {
			error = pe.toExceptionDescriptor();
		}
		return taskGraphAssembler.toResource(new ParsedGraphOutput(graph,error));
	}
	
	/**
	 * Convert a graph format into DSL text format.
	 */
	@RequestMapping(value = "/convertTaskGraphToText", method = RequestMethod.GET)
	public TaskToolsResource convertTaskGraphToText(@RequestBody Graph graph) {
		String dsl = null;
		Map<String,Object>  error = null;
		try {
			dsl = graph.toDSLText();
		}
		catch (ParseException pe) {
			error = pe.toExceptionDescriptor();
		}
		return taskDslAssembler.toResource(new GraphToDslOutput(dsl, error));
	}

	private static class ParsedGraphOutput {
		final Graph graph;
		
		final Map<String,Object> error;
		
		public ParsedGraphOutput(Graph graph, Map<String, Object> error) {
			this.graph = graph;
			this.error = error;
		}
	}

	private static class GraphToDslOutput {
		final String dsl;
		
		final Map<String,Object> error;
		
		public GraphToDslOutput(String dsl, Map<String, Object> error) {
			this.dsl = dsl;
			this.error = error;
		}
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts a {@link ParsedGraphOutput} to a {@link TaskToolsResource}.
	 */
	static class TaskToolsAssembler extends ResourceAssemblerSupport<ParsedGraphOutput, TaskToolsResource> {

		public TaskToolsAssembler() {
			super(ToolsController.class, TaskToolsResource.class);
		}

		@Override
		public TaskToolsResource toResource(ParsedGraphOutput graph) {
			return new TaskToolsResource(graph.graph, graph.error);
		}
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts a {@link GraphToDslOutput} to a {@link TaskToolsResource}.
	 */
	static class TaskDslAssembler extends ResourceAssemblerSupport<GraphToDslOutput, TaskToolsResource> {

		public TaskDslAssembler() {
			super(ToolsController.class, TaskToolsResource.class);
		}

		@Override
		public TaskToolsResource toResource(GraphToDslOutput output) {
			return new TaskToolsResource(output.dsl, output.error);
		}
	}

}
