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

package org.springframework.cloud.dataflow.server.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.dataflow.core.dsl.TaskValidationException;
import org.springframework.cloud.dataflow.core.dsl.TaskValidationProblem;
import org.springframework.cloud.dataflow.core.dsl.graph.Graph;
import org.springframework.cloud.dataflow.rest.resource.TaskToolsResource;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	private final static String TASK_NAME = "name";

	private final static String TASK_DEFINITION = "dsl";

	private TaskToolsAssembler taskGraphAssembler = new TaskToolsAssembler();

	private TaskDslAssembler taskDslAssembler = new TaskDslAssembler();

	/**
	 * Parse a task definition into a graph structure. The definition map is expected to
	 * have a 'dsl' key containing the composed task DSL and a 'name' key indicating the
	 * name of the composed task.
	 *
	 * @param definition the map containing the task definition DSL and task name
	 * @return a resource with the graph property set
	 */
	@PostMapping("/parseTaskTextToGraph")
	public TaskToolsResource parseTaskTextToGraph(@RequestBody Map<String, String> definition) {
		Graph graph = null;
		List<Map<String, Object>> errors = new ArrayList<>();
		try {
			TaskParser taskParser = new TaskParser(definition.get(TASK_NAME), definition.get(TASK_DEFINITION), true,
					true);
			graph = taskParser.parse().toGraph();
		}
		catch (ParseException pe) {
			errors.add(pe.toExceptionDescriptor());
		}
		catch (TaskValidationException tve) {
			for (TaskValidationProblem problem : tve.getValidationProblems()) {
				errors.add(problem.toExceptionDescriptor());
			}
		}
		return taskGraphAssembler.toModel(new ParsedGraphOutput(graph, errors));
	}

	/**
	 * Convert a graph format into DSL text format.
	 *
	 * @param graph the Flo Graph
	 * @return a resource with the dsl property set
	 */
	@PostMapping("/convertTaskGraphToText")
	public TaskToolsResource convertTaskGraphToText(@RequestBody Graph graph) {
		String dsl = null;
		List<Map<String, Object>> errors = new ArrayList<>();
		try {
			dsl = graph.toDSLText();
		}
		catch (ParseException pe) {
			errors.add(pe.toExceptionDescriptor());
		}
		return taskDslAssembler.toModel(new GraphToDslOutput(dsl, errors));
	}

	private static class ParsedGraphOutput {
		final Graph graph;

		final List<Map<String, Object>> errors;

		public ParsedGraphOutput(Graph graph, List<Map<String, Object>> errors) {
			this.graph = graph;
			this.errors = errors;
		}
	}

	private static class GraphToDslOutput {
		final String dsl;

		final List<Map<String, Object>> errors;

		public GraphToDslOutput(String dsl, List<Map<String, Object>> errors) {
			this.dsl = dsl;
			this.errors = errors;
		}
	}

	/**
	 * {@link org.springframework.hateoas.server.ResourceAssembler} implementation that converts
	 * a {@link ParsedGraphOutput} to a {@link TaskToolsResource}.
	 */
	static class TaskToolsAssembler extends RepresentationModelAssemblerSupport<ParsedGraphOutput, TaskToolsResource> {

		public TaskToolsAssembler() {
			super(ToolsController.class, TaskToolsResource.class);
		}

		@Override
		public TaskToolsResource toModel(ParsedGraphOutput graph) {
			return new TaskToolsResource(graph.graph, graph.errors);
		}
	}

	/**
	 * {@link org.springframework.hateoas.server.ResourceAssembler} implementation that converts
	 * a {@link GraphToDslOutput} to a {@link TaskToolsResource}.
	 */
	static class TaskDslAssembler extends RepresentationModelAssemblerSupport<GraphToDslOutput, TaskToolsResource> {

		public TaskDslAssembler() {
			super(ToolsController.class, TaskToolsResource.class);
		}

		@Override
		public TaskToolsResource toModel(GraphToDslOutput output) {
			return new TaskToolsResource(output.dsl, output.errors);
		}
	}

}
