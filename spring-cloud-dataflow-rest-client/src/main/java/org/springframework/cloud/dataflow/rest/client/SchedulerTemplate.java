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

package org.springframework.cloud.dataflow.rest.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.ScheduleInfoResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for
 * {@link org.springframework.cloud.dataflow.rest.client.SchedulerOperations}.
 *
 * @author Glenn Renfro
 */
public class SchedulerTemplate implements SchedulerOperations {

	/* default */ public static final String SCHEDULES_RELATION = "tasks/schedules";

	private static final String SCHEDULES_INSTANCE_RELATION = SCHEDULES_RELATION + "/instances";


	private final RestTemplate restTemplate;

	private final Link schedulesLink;

	private final Link schedulesInstanceLink;

	SchedulerTemplate(RestTemplate restTemplate, RepresentationModel<?> resources) {
		Assert.notNull(resources, "URI CollectionModel must not be be null");
		Assert.notNull(resources.getLink(SCHEDULES_RELATION), "Schedules relation is required");
		Assert.notNull(resources.getLink(SCHEDULES_INSTANCE_RELATION), "Schedules instance relation is required");
		Assert.notNull(restTemplate, "RestTemplate must not be null");

		this.restTemplate = restTemplate;
		this.schedulesLink = resources.getLink(SCHEDULES_RELATION).get();
		this.schedulesInstanceLink = resources.getLink(SCHEDULES_INSTANCE_RELATION).get();

	}

	@Override
	public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs, String platform) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("scheduleName", scheduleName);
		values.add("properties", DeploymentPropertiesUtils.format(taskProperties));
		values.add("taskDefinitionName", taskDefinitionName);
		String commandLineArgValue = new String();
		boolean isFirst = true;
		for(String command : commandLineArgs) {
			if(isFirst) {
				isFirst = false;
			}
			else {
				commandLineArgValue += " ";
			}
			commandLineArgValue += command;
		}
		values.add("arguments", commandLineArgValue);
		if(StringUtils.hasText(platform)) {
			values.add("platform", platform);
		}
		restTemplate.postForObject(schedulesLink.getHref(), values, Long.class);
	}

	@Override
	public void schedule(String scheduleName, String taskDefinitionName, Map<String, String> taskProperties, List<String> commandLineArgs) {
		schedule(scheduleName, taskDefinitionName, taskProperties, commandLineArgs, null);
	}

	@Override
	public void unschedule(String scheduleName, String platform) {
		String url = schedulesLink.getHref() + "/" + scheduleName;
		if(platform != null) {
			url = url + "?platform=" + platform;
		}
		restTemplate.delete(url);
	}

	@Override
	public void unschedule(String scheduleName) {
		unschedule(scheduleName, null);
	}

	@Override
	public PagedModel<ScheduleInfoResource> list(String taskDefinitionName, String platform) {
		String url = schedulesInstanceLink.expand(taskDefinitionName).getHref();
		if(platform != null) {
			url = url + "?platform=" + platform;
		}
		return restTemplate.getForObject(url , ScheduleInfoResource.Page.class);
	}

	@Override
	public PagedModel<ScheduleInfoResource> list(String taskDefinitionName) {
		return list(taskDefinitionName, null);
	}

	@Override
	public PagedModel<ScheduleInfoResource> listByPlatform(String platform) {
		String url = schedulesLink.getHref();
		if(platform != null) {
			url = url + "?platform=" + platform;
		}
		return restTemplate.getForObject(url, ScheduleInfoResource.Page.class);
	}

	@Override
	public PagedModel<ScheduleInfoResource> list() {
		return listByPlatform(null);
	}

	@Override
	public ScheduleInfoResource getSchedule(String scheduleName, String platform) {
		String url = schedulesLink.getHref() + "/" + scheduleName;
		if(platform != null) {
			url = url + "?platform=" + platform;
		}
		return restTemplate.getForObject(url, ScheduleInfoResource.class);
	}

	@Override
	public ScheduleInfoResource getSchedule(String scheduleName) {
		return getSchedule(scheduleName, null);
	}
}
