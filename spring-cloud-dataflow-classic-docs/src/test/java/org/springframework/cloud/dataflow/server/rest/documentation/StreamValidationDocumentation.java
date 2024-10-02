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

package org.springframework.cloud.dataflow.server.rest.documentation;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Documentation for the /streams/validation endpoint.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodOrderer.MethodName.class)
class StreamValidationDocumentation extends BaseDocumentation {

	private static boolean setUpIsDone = false;

	@BeforeEach
	void setup() throws Exception {
		this.mockMvc.perform(
				post("/apps/{type}/time", "source")
						.param("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE")
						.param("force", "true"))
				.andExpect(status().isCreated());
		this.mockMvc.perform(
				post("/apps/{type}/log", "sink")
						.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE")
						.param("force", "true"))
				.andExpect(status().isCreated());
	}

	@Test
	void validateStream() throws Exception {
		this.mockMvc.perform(
			post("/streams/definitions")
					.param("name", "timelog")
					.param("definition", "time --format='YYYY MM DD' | log")
					.param("description", "Demo stream for testing")
					.param("deploy", "false"))
			.andExpect(status().isCreated());

		this.mockMvc.perform(
			get("/streams/validation/{name}", "timelog"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				pathParameters(
					parameterWithName("name").description("The name of a stream definition to be validated (required)")
				),
				responseFields(
					fieldWithPath("appName").description("The name of a stream definition"),
					fieldWithPath("dsl").description("The dsl of a stream definition"),
					fieldWithPath("description").description("The description of the stream definition"),
					subsectionWithPath("appStatuses").description("The status of the application instances")
				)
			));
	}

}
