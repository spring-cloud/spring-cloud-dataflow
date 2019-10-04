/*
 * Copyright 2018 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;

/**
 * A HATEOAS representation of a {@link ScheduleInfo}.
 *
 * @author Glenn Renfro
 */
public class ScheduleInfoResource extends RepresentationModel<ScheduleInfoResource> {

	/**
	 * The name to be associated with the Schedule instance.
	 */
	private String scheduleName;

	/**
	 * The app definition name associated with Schedule instance.
	 */
	private String taskDefinitionName;

	/**
	 * Schedule specific information returned from the Scheduler implementation.
	 */
	private Map<String, String> scheduleProperties = new HashMap<>();

	/**
	 * Default constructor to be used by Jackson.
	 */
	@SuppressWarnings("unused")
	protected ScheduleInfoResource() {
	}

	public ScheduleInfoResource(String scheduleName, String taskDefinitionName, Map<String, String> scheduleProperties) {
		Assert.hasText(scheduleName, "scheduleName must not be null or empty");
		Assert.hasText(taskDefinitionName, "taskDefinitionName must not be null or empty");
		Assert.notNull(scheduleProperties, "schedulerProperties must not be null.");

		this.scheduleName = scheduleName;
		this.scheduleProperties.putAll(scheduleProperties);
		this.taskDefinitionName = taskDefinitionName;
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public String getTaskDefinitionName() {
		return taskDefinitionName;
	}

	public void setTaskDefinitionName(String taskDefinitionName) {
		this.taskDefinitionName = taskDefinitionName;
	}

	public Map<String, String> getScheduleProperties() {
		return Collections.unmodifiableMap(scheduleProperties);
	}

	public void setScheduleProperties(Map<String, String> scheduleProperties) {
		this.scheduleProperties.clear();
		this.scheduleProperties.putAll(scheduleProperties);
	}

	public static class Page extends PagedModel<ScheduleInfoResource> {
	}
}
