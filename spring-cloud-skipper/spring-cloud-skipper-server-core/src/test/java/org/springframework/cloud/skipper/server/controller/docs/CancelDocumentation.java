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

package org.springframework.cloud.skipper.server.controller.docs;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.domain.CancelRequest;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Janne Valkealahti
 * @author Corneil du Plessis
 */
class CancelDocumentation extends BaseDocumentation {

	@Test
	void cancelRelease() throws Exception {
		final String releaseName = "myLogRelease";

		when(this.skipperStateMachineService.cancelRelease(releaseName)).thenReturn(Boolean.TRUE);

		final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
				MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

		this.mockMvc.perform(
				post("/api/release/cancel").accept(MediaType.APPLICATION_JSON).contentType(contentType)
						.content(convertObjectToJson(new CancelRequest(releaseName))))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						responseFields(fieldWithPath("accepted").description("If cancel request was accepted"))
				))
				.andReturn();
	}
}
