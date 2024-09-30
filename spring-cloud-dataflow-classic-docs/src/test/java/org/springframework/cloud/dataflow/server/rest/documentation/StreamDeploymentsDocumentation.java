/*
 * Copyright 2017-2023 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodName.class)
@DirtiesContext
public class StreamDeploymentsDocumentation extends BaseDocumentation {

	@BeforeEach
	void setup() throws Exception {
		this.mockMvc.perform(
				post("/apps/{type}/time", "source")
						.param("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE")
						.param("force", "true"))
				.andExpect(status().isCreated());
		this.mockMvc.perform(
				post("/apps/{type}/log", "sink")
						.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE")
						.param("force", "true"))
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
	}

	@Test
	void scale() throws Exception {
		String json = "{\"app.time.timestamp.format\":\"YYYY\"}";
		this.mockMvc.perform(
				post("/streams/deployments/scale/{streamName}/{appName}/instances/{count}", "timelog", "log", 1)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(pathParameters(
						parameterWithName("streamName")
								.description("the name of an existing stream definition (required)"),
						parameterWithName("appName")
								.description("in stream application name to scale"),
						parameterWithName("count")
								.description("number of instances for the selected stream application (required)"))
				));
	}

	@Test
	void unDeploy() throws Exception {
		this.mockMvc.perform(
				delete("/streams/deployments/{timelog}", "timelog"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog")
								.description("The name of an existing stream definition (required)"))
				));
	}

	@Test
	void unDeployAll() throws Exception {
		this.mockMvc.perform(
				delete("/streams/deployments"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document());
	}


	@Test
	void info() throws Exception {
		String json = "{\"app.time.timestamp.format\":\"YYYY\"}";
		this.mockMvc.perform(
				get("/streams/deployments/{timelog}?reuse-deployment-properties=true", "timelog")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog")
								.description("The name of an existing stream definition (required)")),
						queryParameters(parameterWithName("reuse-deployment-properties")
								.description(parameterWithName("The name of the flag to reuse the deployment properties")))
				));
	}

	@Test
	void deploy() throws Exception {
		String json = "{\"app.time.timestamp.format\":\"YYYY\"}";
		this.mockMvc.perform(
				post("/streams/deployments/{timelog}", "timelog")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog")
								.description("The name of an existing stream definition (required)"))
				));
	}

	@Test
	void streamUpdate() throws Exception {
		String json = "{\"app.time.timestamp.format\":\"YYYY\"}";
		this.mockMvc.perform(
				post("/streams/deployments/{timelog1}", "timelog1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog1")
								.description("The name of an existing stream definition (required)"))
				));
		UpdateStreamRequest updateStreamRequest = new UpdateStreamRequest();
		updateStreamRequest.setReleaseName("timelog1");
		Map<String, String> updateProperties = new HashMap<>();
		updateProperties.put("app.time.timestamp.format", "YYYYMMDD");
		updateStreamRequest.setUpdateProperties(updateProperties);

		final PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("timelog1");
		packageIdentifier.setPackageVersion("1.0.0");
		packageIdentifier.setRepositoryName("test");
		updateStreamRequest.setPackageIdentifier(packageIdentifier);

		this.mockMvc.perform(
				post("/streams/deployments/update/{timelog1}", "timelog1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(convertObjectToJson(updateStreamRequest)))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("timelog1")
								.description("The name of an existing stream definition (required)"))
				));
	}

	@Test
	void rollback() throws Exception {
		final RollbackRequest rollbackRequest = new RollbackRequest();
		rollbackRequest.setReleaseName("timelog1");
		this.mockMvc.perform(
				post("/streams/deployments/rollback/{name}/{version}", "timelog1", 1)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("name")
										.description("The name of an existing stream definition (required)"),
								parameterWithName("version").description("The version to rollback to"))));
	}

	@Test
	void history() throws Exception {
		when(springDataflowServer.getSkipperClient().history(anyString()))
				.thenReturn(Collections.singletonList(new Release()));

		this.mockMvc.perform(
				get("/streams/deployments/history/{name}", "timelog1")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("name")
								.description("The name of an existing stream definition (required)"))));
	}

	@Test
	void manifest() throws Exception {
		this.mockMvc.perform(
				get("/streams/deployments/manifest/{name}/{version}", "timelog1", 1)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
						pathParameters(parameterWithName("name")
										.description("The name of an existing stream definition (required)"),
								parameterWithName("version").description("The version of the stream"))));
	}

	@Test
	void platformList() throws Exception {
		this.mockMvc.perform(
				get("/streams/deployments/platform/list")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	public static String convertObjectToJson(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.writeValueAsString(object);
	}

}
