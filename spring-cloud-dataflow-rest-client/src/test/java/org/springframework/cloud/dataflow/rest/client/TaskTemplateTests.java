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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test the {@link TaskTemplate} implementation of {@link TaskOperations}.
 *
 * @author Glenn Renfro
 */
public class TaskTemplateTests {

	private static final String CURRENT_TASK_EXECUTION_LINK = "tasks/executions/current";

	private RestTemplate restTemplate;

	@BeforeEach
	public void setup() {
		restTemplate = mock(RestTemplate.class);

	}

	@Test
	public void testOldDataFlow() {
		validateExecutionLinkNotPresent("1.6.0");
	}

	@Test
	public void testMinDataFlow() {
		validateExecutionLinkPresent("1.7.0");
	}

	@Test
	public void testFutureDataFlow() {
		validateExecutionLinkPresent("1.8.0");
		validateExecutionLinkPresent("1.9.0");
		validateExecutionLinkPresent("2.0.0");
	}


	private void validateExecutionLinkPresent(String dataFlowVersion) {
		TestResource testResource = new TestResource();
		new TaskTemplate(this.restTemplate, testResource, dataFlowVersion);
		Assertions.assertTrue(testResource.isLinkRequested(CURRENT_TASK_EXECUTION_LINK));
	}

	private void validateExecutionLinkNotPresent(String version) {
		TestResource testResource = new TestResource();
		new TaskTemplate(this.restTemplate, testResource, version);
		Assertions.assertFalse(testResource.isLinkRequested(CURRENT_TASK_EXECUTION_LINK));
	}

	public static class TestResource extends RepresentationModel {

		private Map<String, Long> linksRequested = new HashMap<>();

		public Optional<Link> getLink(String rel) {
			if (this.linksRequested.containsKey(rel)) {
				Long count = this.linksRequested.get(rel);
				this.linksRequested.put(rel, count + 1L);
			}
			else {
				this.linksRequested.put(rel, 1L);
			}

			return Optional.of(new Link("foo", "bar"));
		}

		public boolean isLinkRequested(String linkName) {
			boolean result = false;

			if (this.linksRequested.containsKey(linkName) &&
					this.linksRequested.get(linkName) > 1L) {
				result = true;
			}
			return result;
		}

	}
}
