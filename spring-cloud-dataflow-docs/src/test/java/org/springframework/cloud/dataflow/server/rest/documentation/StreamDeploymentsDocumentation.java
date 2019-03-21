/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.rest.documentation;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.springframework.http.MediaType;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StreamDeploymentsDocumentation extends BaseDocumentation {

	private static boolean setUpIsDone = false;

	@Before
	public void setup() throws Exception {
		if (setUpIsDone) {
			return;
		}

		this.mockMvc.perform(
				post("/apps/{type}/time", "source")
						.param("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE"))
				.andExpect(status().isCreated());
		this.mockMvc.perform(
				post("/apps/{type}/log", "sink")
						.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE"))
				.andExpect(status().isCreated());
		this.mockMvc.perform(
				post("/streams/definitions")
						.param("name", "timelog")
						.param("definition", "time --format='YYYY MM DD' | log")
						.param("deploy", "false"))
				.andExpect(status().isCreated());
		setUpIsDone = true;
	}

	@Test
	public void unDeploy() throws Exception {
		this.mockMvc.perform(
				delete("/streams/deployments/{timelog}", "timelog"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog")
								.description("The name of an existing stream definition (required)"))
				));
	}

	@Test
	public void unDeployAll() throws Exception {
		this.mockMvc.perform(
				delete("/streams/deployments"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document());
	}

	@Test
	public void deploy() throws Exception {
		String json = "{\"app.time.timestamp.format\":\"YYYY\"}";
		this.mockMvc.perform(
				post("/streams/deployments/{timelog}", "timelog")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andDo(print())
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog")
								.description("The name of an existing stream definition (required)"))
				));
	}
}
