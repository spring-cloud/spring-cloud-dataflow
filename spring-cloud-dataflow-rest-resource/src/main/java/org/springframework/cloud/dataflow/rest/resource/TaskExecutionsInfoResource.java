/*
 * Copyright 2021 the original author or authors.
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

import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;

/**
 * An HATEOAS representation of a TaskExecutionsResource.
 *
 * @author Ilayaperumal Gopinathan
 */
public class TaskExecutionsInfoResource extends RepresentationModel<TaskExecutionsInfoResource> {

	/**
	 * The number of task executions
	 */
	private Integer totalExecutions;

	public Integer getTotalExecutions() {
		return totalExecutions;
	}

	public void setTotalExecutions(Integer totalExecutions) {
		this.totalExecutions = totalExecutions;
	}

	public static class Page extends PagedModel<TaskExecutionsInfoResource> {
	}
}
