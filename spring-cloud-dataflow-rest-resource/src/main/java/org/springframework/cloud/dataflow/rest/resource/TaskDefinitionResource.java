/*
 * Copyright 2015-2018 the original author or authors.
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

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;

/**
 * A HATEOAS representation of a {@link TaskDefinition}.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
public class TaskDefinitionResource extends RepresentationModel<TaskDefinitionResource> {

	private String name;

	private String dslText;

	private String description;

	/**
	 * Indicates whether this is a composed task.
	 */
	private boolean composed;

	/**
	 * Indicates whether this is a composed task element.
	 */
	private boolean composedTaskElement;

	/**
	 * The last execution of this task execution.
	 */
	private TaskExecutionResource lastTaskExecution;

	/**
	 * Default constructor to be used by Jackson.
	 */
	@SuppressWarnings("unused")
	protected TaskDefinitionResource() {
	}

	public TaskDefinitionResource(String name, String dslText, String description) {
		this.name = name;
		this.dslText = dslText;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDslText() {
		return dslText;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Return if this task is a composed task
	 *
	 * @return composed
	 */
	public boolean isComposed() {
		return composed;
	}

	/**
	 * Set if this task is a composed task
	 *
	 * @param composed is task a composed task
	 */
	public void setComposed(boolean composed) {
		this.composed = composed;
	}


	public boolean isComposedTaskElement() {
		return composedTaskElement;
	}

	public void setComposedTaskElement(boolean composedTaskElement) {
		this.composedTaskElement = composedTaskElement;
	}

	/**
	 * Return the status of this task. Returns the value of {@link TaskExecutionStatus#toString()}.
	 *
	 * @return task status
	 */
	public String getStatus() {
		if (this.getLastTaskExecution() == null) {
			return TaskExecutionStatus.UNKNOWN.toString();
		} else {
			return this.getLastTaskExecution().getTaskExecutionStatus().toString();
		}
	}

	/**
	 * @return Last {@link TaskExecution} if available, null otherwise
	 */
	public TaskExecutionResource getLastTaskExecution() {
		return lastTaskExecution;
	}

	/**
	 *
	 * @param lastTaskExecution the last Task Execution
	 */
	public void setLastTaskExecution(TaskExecutionResource lastTaskExecution) {
		this.lastTaskExecution = lastTaskExecution;
	}

	public static class Page extends PagedModel<TaskDefinitionResource> {
	}
}
