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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.springframework.cloud.dataflow.core.ApplicationType.sink;
import static org.springframework.cloud.dataflow.core.ApplicationType.source;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creates asciidoc snippets for endpoints exposed by {@literal RuntimeAppsController}.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class RuntimeAppsDocumentation extends BaseDocumentation {

	@Before
	public void setup() throws Exception {
		registerApp(source, "http");
		registerApp(sink, "log");
		createStream("mystream", "http | log", true);
	}

	@After
	public void cleanup() throws Exception {
		destroyStream("mystream");
		unregisterApp(source, "http");
		unregisterApp(sink, "log");
	}

	@Test
	public void listAllApps() throws Exception {
		this.mockMvc.perform(
			get("/runtime/apps")
				.accept(APPLICATION_JSON)
		)
		.andExpect(status().isOk())
		.andDo(this.documentationHandler.document());
	}

	@Test
	public void listSingleAppAllInstances() throws Exception {
		this.mockMvc.perform(
			get("/runtime/apps/mystream.http/instances")
				.accept(APPLICATION_JSON)
		)
		.andExpect(status().isOk())
			.andDo(this.documentationHandler.document());
	}

	@Test
	public void getSingleAppSingleInstance() throws Exception {
		this.mockMvc.perform(
			get("/runtime/apps/mystream.http/instances/mystream.http-0")
				.accept(APPLICATION_JSON)
		)
		.andExpect(status().isOk())
		.andDo(this.documentationHandler.document());
	}
}
