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
 * Documentation for the /tasks/executions endpoint.
 *
 * @author Eric Bottard
 */
public class TaskExecutionsDocumentation extends BaseDocumentation {

	@Before
	public void setup() throws Exception {
		this.mockMvc.perform(
				post("/apps/task/timestamp")
						.param("uri", "maven://org.springframework.cloud.task.app:timestamp-task:1.1.0.RELEASE"))
				.andExpect(status().isCreated());
		this.mockMvc.perform(
				post("/tasks/definitions")
						.param("name", "my-task")
						.param("definition", "timestamp --format='yyyy MM dd'"))
				.andExpect(status().isOk());
	}

	@Test
	public void launchTask() throws Exception {
		this.mockMvc.perform(
				post("/tasks/executions")
						.param("name", "my-task")
						.param("properties", "app.my-task.foo=bar,deployer.my-task.something-else=3")
						.param("arguments", "--server.port=8080,--foo=bar"))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("name").description("The name of the task definition to launch"),
								parameterWithName("properties").optional()
										.description("Application and Deployer properties to use while launching"),
								parameterWithName("arguments").optional()
										.description("Command line arguments to pass to the task"))));
	}

}
