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

package org.springframework.cloud.dataflow.server.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.registry.DataFlowAppRegistryPopulator;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultStreamServiceIntegrationTests;
import org.springframework.cloud.dataflow.server.support.MockUtils;
import org.springframework.cloud.dataflow.server.support.TestResourceUtils;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.WebApplicationContext;


/**
 * Tests for {@link AppRegistryController}
 *
 * @author Ilayaperumal Gopinathan
 * @author Chris Schaefer
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class AppRegistryControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppRegistryService appRegistryService;

	@Autowired
	private StreamService streamService;

	@MockBean
	private SkipperClient skipperClient;

	@Autowired
	private DataFlowAppRegistryPopulator uriRegistryPopulator;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@BeforeEach
	void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration : this.appRegistryService.findAll()) {
			this.appRegistryService.delete(appRegistration.getName(), appRegistration.getType(), appRegistration.getVersion());
		}
		this.uriRegistryPopulator.afterPropertiesSet();
		this.skipperClient = MockUtils.configureMock(this.skipperClient);
	}

	@Test
	void registerVersionedApp() throws Exception {
		mockMvc.perform(post("/apps/sink/log1/1.2.0.RELEASE").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		assertThat(this.appRegistryService.find("log1", ApplicationType.sink).getUri()).hasToString("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE");
	}

	@Test
	void findRegisteredApp() throws Exception {
		// given
		mockMvc.perform(
				post("/apps/sink/log1/3.0.0")
						.queryParam("bootVersion", "3")
						.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:3.0.0").accept(MediaType.APPLICATION_JSON)
				).andExpect(status().isCreated());
		// when
		AppRegistration registration = this.appRegistryService.find("log1", ApplicationType.sink);
		// then
		assertThat(registration.getUri()).hasToString("maven://org.springframework.cloud.stream.app:log-sink-rabbit:3.0.0");
	}

	@Test
	void registerAppAndUpdate() throws Exception {
		testAndValidateUpdate();
	}

	private void testAndValidateUpdate() throws Exception{
		mockMvc.perform(post("/apps/sink/log1/1.2.0.RELEASE").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
		assertThat(this.appRegistryService.find("log1", ApplicationType.sink).getUri()).hasToString("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE");
		// given
		mockMvc.perform(post("/apps/sink/log1/3.0.0")
						.queryParam("force", "true")
						.queryParam("bootVersion", "3")
						.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:3.0.0").accept(MediaType.APPLICATION_JSON)
				)
				.andExpect(status().isCreated());
		// updating default version to 3.0.0
		mockMvc.perform(put("/apps/sink/log1/3.0.0")).andDo(print()).andExpect(status().isAccepted());
		// when
		AppRegistration registration = this.appRegistryService.find("log1", ApplicationType.sink);
		// then
		assertThat(registration.getUri()).hasToString("maven://org.springframework.cloud.stream.app:log-sink-rabbit:3.0.0");

	}

	@Test
	void registerAppAndUpdateToAndRollback() throws Exception {
		testAndValidateUpdate();

		// updating Rollback version to 1.2.0
		mockMvc.perform(put("/apps/sink/log1/1.2.0.RELEASE")).andExpect(status().isAccepted());
		// when
		AppRegistration registration = this.appRegistryService.find("log1", ApplicationType.sink);
		// then
		assertThat(registration.getUri()).hasToString("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE");

	}

	@Test
	void registerInvalidAppUri() throws Exception {
		mockMvc.perform(post("/apps/sink/log1/1.2.0.RELEASE").param("uri", "\\boza").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
	}

	@Test
	void registerAppWithInvalidName() throws Exception {
		mockMvc.perform(post("/apps/sink/log:1")
				.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE")
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().is4xxClientError());
	}

	@Test
	void registerAppWithNameLongerThan255Characters() throws Exception {
		mockMvc.perform(post(
				"/apps/sink/sinkAppToTestIfLengthIsGreaterThanTwoHundredAndFiftyFiveCharacterssinkAppToTestIfLengthIsGreaterThanTwoHundredAndFiftyFiveCharacterssinkAppToTestIfLengthIsGreaterThanTwoHundredAndFiftyFiveCharacterssinkAppToTestIfLengthIsGreaterThanTwoHundredAndFiftyFiveCharacters")
						.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE")
						.accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is4xxClientError());
	}

	@Test
	void registerApp() throws Exception {
		mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		assertThat(this.appRegistryService.find("log1", ApplicationType.sink).getUri()).hasToString("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE");
	}

	@Test
	void appInfoNonExistingApp() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/apps/sink/log1")).andDo(print()).andExpect(status().is4xxClientError()).andReturn();
		Assert.isInstanceOf(NoSuchAppRegistrationException.class, mvcResult.getResolvedException());
	}

	@Test
	void appInfoNonExistingVersionedApp() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/apps/sink/log1/1.0.0")).andDo(print()).andExpect(status().is4xxClientError()).andReturn();
		Assert.isInstanceOf(NoSuchAppRegistrationException.class, mvcResult.getResolvedException());
	}

	@Test
	void defaultVersion() throws Exception {
		this.mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
                .andExpect(status().isCreated());

        this.mockMvc.perform(get("/apps/sink/log1"))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.name", is("log1")))
                .andExpect(jsonPath("$.type", is("sink")))
                .andExpect(jsonPath("$.uri", is("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE")))
                .andExpect(jsonPath("$.version", is("1.2.0.RELEASE")))
                .andExpect(jsonPath("$.defaultVersion", is(true)));
    }

	@Test
	void versionOverride() throws Exception {
		this.mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		this.mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.3.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		this.mockMvc.perform(put("/apps/sink/log1/1.3.0.RELEASE")).andDo(print()).andExpect(status().isAccepted());

		this.mockMvc.perform(get("/apps/sink/log1"))
                .andDo(print())
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.name", is("log1")))
                .andExpect(jsonPath("$.type", is("sink")))
                .andExpect(jsonPath("$.uri", is("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.3.0.RELEASE")))
                .andExpect(jsonPath("$.version", is("1.3.0.RELEASE")))
                .andExpect(jsonPath("$.defaultVersion", is(true)));
	}

	@Test
	void versionOverrideNonExistentApp() throws Exception {
		this.mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		MvcResult mvcResult = this.mockMvc.perform(put("/apps/sink/log1/1.3.0.RELEASE")).andDo(print()).andExpect(status().is4xxClientError()).andReturn();
		Assert.isInstanceOf(NoSuchAppRegistrationException.class, mvcResult.getResolvedException());
	}

	@Test
	void registerApplicationTwice() throws Exception {
		mockMvc.perform(post("/apps/processor/blubba").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/apps/processor/blubba").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isConflict());
	}

	@Test
	void versionWithMismatchBaseUri() throws Exception {
		mockMvc.perform(post("/apps/processor/maven1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/apps/processor/maven1").param("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.1.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
		mockMvc.perform(post("/apps/processor/docker1").param("uri", "docker:prefix1/my-source:0.1.0").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/apps/processor/docker1").param("uri", "docker:prefix2/my-source:0.2.0").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
		mockMvc.perform(post("/apps/processor/http1").param("uri", "https://example.com/my-app1-1.1.1.RELEASE.jar").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/apps/processor/http1").param("uri", "https://example.com/my-app2-1.1.2.RELEASE.jar").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());

		// in case you actually have version in part of an uri
		mockMvc.perform(post("/apps/processor/maven2").param("uri", "maven://org.springframework.cloud.stream.app.1.2.0.RELEASE:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/apps/processor/maven2").param("uri", "maven://org.springframework.cloud.stream.app.1.2.0.RELEASE:time-source-rabbit:1.2.1.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
		mockMvc.perform(post("/apps/processor/docker2").param("uri", "docker:prefix1.0.1.0/my-source:0.1.0").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/apps/processor/docker2").param("uri", "docker:prefix2.0.1.0/my-source:0.2.0").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
		mockMvc.perform(post("/apps/processor/http2").param("uri", "https://1.1.1.example.com/my-app1-1.1.1.RELEASE.jar").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/apps/processor/http2").param("uri", "https://1.1.1.example.com/my-app2-1.1.2.RELEASE.jar").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
	}

	@Test
	void registerAll() throws Exception {
		mockMvc.perform(post("/apps").param("apps", "sink.foo=maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		assertThat(this.appRegistryService.find("foo", ApplicationType.sink).getUri()).hasToString("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE");
	}

	@Test
	void registerAllFromFile() throws Exception {
		mockMvc.perform(post("/apps").param("uri", "classpath:/register-all.txt").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		assertThat(this.appRegistryService.find("foo", ApplicationType.sink).getUri()).hasToString("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE");
	}

	@Test
	void registerAllWithoutForce() throws Exception {
		this.appRegistryService.importAll(false, new ClassPathResource("META-INF/test-apps-overwrite.properties"));
		assertThat(this.appRegistryService.find("time", ApplicationType.source).getUri())
				.hasToString("maven://org" + ".springframework.cloud.stream.app:time-source-rabbit:5.0.0");
		assertThat(this.appRegistryService.find("filter", ApplicationType.processor).getUri())
				.hasToString("maven://org" + ".springframework.cloud.stream.app:filter-processor-rabbit:5.0.0");
		assertThat(this.appRegistryService.find("log", ApplicationType.sink).getUri())
				.hasToString("maven://org.springframework" + ".cloud.stream.app:log-sink-rabbit:5.0.0");
		assertThat(this.appRegistryService.find("timestamp", ApplicationType.task).getUri())
				.hasToString("maven://org" + ".springframework.cloud.task.app:timestamp-task:5.0.0");
	}

	@Test
	void registerAllWithForce() throws Exception {
		this.appRegistryService.importAll(true, new ClassPathResource("META-INF/test-apps-overwrite.properties"));
		assertThat(this.appRegistryService.find("time", ApplicationType.source).getUri())
				.hasToString("maven://org" + ".springframework.cloud.stream.app:time-source-kafka:5.0.0");
		assertThat(this.appRegistryService.find("filter", ApplicationType.processor).getUri())
				.hasToString("maven://org" + ".springframework.cloud.stream.app:filter-processor-kafka:5.0.0");
		assertThat(this.appRegistryService.find("log", ApplicationType.sink).getUri())
				.hasToString("maven://org.springframework" + ".cloud.stream.app:log-sink-kafka:5.0.0");
		assertThat(this.appRegistryService.find("timestamp", ApplicationType.task).getUri())
				.hasToString("maven://org" + ".springframework.cloud.task.app:timestamp-overwrite-task:5.0.0");
	}

	@Test
	void registerAllWithBadApplication() throws Exception {
		mockMvc.perform(post("/apps").param("apps", "sink-foo=maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
	}

	@Test
	void listApplications() throws Exception {
		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(4)));
	}

	@Test
	void listAppsWithMultiVersions() throws Exception {
		this.appRegistryService.importAll(false, new ClassPathResource("META-INF/test-apps-multi-versions.properties"));
		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[*]", hasSize(9)));
		mockMvc.perform(get("/apps?defaultVersion=true").accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[*]", hasSize(6)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'time' && @.type == 'source')]", hasSize(1)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'time' && @.type == 'source')].defaultVersion", contains(true)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'log' && @.type == 'sink')]", hasSize(1)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'log' && @.type == 'sink')].defaultVersion", contains(true)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'file' && @.type == 'source')]", hasSize(1)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'file' && @.type == 'source')].defaultVersion", contains(true)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'file' && @.type == 'sink')]", hasSize(1)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'file' && @.type == 'sink')].defaultVersion", contains(true)))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'file' && @.type == 'source')].versions[*]",
						containsInAnyOrder("3.0.1")))
				.andExpect(jsonPath("$._embedded.appRegistrationResourceList[?(@.name == 'file' && @.type == 'sink')].versions[*]",
						containsInAnyOrder("3.0.0", "3.0.1")));
	}

	@Test
	void listApplicationsByType() throws Exception {
		mockMvc.perform(get("/apps?type=task").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(1)));
	}

	@Test
	void listApplicationsBySearch() throws Exception {
		mockMvc.perform(get("/apps?search=timestamp").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(1)));
		mockMvc.perform(get("/apps?search=time").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(2)));
	}

	@Test
	void listApplicationsByTypeAndSearch() throws Exception {
		mockMvc.perform(get("/apps?type=task&search=time").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(1)));
		mockMvc.perform(get("/apps?type=source&search=time").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(1)));
		mockMvc.perform(get("/apps?type=sink&search=time").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList").doesNotExist());
	}

	@Test
	void findNonExistentApp() throws Exception {
		mockMvc.perform(get("/apps/source/foo").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString()
				.contains("NoSuchAppRegistrationException");
	}

	@Test
	void registerAndListApplications() throws Exception {
		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(4)));
		mockMvc.perform(post("/apps/processor/blubba").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(5)));
	}

	@Test
	void listSingleApplication() throws Exception {
		mockMvc.perform(get("/apps/source/time").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("time")))
				.andExpect(jsonPath("type", is("source")))
				.andExpect(jsonPath("$.options[*]", hasSize(7)));
	}

	@Test
	void listSingleApplicationExhaustive() throws Exception {
		mockMvc.perform(get("/apps/source/time?exhaustive=true").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("time")))
				.andExpect(jsonPath("type", is("source")))
			.andExpect(jsonPath("$.options[*]", hasSize(2059)));
	}

	@Test
	@Transactional
	void unregisterApplication() throws Exception {
		mockMvc.perform(post("/apps/processor/blubba").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(delete("/apps/processor/blubba").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	@Transactional
	void unregisterAllApplications() throws Exception {
		mockMvc.perform(delete("/apps").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		mockMvc.perform(post("/apps/sink/log").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/apps/source/time").param("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/apps/source/time").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("time")))
				.andExpect(jsonPath("type", is("source")));

		mockMvc.perform(get("/apps/sink/log").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("log")))
				.andExpect(jsonPath("type", is("sink")));

		mockMvc.perform(delete("/apps").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList").doesNotExist());
	}

	@Test
	@Transactional
	void unregisterApplicationUsedInStream() throws Exception {
		setupUnregistrationTestStreams();

		// This log sink v1.2 is part of a deployed stream, so it can not be unregistered
		mockMvc.perform(delete("/apps/sink/log/1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict());

		// This log sink v1.0.BS is part of a deployed stream, so it can be unregistered
		mockMvc.perform(delete("/apps/sink/log/5.0.0").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// This time source v1.0 BS is not part of a deployed stream, so it can be unregistered
		mockMvc.perform(delete("/apps/source/time/5.0.0").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// This time source is part of a deployed stream, so it can not be unregistered.
		mockMvc.perform(delete("/apps/source/time/1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isConflict());

		// This is unrelated to a stream, so should work
		mockMvc.perform(delete("/apps/task/timestamp/1.3.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// Transformer processor is not deployed, so should work
		mockMvc.perform(delete("/apps/processor/transformer").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	@Transactional
	void unregisterAllApplicationsWhenApplicationUsedInStream() throws Exception {
		setupUnregistrationTestStreams();

		streamDefinitionRepository.deleteById("ticktock");
		skipperClient.delete("ticktock", true);

		mockMvc.perform(delete("/apps").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList").doesNotExist());
	}

	private void setupUnregistrationTestStreams() throws Exception {
		// Note, by default there are apps registered from classpath:META-INF/test-apps.properties.

		// Register time source v1.2
		mockMvc.perform(post("/apps/source/time")
				.param("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// Make sure the 1.2 time source is registered.
		mockMvc.perform(get("/apps/source/time/1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("time")))
				.andExpect(jsonPath("type", is("source")));

		// Register log sink v1.2
		mockMvc.perform(post("/apps/sink/log")
				.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// Make sure the 1.2 log sink is registered.
		mockMvc.perform(get("/apps/sink/log/1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("log")))
				.andExpect(jsonPath("type", is("sink")));

		// Register a transformer
		mockMvc.perform(post("/apps/processor/transformer")
				.param("uri", "maven://org.springframework.cloud.stream.app:transformer-processor-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// Register a task
		mockMvc.perform(post("/apps/task/timestamp")
				.param("uri", "maven://org.springframework.cloud.task.app:timestamp-task:1.3.0.RELEASE")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// Create stream definition
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time --fixed-delay=100 | log --level=DEBUG");
		streamDefinitionRepository.save(streamDefinition);

		// configure mock SkipperClient
		String expectedReleaseManifest = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(DefaultStreamServiceIntegrationTests.class,
						"deployManifest.yml").getInputStream(),
				Charset.defaultCharset());
		Release release = new Release();
		Manifest manifest = new Manifest();
		manifest.setData(expectedReleaseManifest);
		release.setManifest(manifest);
		when(skipperClient.install(isA(InstallRequest.class))).thenReturn(release);
		when(skipperClient.manifest(eq("ticktock"))).thenReturn(expectedReleaseManifest);
		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));

		// Deploy stream with time source v1.2 and log sink v1.2
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put("version.time", "1.2.0.RELEASE");
		deploymentProperties.put("version.log", "1.2.0.RELEASE");
		streamService.deployStream("ticktock", deploymentProperties);
	}

	@Test
	@Transactional
	void unregisterApplicationUsedInStreamNotDeployed() throws Exception {
		// Note, by default there are apps registered from classpath:META-INF/test-apps.properties.

		// Register time source v1.2
		mockMvc.perform(post("/apps/source/time")
				.param("uri", "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// Make sure the 1.2 time source is registered.
		mockMvc.perform(get("/apps/source/time/1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("time")))
				.andExpect(jsonPath("type", is("source")));

		// Register log sink v1.2
		mockMvc.perform(post("/apps/sink/log")
				.param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// Make sure the 1.2 log sink is registered.
		mockMvc.perform(get("/apps/sink/log/1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("log")))
				.andExpect(jsonPath("type", is("sink")));

		// Register a transformer
		mockMvc.perform(post("/apps/processor/transformer")
				.param("uri", "maven://org.springframework.cloud.stream.app:transformer-processor-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// Register a task
		mockMvc.perform(post("/apps/task/timestamp")
				.param("uri", "maven://org.springframework.cloud.task.app:timestamp-task:1.3.0.RELEASE")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());

		// Create stream definition
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time --fixed-delay=100 | log --level=DEBUG");
		streamDefinitionRepository.save(streamDefinition);

		// configure mock SkipperClient
		when(skipperClient.manifest(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));
		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));

		// This log sink v1.2 is part of a deployed stream, so it can be unregistered
		mockMvc.perform(delete("/apps/sink/log/1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// This log sink v1.0.BS is part of a deployed stream, so it can be unregistered
		mockMvc.perform(delete("/apps/sink/log/5.0.0").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// This time source v1.0 BS is not part of a deployed stream, so it can be unregistered
		mockMvc.perform(delete("/apps/source/time/5.0.0").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// This time source is part of a deployed stream, so it can not be unregistered.
		mockMvc.perform(delete("/apps/source/time/1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());


		// This is unrelated to a stream, so should work
		mockMvc.perform(delete("/apps/task/timestamp/1.3.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		// Transformer processor is not deployed, so should work
		mockMvc.perform(delete("/apps/processor/transformer").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	void unregisterUnversionedApplicationNotFound() throws Exception {
		mockMvc.perform(delete("/apps/processor/transformer").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void unregisterApplicationNotFound() throws Exception {
		mockMvc.perform(delete("/apps/processor/transformer/blubba").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void pagination() throws Exception {
		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(20)))
				.andExpect(jsonPath("page.totalElements", is(4)))
				.andExpect(jsonPath("page.totalPages", is(1)))
				.andExpect(jsonPath("page.number", is(0)));

		mockMvc.perform(get("/apps?page=0&size=10").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(10)))
				.andExpect(jsonPath("page.totalElements", is(4)))
				.andExpect(jsonPath("page.totalPages", is(1)))
				.andExpect(jsonPath("page.number", is(0)));

		mockMvc.perform(get("/apps?page=0&size=1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(1)))
				.andExpect(jsonPath("page.totalElements", is(4)))
				.andExpect(jsonPath("page.totalPages", is(4)))
				.andExpect(jsonPath("page.number", is(0)));

		mockMvc.perform(get("/apps?page=1&size=2").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(2)))
				.andExpect(jsonPath("page.totalElements", is(4)))
				.andExpect(jsonPath("page.totalPages", is(2)))
				.andExpect(jsonPath("page.number", is(1)));

		mockMvc.perform(get("/apps?page=0&size=3").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(3)))
				.andExpect(jsonPath("page.totalElements", is(4)))
				.andExpect(jsonPath("page.totalPages", is(2)))
				.andExpect(jsonPath("page.number", is(0)));

		mockMvc.perform(get("/apps?page=1&size=3").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(3)))
				.andExpect(jsonPath("page.totalElements", is(4)))
				.andExpect(jsonPath("page.totalPages", is(2)))
				.andExpect(jsonPath("page.number", is(1)));

		mockMvc.perform(get("/apps?page=5&size=2").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(2)))
				.andExpect(jsonPath("page.totalElements", is(4)))
				.andExpect(jsonPath("page.totalPages", is(2)))
				.andExpect(jsonPath("page.number", is(5)));

		mockMvc.perform(get("/apps?page=0&size=10&search=i").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(10)))
				.andExpect(jsonPath("page.totalElements", is(3)))
				.andExpect(jsonPath("page.totalPages", is(1)))
				.andExpect(jsonPath("page.number", is(0)));

		mockMvc.perform(get("/apps?page=0&size=1&search=i").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(1)))
				.andExpect(jsonPath("page.totalElements", is(3)))
				.andExpect(jsonPath("page.totalPages", is(3)))
				.andExpect(jsonPath("page.number", is(0)));

		mockMvc.perform(get("/apps?page=1&size=2&search=i").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(2)))
				.andExpect(jsonPath("page.totalElements", is(3)))
				.andExpect(jsonPath("page.totalPages", is(2)))
				.andExpect(jsonPath("page.number", is(1)));

		mockMvc.perform(get("/apps?page=5&size=2&search=i").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("page.size", is(2)))
				.andExpect(jsonPath("page.totalElements", is(3)))
				.andExpect(jsonPath("page.totalPages", is(2)))
				.andExpect(jsonPath("page.number", is(5)));
	}

	@Test

	void listApplicationsByVersion() throws Exception {
		mockMvc.perform(get("/apps?version=5.0.0").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(4)));
	}

	@Test
	void listApplicationsByVersionAndSearch() throws Exception {
		mockMvc.perform(get("/apps?version=5.0.0&search=time").accept(MediaType.APPLICATION_JSON))
			.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(2)));
		mockMvc.perform(get("/apps?version=5.0.0&search=timestamp").accept(MediaType.APPLICATION_JSON))
			.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("_embedded.appRegistrationResourceList", hasSize(1)));
	}

}
