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

package org.springframework.cloud.dataflow.core;

import java.util.Map;

import org.springframework.cloud.dataflow.core.dsl.ArgumentNode;
import org.springframework.cloud.dataflow.core.dsl.ModuleNode;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.core.style.ToStringCreator;

/**
 * @author Michael Minella
 * @author Mark Fisher
 */
public class TaskDefinition {

	/**
	 * Name of module.
	 */
	private final String name;

	/**
	 * DSL text for the module.
	 */
	private final String dslText;

	private final ModuleDefinition moduleDefinition;

	public TaskDefinition(String name, String dsl) {
		this.name = name;
		this.dslText = dsl;
		ModuleNode taskNode = new TaskParser(name, dsl).parse();
		ModuleDefinition.Builder builder = new ModuleDefinition.Builder()
				.setGroup(name)
				.setLabel(taskNode.getLabelName())
				.setName(taskNode.getName());

		if (taskNode.hasArguments()) {
			for (ArgumentNode argumentNode : taskNode.getArguments()) {
				builder.setParameter(argumentNode.getName(), argumentNode.getValue());
			}
		}
		this.moduleDefinition = builder.build();
	}

	public String getName() {
		return name;
	}

	public String getDslText() {
		return dslText;
	}

	public ModuleDefinition getModuleDefinition() {
		return this.moduleDefinition;
	}

	/**
	 * Return parameters for module. This is specific to the type of module - for
	 * instance a filejdbc module would contain a file name as a parameter.
	 *
	 * @return read-only map of module parameters
	 */
	public Map<String, String> getParameters() {
		return this.moduleDefinition.getParameters();
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("name", this.name)
				.append("dslText", this.dslText)
				.append("parameters", this.moduleDefinition.getParameters()).toString();
	}
}
