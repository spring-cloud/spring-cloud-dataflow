/*
 * Copyright 2016 the original author or authors.
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

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.cloud.dataflow.server.local.LocalDataflowResource;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Gunnar Hillert
 */
public abstract class BaseDocumentation {

	protected String TARGET_DIRECTORY = "target/generated-snippets";

	@Rule
	public JUnitRestDocumentation restDocumentation =
			new JUnitRestDocumentation(TARGET_DIRECTORY);

	@ClassRule
	public final static LocalDataflowResource springDataflowServer =
			new LocalDataflowResource("classpath:rest-docs-config.yml");

	protected MockMvc mockMvc;
	protected RestDocumentationResultHandler documentationHandler;

	@Before
	public void setupMocks() {
		prepareDocumentationTests(restDocumentation);
	}

	protected void prepareDocumentationTests(JUnitRestDocumentation restDocumentation) {
		this.documentationHandler = document("{class-name}/{method-name}",
				preprocessResponse(prettyPrint()));
		this.mockMvc = MockMvcBuilders.webAppContextSetup(springDataflowServer.getWebApplicationContext())
				.apply(documentationConfiguration(restDocumentation))
				.alwaysDo(this.documentationHandler)
				.build();
	}

}
