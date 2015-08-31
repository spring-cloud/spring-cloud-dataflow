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
package org.springframework.cloud.data.module.deployer.lattice;

import org.cloudfoundry.receptor.commands.TaskResponse;

import org.springframework.cloud.data.module.ModuleStatus;

/**
 * @author Michael Minella
 */
public class ReceptorTaskStatusMapper {

	public ModuleStatus.State map(TaskResponse taskResponse) {
		ModuleStatus.State state;

		switch (taskResponse.getState()) {
			case "PENDING":
				state = ModuleStatus.State.deploying;
				break;
			case "CLAIMED":
				state = ModuleStatus.State.deploying;
				break;
			case "RUNNING":
				//TODO: Add support for canceling
				state = ModuleStatus.State.deployed;
				break;
			case "COMPLETED":
				//TODO: Add support for canceled
				if(taskResponse.isFailed()) {
					state = ModuleStatus.State.failed;
				}
				else {
					state = ModuleStatus.State.complete;
				}
				break;
			default:
				state = ModuleStatus.State.unknown;
		}

		return state;
	}
}
