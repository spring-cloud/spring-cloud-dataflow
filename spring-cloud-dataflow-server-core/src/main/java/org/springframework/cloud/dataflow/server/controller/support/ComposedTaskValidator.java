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

package org.springframework.cloud.dataflow.server.controller.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.ComposedTaskVisitor;
import org.springframework.cloud.dataflow.core.dsl.TaskAppNode;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.util.Assert;

/**
 * Identifies all task names that do not have a task definition in the
 * task repository.
 *
 * @author Glenn Renfro
 */
public class ComposedTaskValidator extends ComposedTaskVisitor {

	private TaskDefinitionRepository repository;

	private Map<String, Integer> invalidDefinitions = new HashMap<>();

	Set<String> availableDefinitionNames;

	public ComposedTaskValidator(TaskDefinitionRepository repository) {
		Assert.notNull(repository, "repository must not be null");
		this.repository = repository;
		availableDefinitionNames = new HashSet();

		Stream<TaskDefinition> taskDefinitionStream =
				StreamSupport.stream(repository.findAll().spliterator(), false);
		taskDefinitionStream.forEach(taskDefinition ->
				availableDefinitionNames.add(taskDefinition.getName()));
	}

	@Override
	public void visit(TaskAppNode taskApp) {
		if (!availableDefinitionNames.contains(taskApp.getName())) {
			invalidDefinitions.put(taskApp.getName(), taskApp.getStartPos());
		}
	}

	public Map<String, Integer> getInvalidDefinitions() {
		return invalidDefinitions;
	}
}
