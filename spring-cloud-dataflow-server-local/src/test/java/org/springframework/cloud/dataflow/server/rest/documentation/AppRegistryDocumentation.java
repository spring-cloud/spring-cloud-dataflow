/*
 * Copyright 2016 the original author or authors.
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

import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;

/**
 * @author Gunnar Hillert
 */
public class AppRegistryDocumentation extends BaseDocumentation {

	@ClassRule
	public static TestRule springDataflowServer = localDataflowResource;

	@Rule
	public JUnitRestDocumentation restDocumentation =
			new JUnitRestDocumentation(TARGET_DIRECTORY);

	@Before
	public void setupMocks() {
		super.prepareDocumentationTests(restDocumentation);
	}

	@Test
	public void getApplicationsFiltered() throws Exception {
		this.mockMvc.perform(get("/apps").param("type", "source")
			.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(print())
			.andDo(this.documentationHandler.document(requestParameters(
					parameterWithName("type").description("Restrict the returned apps to the type of the app."))));
	}
}
