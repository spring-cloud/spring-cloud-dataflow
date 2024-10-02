/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller.assembler;

import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.util.Assert;

/**
 * Default REST resource assembler that returns the {@link TaskDefinitionResource} type.
 *
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
public class DefaultTaskDefinitionAssemblerProvider implements TaskDefinitionAssemblerProvider<TaskDefinitionResource> {

	private final TaskExecutionService taskExecutionService;

	private final DataflowTaskExplorer taskExplorer;

	private final TaskJobService taskJobService;

	public DefaultTaskDefinitionAssemblerProvider(
			TaskExecutionService taskExecutionService,
			TaskJobService taskJobService,
			DataflowTaskExplorer taskExplorer
	) {
		Assert.notNull(taskExecutionService, "taskExecutionService required");
		Assert.notNull(taskJobService, "taskJobService required");
		Assert.notNull(taskExplorer, "taskExplorer required");
		this.taskExecutionService = taskExecutionService;
		this.taskJobService = taskJobService;
		this.taskExplorer = taskExplorer;
	}

	@Override
	public DefaultTaskDefinitionAssembler getTaskDefinitionAssembler(boolean enableManifest) {
		return new DefaultTaskDefinitionAssembler(taskExecutionService, enableManifest,
				TaskDefinitionResource.class, taskJobService, taskExplorer);
	}
}
