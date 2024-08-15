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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;

/**
 * Documentation for the {@code /audit-records} endpoint.
 *
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AuditRecordsDocumentation extends BaseDocumentation {

	private static boolean setUpIsDone = false;

	@BeforeEach
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
	public void listAllAuditRecords() throws Exception {
		this.mockMvc.perform(
			get("/audit-records")
				.param("page", "0")
				.param("size", "10")
				.param("operations", "STREAM")
				.param("actions", "CREATE")
				.param("fromDate", "2000-01-01T00:00:00")
				.param("toDate", "2099-01-01T00:00:00")
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				queryParameters(
					parameterWithName("page").description("The zero-based page number (optional)"),
					parameterWithName("size").description("The requested page size (optional)"),
					parameterWithName("operations").description("Comma-separated list of Audit Operations (optional)"),
					parameterWithName("actions").description("Comma-separated list of Audit Actions (optional)"),
					parameterWithName("fromDate")
							.description("From date filter (ex.: 2019-02-03T00:00:30) (optional)"),
					parameterWithName("toDate")
							.description("To date filter (ex.: 2019-02-03T00:00:30) (optional)")
				),
				responseFields(
					subsectionWithPath("_embedded.auditRecordResourceList")
						.description("Contains a collection of Audit Records"),
					subsectionWithPath("_links.self").description("Link to the audit record resource"),
					subsectionWithPath("page").description("Pagination properties"))));
	}

	@Test
	@Disabled("find 404")
	public void getAuditRecord() throws Exception {
		this.mockMvc.perform(
			get("/audit-records/{id}", "5"))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				pathParameters(
					parameterWithName("id").description("The id of the audit record to query (required)")
				),
				responseFields(
					fieldWithPath("auditRecordId").description("The id of the audit record"),
					fieldWithPath("createdBy").description("The author of the audit record (optional)"),
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
	public void getAuditActionTypes() throws Exception {
		this.mockMvc.perform(
			get("/audit-records/audit-action-types"))
			.andDo(print())
			.andExpect(status().isOk()
		);
	}

	@Test
	public void getAuditOperationTypes() throws Exception {
		this.mockMvc.perform(
			get("/audit-records/audit-operation-types"))
			.andDo(print())
			.andExpect(status().isOk()
		);
	}

}
