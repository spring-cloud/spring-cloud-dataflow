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

package org.springframework.cloud.dataflow.server.rest.documentation;

import org.junit.Before;
import org.junit.Test;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /tasks/definitions endpoint.
 *
 * @author Eric Bottard
 */
public class TaskDefinitionsDocumentation extends BaseDocumentation {

	@Before
	public void setup() throws Exception {
		this.mockMvc.perform(
				post("/apps/task/timestamp")
						.param("uri", "maven://org.springframework.cloud.task.app:timestamp-task:1.1.0.RELEASE"))
				.andExpect(status().isCreated());
	}

	@Test
	public void createDefinition() throws Exception {
		this.mockMvc.perform(
				post("/tasks/definitions")
						.param("name", "my-task")
						.param("definition", "timestamp --format='YYYY MM DD'"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("name").description("The name for the created task definitions"),
								parameterWithName("definition")
										.description("The definition for the task, using Data Flow DSL"))));
	}
}
