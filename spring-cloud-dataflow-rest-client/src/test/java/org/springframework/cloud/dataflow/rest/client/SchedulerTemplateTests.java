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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.dataflow.rest.resource.RootResource;
import org.springframework.cloud.dataflow.rest.resource.ScheduleInfoResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.hateoas.Link;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
class SchedulerTemplateTests {
	private static final String SCHEDULES_RELATION = org.springframework.cloud.dataflow.rest.client.SchedulerTemplate.SCHEDULES_RELATION;
	private static final String SCHEDULES_RELATION_INSTANCE = SCHEDULES_RELATION + "/instances";
	private static final String DEFAULT_SCHEDULE_NAME = "testSchedule";
	private static final String DEFAULT_DEFINITION_NAME = "testDefName";

	private RootResource rootResource;
	private RestTemplate restTemplate;
	private SchedulerTemplate template;

	@BeforeEach
	void setup() {
		rootResource = mock(RootResource.class);
		when(rootResource.getLink(SCHEDULES_RELATION)).thenReturn(Optional.of(Link.of(SCHEDULES_RELATION)));
		when(rootResource.getLink(SCHEDULES_RELATION_INSTANCE)).thenReturn(Optional.of(Link.of(SCHEDULES_RELATION_INSTANCE)));
		restTemplate = mock(RestTemplate.class);
		template = new SchedulerTemplate(restTemplate, rootResource);
	}

	@Test
	void scheduleTest() {
		verifyControllerResult(null);
	}

	@Test
	void multiPlatformScheduleTest() {
		verifyControllerResult("default");
		verifyControllerResult("foo");
	}

	private void verifyControllerResult(String platform) {
		Map<String, String> props = Collections.singletonMap("Hello", "World");
		List<String> args = Collections.singletonList("args=vals");
		template.schedule(DEFAULT_SCHEDULE_NAME, DEFAULT_DEFINITION_NAME, props, args, platform);

		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("scheduleName", DEFAULT_SCHEDULE_NAME);
		values.add("properties", DeploymentPropertiesUtils.format(props));
		values.add("taskDefinitionName", DEFAULT_DEFINITION_NAME);
		values.add("arguments", args.get(0));
		if(platform != null) {
			values.add("platform", platform);
		}
		Mockito.verify(restTemplate).postForObject(SCHEDULES_RELATION, values, Long.class);
	}

	@Test
	void unScheduleTest() {
		template.unschedule(DEFAULT_SCHEDULE_NAME);
		Mockito.verify(restTemplate).delete(SCHEDULES_RELATION + "/testSchedule");
	}

	@Test
	void unSchedulePlatformTest() {
		template.unschedule(DEFAULT_SCHEDULE_NAME, "foo");
		Mockito.verify(restTemplate).delete(SCHEDULES_RELATION + "/testSchedule?platform=foo");
	}

	@Test
	void unScheduleNullTest() {
		template.unschedule(DEFAULT_SCHEDULE_NAME, null);
		Mockito.verify(restTemplate).delete(SCHEDULES_RELATION + "/testSchedule");
	}

	@Test
	void listTest() {
		template.list();
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION, ScheduleInfoResource.Page.class);
	}

	@Test
	void listByPlatformNullTest() {
		template.listByPlatform(null);
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION, ScheduleInfoResource.Page.class);
	}

	@Test
	void listByPlatformTest() {
		template.listByPlatform("foo");
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION + "?platform=foo", ScheduleInfoResource.Page.class);
	}

	@Test
	void listTaskDefNameTest() {
		template.list(DEFAULT_DEFINITION_NAME);
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION_INSTANCE, ScheduleInfoResource.Page.class);
	}

	@Test
	void listTaskDefNameNullTest() {
		template.list(DEFAULT_DEFINITION_NAME, null);
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION_INSTANCE, ScheduleInfoResource.Page.class);
	}

	@Test
	void listTaskDefNamePlatformTest() {
		template.list(DEFAULT_DEFINITION_NAME, "foo");
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION_INSTANCE + "?platform=foo", ScheduleInfoResource.Page.class);
	}

	@Test
	void getScheduleTest() {
		template.getSchedule(DEFAULT_SCHEDULE_NAME);
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION + "/" + DEFAULT_SCHEDULE_NAME,
				ScheduleInfoResource.class);
	}

	@Test
	void getScheduleNullTest() {
		template.getSchedule(DEFAULT_SCHEDULE_NAME, null);
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION + "/" + DEFAULT_SCHEDULE_NAME,
				ScheduleInfoResource.class);
	}

	@Test
	void getSchedulePlatformTest() {
		template.getSchedule(DEFAULT_SCHEDULE_NAME, "foo");
		Mockito.verify(restTemplate).getForObject(SCHEDULES_RELATION + "/" +
						DEFAULT_SCHEDULE_NAME + "?platform=foo", ScheduleInfoResource.class);
	}
}
