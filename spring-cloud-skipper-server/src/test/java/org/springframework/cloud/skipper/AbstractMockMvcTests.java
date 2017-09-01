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
package org.springframework.cloud.skipper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * @author Mark Pollack
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractMockMvcTests {

	private final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	protected MockMvc mockMvc;

	@Autowired
	protected WebApplicationContext wac;

	public static String convertObjectToJson(Object object) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		String json = mapper.writeValueAsString(object);
		return json;
	}

	@Before
	public void setupMockMvc() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON).contentType(contentType))
				.build();
	}

	protected void assertReleaseIsDeployedSuccessfully(String releaseName, String releaseVersion)
			throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		long startTime = System.currentTimeMillis();
		while (!isDeployed(releaseName, releaseVersion)
				|| (System.currentTimeMillis() - startTime) < 12000) {
			Thread.sleep(10000);
		}
		if (isDeployed(releaseName, releaseVersion)) {
			latch.countDown();
		}
		assertThat(latch.await(1, TimeUnit.SECONDS)).describedAs("Status check timed out").isTrue();
	}

	private boolean isDeployed(String releaseName, String releaseVersion) {
		try {
			MvcResult result = mockMvc.perform(get(String.format("/release/status/%s/%s", releaseName, releaseVersion)))
					.andDo(print()).andReturn();
			String content = result.getResponse().getContentAsString();
			return content.startsWith(getSuccessStatus(releaseName, releaseVersion));
		}
		catch (Exception e) {
			return false;
		}
	}

	private String getSuccessStatus(String release, String version) {
		return "{\"name\":\"" + release + "\",\"version\":" + version + ","
				+ "\"info\":{\"status\":{\"statusCode\":\"DEPLOYED\","
				+ "\"platformStatus\":\"All the applications are deployed successfully.";
	}

}
