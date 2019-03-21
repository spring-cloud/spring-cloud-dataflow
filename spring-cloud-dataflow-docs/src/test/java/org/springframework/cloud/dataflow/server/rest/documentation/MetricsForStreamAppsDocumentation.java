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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import org.springframework.cloud.dataflow.server.local.LocalDataflowResource;
import org.springframework.cloud.dataflow.server.local.metrics.FakeMetricsCollectorResource;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
public class MetricsForStreamAppsDocumentation {

	private final static FakeMetricsCollectorResource fakeMetricsCollectorResource = new FakeMetricsCollectorResource();

	private final static LocalDataflowResource localDataflowResource = new LocalDataflowResource(
			"classpath:rest-docs-config.yml");

	@Rule
	public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation(BaseDocumentation.TARGET_DIRECTORY);

	private MockMvc mockMvc;

	private RestDocumentationResultHandler documentationHandler;

	private void prepareDocumentationTests(WebApplicationContext context) {
		this.documentationHandler = document("{class-name}/{method-name}", preprocessResponse(prettyPrint()));

		this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(documentationConfiguration(this.restDocumentation)).alwaysDo(this.documentationHandler).build();
	}

	@ClassRule
	public static TestRule springDataflowAndLdapServer = RuleChain
			.outerRule(fakeMetricsCollectorResource)
			.around(localDataflowResource);

	@Before
	public void setupMocks() {
		this.prepareDocumentationTests(localDataflowResource.getWebApplicationContext());
	}

	@Test
	public void getMetricsWithCollectorRunning() throws Exception {
		this.mockMvc.perform(get("/metrics/streams")
				.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(jsonPath("$[0].name", is("foostream")))
				.andExpect(jsonPath("$[0].applications", hasSize(2)))
				.andExpect(status().isOk());
	}
}
