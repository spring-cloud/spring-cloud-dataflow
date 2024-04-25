/*
 * Copyright 2019 the original author or authors.
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

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the /tasks/platforms endpoint.
 *
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodName.class)
public class TaskPlatformDocumentation extends BaseDocumentation {

	@Test
	public void listTaskPlatforms() throws Exception {
		this.mockMvc.perform(
				get("/tasks/platforms")
						.param("page", "0")
						.param("size", "10"))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						requestParameters(
								parameterWithName("page")
										.description("The zero-based page number (optional)"),
								parameterWithName("size")
										.description("The requested page size (optional)")),
						responseFields(
								subsectionWithPath("_embedded.launcherResourceList")
										.description("Contains a collection of Platform accounts for tasks"),
								subsectionWithPath("_links.self").description("Link to the platform account"),
								subsectionWithPath("page").description("Pagination properties"))));
	}
}
