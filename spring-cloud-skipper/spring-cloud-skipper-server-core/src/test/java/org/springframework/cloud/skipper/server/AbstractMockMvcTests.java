/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.skipper.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.autoconfigure.ResourceLoadingAutoConfiguration;
import org.springframework.cloud.skipper.domain.CancelResponse;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.cloud.skipper.server.config.SkipperServerPlatformConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.config.HypermediaMappingInformation;
import org.springframework.http.MediaType;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = { AbstractMockMvcTests.TestConfig.class,
		AbstractMockMvcTests.HypermediaBareJsonConfiguration.class }, properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
public abstract class AbstractMockMvcTests extends AbstractAssertReleaseDeployedTest {

	private final Logger logger = LoggerFactory.getLogger(getClass());

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

	@BeforeEach
	public void setupMockMvc() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON).contentType(contentType)).build();
	}

	@Override
	protected boolean isDeployed(String releaseName, int releaseVersion) {
		try {
			logger.info("Checking status of release={} version={}", releaseName, releaseVersion);
			MvcResult result = mockMvc.perform(get(String.format("/api/release/status/%s/%s", releaseName, releaseVersion)))
					.andReturn();
			Info info = convertContentToInfo(result.getResponse().getContentAsString());

			logger.info("Status = " + info.getStatus());
			return info.getStatus().getStatusCode().equals(StatusCode.DEPLOYED) &&
					allAppsDeployed(info.getStatus().getAppStatusList());
		}
		catch (Exception e) {
			logger.error("Exception getting status", e);
			return false;
		}
	}

	private Info convertContentToInfo(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return objectMapper.readValue(json, new TypeReference<Info>() {
			});
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't parse JSON for Info", e);
		}
	}

	protected Release convertContentToRelease(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return objectMapper.readValue(json, new TypeReference<Release>() {
			});
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't parse JSON for Release", e);
		}
	}

	protected CancelResponse convertContentToCancelResponse(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return objectMapper.readValue(json, new TypeReference<CancelResponse>() {
			});
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't parse JSON for Release", e);
		}
	}

	protected UpgradeProperties createUpdateProperties(String releaseName) {
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		return upgradeProperties;
	}

	@Configuration
	@ImportAutoConfiguration(classes = { SecurityAutoConfiguration.class, JacksonAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
			HibernateJpaAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, StateMachineJpaRepositoriesAutoConfiguration.class,
			SkipperServerPlatformConfiguration.class, ResourceLoadingAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	@EnableWebMvc
	static class TestConfig {
	}

	@Configuration
	static class HypermediaBareJsonConfiguration implements HypermediaMappingInformation {

		@Override
		public List<MediaType> getMediaTypes() {
			return Collections.singletonList(MediaType.APPLICATION_JSON);
		}
	}
}
