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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the {@code /audit-records} endpoint.
 *
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodOrderer.MethodName.class)
class AuditRecordsDocumentation extends BaseDocumentation {

	@BeforeEach
	void setup() throws Exception {
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
	}

	@Test
	void listAllAuditRecords() throws Exception {
		this.mockMvc.perform(
			get("/audit-records")
				.param("page", "0")
				.param("size", "10")
				.param("operations", "STREAM")
				.param("actions", "CREATE")
				.param("fromDate", "2000-01-01T00:00:00")
				.param("toDate", "2099-01-01T00:00:00")
			)
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				queryParameters(
					parameterWithName("page").optional().description("The zero-based page number"),
					parameterWithName("size").optional().description("The requested page size"),
					parameterWithName("operations").optional().description("Comma-separated list of Audit Operations"),
					parameterWithName("actions").optional().description("Comma-separated list of Audit Actions"),
					parameterWithName("fromDate").optional()
							.description("From date filter (ex.: 2019-02-03T00:00:30)"),
					parameterWithName("toDate").optional()
							.description("To date filter (ex.: 2019-02-03T00:00:30)")
				),
				responseFields(
					subsectionWithPath("_embedded.auditRecordResourceList")
						.description("Contains a collection of Audit Records"),
					subsectionWithPath("_links.self").description("Link to the audit record resource"),
					subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	void getAuditRecord() throws Exception {
		this.mockMvc.perform(
			get("/audit-records/{id}", "5"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				pathParameters(
					parameterWithName("id").description("The id of the audit record to query")
				),
				responseFields(
					fieldWithPath("auditRecordId").description("The id of the audit record"),
					fieldWithPath("createdBy").optional().description("The author of the audit record"),
					fieldWithPath("correlationId").description("The correlation ID of the audit record"),
					fieldWithPath("auditData").description("The data of the audit record"),
					fieldWithPath("createdOn").description("The creation date of the audit record"),
					fieldWithPath("auditAction").description("The action of the audit record"),
					fieldWithPath("auditOperation").description("The operation of the audit record"),
					fieldWithPath("platformName").description("The platform name of the audit record"),
					subsectionWithPath("_links.self").description("Link to the audit record resource")
				)
			));
	}

	@Test
	void getAuditActionTypes() throws Exception {
		this.mockMvc.perform(
			get("/audit-records/audit-action-types"))
			.andExpect(status().isOk()
		);
	}

	@Test
	void getAuditOperationTypes() throws Exception {
		this.mockMvc.perform(
			get("/audit-records/audit-operation-types"))
			.andExpect(status().isOk()
		);
	}

}
