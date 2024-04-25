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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creates asciidoc snippets for endpoints exposed by {@literal RuntimeAppsController}.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@SuppressWarnings("NewClassNamingConvention")
@DirtiesContext
public class RuntimeAppsDocumentation extends BaseDocumentation {

	@BeforeEach
	public void setup() throws Exception {
		registerApp(ApplicationType.source, "http", "1.2.0.RELEASE");
		registerApp(ApplicationType.sink, "log", "1.2.0.RELEASE");
		createStream("mystream", "http | log", true);
	}

	@AfterEach
	public void cleanup() throws Exception {
		destroyStream("mystream");
		unregisterApp(ApplicationType.source, "http");
		unregisterApp(ApplicationType.sink, "log");
	}

	@Test
	public void listRuntimeStreamStatus() throws Exception {
		this.mockMvc.perform(
				get("/runtime/streams")
						.accept(MediaType.APPLICATION_JSON)
						.param("names", "mystream"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document());
	}

	@Test
	public void listRuntimeStreamStatusV2() throws Exception {
		this.mockMvc.perform(
				get("/runtime/streams/status")
						.accept(MediaType.APPLICATION_JSON)
						.param("names", "mystream"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document());
	}

	@Test
	public void listAllApps() throws Exception {
		this.mockMvc.perform(
				get("/runtime/apps")
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document());
	}

	@Test
	public void listSingleAppAllInstances() throws Exception {

		Info info = new Info();
		info.setStatus(new Status());
		info.getStatus().setStatusCode(StatusCode.DEPLOYED);
		info.getStatus().setPlatformStatus("[{\"deploymentId\":\"mystream.log\","
				+ "\"instances\":{\"mystream.log-0\":{\"instanceNumber\":0,\"id\":\"mystream.log-0\","
				+ "\"state\":\"deployed\"}},\"state\":\"deployed\"},"
				+ "{\"deploymentId\":\"mystream.http\",\"instances\":{\"mystream.http-0\":{\"instanceNumber\":0,"
				+ "\"baseUrl\":\"https://192.168.1.100:32451\","
				+ "\"process\":{\"alive\":true,\"inputStream\":{},\"outputStream\":{},\"errorStream\":{}},"
				+ "\"attributes\":{\"guid\":\"32451\",\"pid\":\"53492\",\"port\":\"32451\"},"
				+ "\"id\":\"mystream.http-0\",\"state\":\"deployed\"}},\"state\":\"deployed\"}]");
		List<Release> releases = new ArrayList<>();
		Release release = new Release();
		release.setInfo(info);
		releases.add(release);
		when(springDataflowServer.getSkipperClient().list(any())).thenReturn(releases);

		this.mockMvc.perform(
				get("/runtime/apps/mystream.http/instances")
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document());
	}

	@Test
	public void getSingleAppSingleInstance() throws Exception {

		Info info = new Info();
		info.setStatus(new Status());
		info.getStatus().setStatusCode(StatusCode.DEPLOYED);
		info.getStatus().setPlatformStatus("[{\"deploymentId\":\"mystream.log\","
				+ "\"instances\":{\"mystream.log-0\":{\"instanceNumber\":0,\"id\":\"mystream.log-0\","
				+ "\"state\":\"deployed\"}},\"state\":\"deployed\"},"
				+ "{\"deploymentId\":\"mystream.http\",\"instances\":{\"mystream.http-0\":{\"instanceNumber\":0,"
				+ "\"baseUrl\":\"https://192.168.1.100:32451\","
				+ "\"process\":{\"alive\":true,\"inputStream\":{},\"outputStream\":{},\"errorStream\":{}},"
				+ "\"attributes\":{\"guid\":\"32451\",\"pid\":\"53492\",\"port\":\"32451\"},"
				+ "\"id\":\"mystream.http-0\",\"state\":\"deployed\"}},\"state\":\"deployed\"}]");
		List<Release> releases = new ArrayList<>();
		Release release = new Release();
		release.setInfo(info);
		releases.add(release);
		when(springDataflowServer.getSkipperClient().list(any())).thenReturn(releases);

		this.mockMvc.perform(
				get("/runtime/apps/mystream.http/instances/mystream.http-0")
						.accept(MediaType.APPLICATION_JSON)
		)
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document());
	}
}
