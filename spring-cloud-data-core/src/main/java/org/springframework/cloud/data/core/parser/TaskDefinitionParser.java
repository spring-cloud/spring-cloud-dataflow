/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.cloud.data.core.parser;

import org.springframework.cloud.data.core.TaskDefinition;
import org.springframework.cloud.data.core.dsl.ArgumentNode;
import org.springframework.cloud.data.core.dsl.ModuleNode;
import org.springframework.cloud.data.core.dsl.TaskDslParser;

/**
 * @author Michael Minella
 */
public class TaskDefinitionParser {

	/**
	 * Parse the provided task DSL task and return a
	 * {@link TaskDefinition task definition} that define
	 * the task.
	 *
	 * @param name  task name
	 * @param dsl   task DSL text
	 * @return task definition
	 */
	public TaskDefinition parse(String name, String dsl) {
		TaskDslParser parser = new TaskDslParser();
		return buildModuleDefinitions(name, parser.parse(name, dsl));
	}

	/**
	 * Build TaskDefinition out of a parsed ModuleNode.
	 *
	 * @param name the name of the definition unit
	 * @param taskNode the AST construct representing the definition
	 */
	private TaskDefinition buildModuleDefinitions(String name, ModuleNode taskNode) {
		TaskDefinition.Builder builder =
					new TaskDefinition.Builder()
						.setTaskName(name)
						.setTask(taskNode.getLabelName());
		if (taskNode.hasArguments()) {
			ArgumentNode[] arguments = taskNode.getArguments();
			for (ArgumentNode argument : arguments) {
				builder.setParameter(argument.getName(), argument.getValue());
			}
		}

		return builder.build();
	}
}
