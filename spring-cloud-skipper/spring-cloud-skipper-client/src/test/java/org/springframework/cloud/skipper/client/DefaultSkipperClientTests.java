/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.LogInfo;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.ScaleRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;



// @checkstyle:off
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
// @checkstyle:on

/**
 * Tests for {@link DefaultSkipperClient}.
 *
 * @author Mark Pollack
 * @author Janne Valkealahti
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
public class DefaultSkipperClientTests {

	private final String ERROR1 = "{\"timestamp\":1508161424577," +
			"\"status\":404," +
			"\"error\":\"Not Found\"," +
			"\"exception\":\"org.springframework.cloud.skipper.ReleaseNotFoundException\"," +
			"\"message\":\"Release not found\",\"path\":\"/api/status/mylog\",\"releaseName\":\"mylog\"}";

	private final String ERROR2 = "{\"timestamp\":1508161424577," +
			"\"status\":404," +
			"\"error\":\"Not Found\"," +
			"\"exception\":\"org.springframework.cloud.skipper.SkipperException\"," +
			"\"message\":\"Some skipper message\",\"path\":\"/api/status/mylog\"}";

	private final String ERROR3 = "{\"timestamp\":1508161424577," +
			"\"status\":409," +
			"\"error\":\"Conflict\"," +
			"\"exception\":\"org.springframework.cloud.skipper.PackageDeleteException\"," +
			"\"message\":\"Can't delete package: [package1] because is used by deployed releases: [release2]\"," +
			"\"path\":\"/api/status/mylog\",\"releaseName\":\"mylog\"}";

	@Test
	public void genericTemplateTest() {
		SkipperClient skipperClient = new DefaultSkipperClient("http://localhost:7577");
		assertThat(skipperClient.getSpringCloudDeployerApplicationTemplate()).isNotNull();
		assertThat(skipperClient.getSpringCloudDeployerApplicationTemplate().getData()).isNotEmpty();
	}

	@Test
	public void testStatusReleaseNameFound() {
		RestTemplate restTemplate = new RestTemplate();
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer.expect(requestTo("/release/status/mylog")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		Info status = skipperClient.status("mylog");
		mockServer.verify();

		assertThat(status).isNotNull();
		assertThat(status).isInstanceOf(Info.class);
	}

	@Test
	public void testStatusReleaseNameNotFound() {
		assertThatExceptionOfType(ReleaseNotFoundException.class).isThrownBy(() -> {
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setErrorHandler(new SkipperClientResponseErrorHandler(new ObjectMapper()));
			SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

			MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
			mockServer.expect(requestTo("/release/status/mylog"))
					.andRespond(withStatus(HttpStatus.NOT_FOUND).body(ERROR1).contentType(MediaType.APPLICATION_JSON));

			skipperClient.status("mylog");
		});
	}

	@Test
	public void testSkipperException() {
		assertThatExceptionOfType(SkipperException.class).isThrownBy(() -> {
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setErrorHandler(new SkipperClientResponseErrorHandler(new ObjectMapper()));
			SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

			MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
			mockServer.expect(requestTo("/release/status/mylog"))
					.andRespond(withStatus(HttpStatus.NOT_FOUND).body(ERROR2).contentType(MediaType.APPLICATION_JSON));

			skipperClient.status("mylog");
		});
	}

	@Test
	public void testDeleteReleaseWithoutPackageDeletion() {
		testDeleteRelease(false);
	}

	@Test
	public void testDeleteReleaseWithPackageDeletion() {
		testDeleteRelease(true);
	}

	private void testDeleteRelease(boolean deletePackage) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new SkipperClientResponseErrorHandler(new ObjectMapper()));
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
				MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer.expect(requestTo("/release/release1" + (deletePackage ? "/package" : "")))
				.andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON));

		skipperClient.delete("release1", deletePackage);
	}

	@Test
	public void testDeletePackageHasDeployedRelease() {
		assertThatExceptionOfType(PackageDeleteException.class).isThrownBy(() -> {
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setErrorHandler(new SkipperClientResponseErrorHandler(new ObjectMapper()));
			SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

			MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
			mockServer.expect(requestTo("/release/release1/package"))
					.andRespond(withStatus(HttpStatus.CONFLICT).body(ERROR3).contentType(MediaType.APPLICATION_JSON));
			skipperClient.delete("release1", true);
		});
	}

	@Test
	public void testLogByReleaseName() {
		RestTemplate restTemplate = new RestTemplate();
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer.expect(requestTo("/release/logs/mylog")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		LogInfo logContent = skipperClient.getLog("mylog");
		mockServer.verify();

		assertThat(logContent).isNotNull();
	}

	@Test
	public void testLogByReleaseAndAppNames() {
		RestTemplate restTemplate = new RestTemplate();
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer.expect(requestTo("/release/logs/mylog/app")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		LogInfo logContent = skipperClient.getLog("mylog", "app");
		mockServer.verify();

		assertThat(logContent).isNotNull();
	}

	@Test
	public void testScaleByReleaseAndScaleRequest() {
		RestTemplate restTemplate = new RestTemplate();
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer
			.expect(requestTo("/release/scale/mylog"))
			.andExpect(content().json("{\"scale\":[{\"name\":\"app\",\"count\":2,\"properties\":{}}]}"))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		ScaleRequest scaleRequest = ScaleRequest.of("app", 2);
		Release release = skipperClient.scale("mylog", scaleRequest);
		mockServer.verify();

		assertThat(release).isNotNull();
	}

	@Test
	public void testReleaseList() {
		RestTemplate restTemplate = new RestTemplate();
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer.expect(requestTo("/release/list")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		List<Release> list = skipperClient.list(null);
		assertThat(list).isEmpty();
	}

	@Test
	public void testActuatorGet() {
		RestTemplate restTemplate = new RestTemplate();
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer
				.expect(requestTo("/release/actuator/tiktok/log/tiktok-log-0?endpoint=/foo/bar"))
				.andExpect(queryParam("endpoint","/foo/bar"))
				.andRespond(withSuccess("{\"foo\":\"bar\"}", MediaType.APPLICATION_JSON));


		String response = skipperClient.getFromActuator(
				"tiktok","log", "tiktok-log-0","/foo/bar");
		assertThat(response).isEqualTo("{\"foo\":\"bar\"}");
	}

	@Test
	public void testActuatorPost() {
		RestTemplate restTemplate = new RestTemplate();
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);
		ActuatorPostRequest actuatorPostRequest = ActuatorPostRequest.of("/bindings/input",
				Collections.singletonMap("state","STOPPED"));
		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer
				.expect(requestTo("/release/actuator/tiktok/log/tiktok-log-0"))
				.andExpect(content().json(
						"{\"endpoint\":\"/bindings/input\",\"body\":{\"state\":\"STOPPED\"}}"))
				.andRespond(withSuccess());

		//Don't care about the response here.
		skipperClient.postToActuator(
				"tiktok","log", "tiktok-log-0", actuatorPostRequest);
	}
}
