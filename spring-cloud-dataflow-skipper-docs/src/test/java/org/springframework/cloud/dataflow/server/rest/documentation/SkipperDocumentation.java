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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilayaperumal Gopinathan
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SkipperDocumentation extends BaseSkipperDocumentation {

	private static boolean setUpIsDone = false;

	@Before
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
		this.mockMvc.perform(
				post("/streams/definitions")
						.param("name", "timelog1")
						.param("definition", "time --format='YYYY MM DD' | log")
						.param("deploy", "false"))
				.andExpect(status().isCreated());
		setUpIsDone = true;
	}

	@Test
	public void appDefault() throws Exception {
		registerApp(ApplicationType.source, "http", "1.2.0.RELEASE");
		registerApp(ApplicationType.source, "http", "1.3.0.RELEASE");

		this.mockMvc.perform(RestDocumentationRequestBuilders
				.put("/apps/{type}/{name}/{version:.+}", ApplicationType.source, "http", "1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isAccepted())
				.andDo(
				this.documentationHandler.document(
						pathParameters(
								parameterWithName("type").description("The type of application. One of " + Arrays.asList(ApplicationType.values())),
								parameterWithName("name").description("The name of the application"),
								parameterWithName("version").description("The version of the application")
						)
				)
		);
	}

	@Test
	public void registeringAnApplicationVersion() throws Exception {
		this.mockMvc.perform(
				post("/apps/{type}/{name}/{version:.+}", ApplicationType.source, "http", "1.1.0.RELEASE")
						.param("uri", "maven://org.springframework.cloud.stream.app:http-source-rabbit:1.1.0.RELEASE"))
				.andExpect(status().isCreated())
				.andDo(
						this.documentationHandler.document(
								pathParameters(
										parameterWithName("type").description("The type of application to register. One of " + Arrays.asList(ApplicationType.values())),
										parameterWithName("name").description("The name of the application to register"),
										parameterWithName("version").description("The version of the application to register")
								),
								requestParameters(
										parameterWithName("uri").description("URI where the application bits reside"),
										parameterWithName("metadata-uri").optional().description("URI where the application metadata jar can be found"),
										parameterWithName("force").optional().description("Must be true if a registration with the same name and type already exists, otherwise an error will occur")
								)
						)
				);

		unregisterApp(ApplicationType.source, "http");
	}

	@Test
	public void a_streamUpdate() throws Exception {
		String json = "{\"app.time.timestamp.format\":\"YYYY\"}";
		this.mockMvc.perform(
				post("/streams/deployments/{timelog1}", "timelog1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andDo(print())
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog1")
								.description("The name of an existing stream definition (required)"))
				));
		Thread.sleep(30000);
		UpdateStreamRequest updateStreamRequest = new UpdateStreamRequest();
		updateStreamRequest.setReleaseName("timelog1");
		Map<String, String> updateProperties = new HashMap<>();
		updateProperties.put("app.time.timestamp.format", "YYYYMMDD");
		updateStreamRequest.setUpdateProperties(updateProperties);
		final String releaseName = "myLogRelease";
		final PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("timelog1");
		packageIdentifier.setPackageVersion("1.0.0");
		packageIdentifier.setRepositoryName("test");
		updateStreamRequest.setPackageIdentifier(packageIdentifier);

		this.mockMvc.perform(
				post("/streams/deployments/update/{timelog1}", "timelog1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(convertObjectToJson(updateStreamRequest)))
				.andDo(print())
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog1")
								.description("The name of an existing stream definition (required)"))
				));
		Thread.sleep(30000);
	}

	@Test
	public void b_rollback() throws Exception {
		RollbackRequest rollbackRequest = new RollbackRequest();
		rollbackRequest.setReleaseName("timelog1");
		this.mockMvc.perform(
				post("/streams/deployments//rollback/{name}/{version}", "timelog1", 1)
						.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("name")
										.description("The name of an existing stream definition (required)"),
								parameterWithName("version").description("The version to rollback to"))));
		Thread.sleep(30000);
	}

	@Test
	public void history() throws Exception {
		this.mockMvc.perform(
				get("/streams/deployments/history/{name}", "timelog1")
						.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("name")
								.description("The name of an existing stream definition (required)"))));
	}

	@Test
	public void manifest() throws Exception {
		this.mockMvc.perform(
				get("/streams/deployments/manifest/{name}/{version}", "timelog1", 1)
						.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("name")
										.description("The name of an existing stream definition (required)"),
								parameterWithName("version").description("The version of the stream"))));
	}

	@Test
	public void platformList() throws Exception {
		this.mockMvc.perform(
				get("/streams/deployments/platform/list")
						.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk());
	}

	public static String convertObjectToJson(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		String json = mapper.writeValueAsString(object);
		return json;
	}
}
