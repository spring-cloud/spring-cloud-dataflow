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

package org.springframework.cloud.skipper.controller.docs;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.controller.AbstractControllerTests;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.hypermedia.HypermediaDocumentation;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.RequestParametersSnippet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;

/**
 * Sets up Spring Rest Docs via {@link #setupMocks()} and also provides common snippets to be used by
 * the various documentation tests.
 *
 * @author Gunnar Hillert
 */
public abstract class BaseDocumentation extends AbstractControllerTests {

	@Autowired
	WebApplicationContext context;

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

	protected RestDocumentationResultHandler documentationHandler;

	protected MockMvc mockMvc;

	@Before
	public void setupMocks() {
		this.prepareDocumentationTests(this.context);
	}

	private void prepareDocumentationTests(WebApplicationContext context) {
		this.documentationHandler = document("{class-name}/{method-name}",
				preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()));

		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation).uris()
				.withScheme("http")
				.withHost("localhost")
				.withPort(7577))
				.alwaysDo(this.documentationHandler)
		.build();
	}

	/**
	 * Snippet, documenting common pagination properties.
	 */
	protected final ResponseFieldsSnippet paginationProperties = responseFields(
		fieldWithPath("page").description("Pagination properties"),
		fieldWithPath("page.size").description("The size of the page being returned"),
		fieldWithPath("page.totalElements").description("Total elements available for pagination"),
		fieldWithPath("page.totalPages").description("Total amount of pages"),
		fieldWithPath("page.number").description("Page number of the page returned (zero-based)")
	);

	/**
	 * Snippet for link properties. Set to ignore common links.
	 */
	protected final List<FieldDescriptor> defaultLinkProperties = Arrays.asList(
		fieldWithPath("_links.self.href").ignored().optional(),
		fieldWithPath("_links.self.templated").ignored().optional(),
		fieldWithPath("_links.profile.href").ignored().optional(),
		fieldWithPath("_links.repository.href").ignored().optional(),
		fieldWithPath("_links.search.href").ignored().optional()
	);

	/**
	 * Snippet for common pagination-related request parameters.
	 */
	protected final RequestParametersSnippet paginationRequestParameterProperties = requestParameters(
		parameterWithName("page").description("The zero-based page number (optional)"),
		parameterWithName("size").description("The requested page size (optional)")
	);

	/**
	 * {@link LinksSnippet} for common links. Common links are set to be ignored.
	 *
	 * @param descriptors Provide addition link descriptors
	 * @return the link snipped
	 */
	public static LinksSnippet linksForSkipper(LinkDescriptor... descriptors) {
		return HypermediaDocumentation.links(
			linkWithRel("self").ignored(),
			linkWithRel("profile").ignored(),
			linkWithRel("search").ignored(),
			linkWithRel("deployer").ignored().optional(),
			linkWithRel("curies").ignored().optional()
		).and(descriptors);
	}

}
