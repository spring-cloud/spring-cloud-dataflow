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

package org.springframework.cloud.dataflow.server.controller;

import javax.transaction.Transactional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.support.NoSuchAppRegistrationException;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.registry.DataFlowAppRegistryPopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class, properties = {"spring.cloud.dataflow.features.skipper-enabled=true",
		"spring.datasource.url=jdbc:h2:tcp://localhost:19092/mem:dataflow"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class VersionedAppRegistryControllerTests {

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppRegistryService appRegistryService;

	@Autowired
	private DataFlowAppRegistryPopulator uriRegistryPopulator;

	@Before
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		for (AppRegistration appRegistration : this.appRegistryService.findAll()) {
			this.appRegistryService.delete(appRegistration.getName(), appRegistration.getType(), appRegistration.getVersion());
		}
		this.uriRegistryPopulator.afterPropertiesSet();
	}

	@Test
	public void testRegisterApplication() throws Exception {
		mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		assertThat(this.appRegistryService.find("log1", ApplicationType.sink).getUri().toString(), is("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE"));
	}

	@Test
	public void testAppInfoNonExistingApp() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/apps/sink/log1")).andDo(print()).andExpect(status().is4xxClientError()).andReturn();
		Assert.isInstanceOf(NoSuchAppRegistrationException.class, mvcResult.getResolvedException());
	}

	@Test
	public void testDefaultVersion() throws Exception {
		this.mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		MvcResult mvcResult = this.mockMvc.perform(get("/apps/sink/log1")).andDo(print()).andExpect(status().isOk()).andReturn();
		Assert.isTrue(mvcResult.getResponse().getContentAsString().contains("{\"name\":\"log1\",\"type\":\"sink\"," +
				"\"uri\":\"maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE\"," +
				"\"version\":\"1.2.0.RELEASE\",\"defaultVersion\":true"), "Default version is incorrect");
	}

	@Test
	public void testVersionOverride() throws Exception {
		this.mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		this.mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.3.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		this.mockMvc.perform(put("/apps/sink/log1/1.3.0.RELEASE")).andDo(print()).andExpect(status().isAccepted());
		MvcResult mvcResult = this.mockMvc.perform(get("/apps/sink/log1")).andDo(print()).andExpect(status().isOk()).andReturn();
		Assert.isTrue(mvcResult.getResponse().getContentAsString().contains("{\"name\":\"log1\",\"type\":\"sink\"," +
				"\"uri\":\"maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.3.0.RELEASE\"," +
				"\"version\":\"1.3.0.RELEASE\",\"defaultVersion\":true"), "Make version as default is incorrect");
	}

	@Test
	public void testVersionOverrideNonExistentApp() throws Exception {
		this.mockMvc.perform(post("/apps/sink/log1").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		MvcResult mvcResult = this.mockMvc.perform(put("/apps/sink/log1/1.3.0.RELEASE/")).andDo(print()).andExpect(status().is4xxClientError()).andReturn();
		Assert.isInstanceOf(NoSuchAppRegistrationException.class, mvcResult.getResolvedException());
	}

	@Test
	public void testRegisterApplicationTwice() throws Exception {
		mockMvc.perform(post("/apps/processor/blubba").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/apps/processor/blubba").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isConflict());
	}

	@Test
	public void testRegisterAll() throws Exception {
		mockMvc.perform(post("/apps").param("apps", "sink.foo=maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());
		assertThat(this.appRegistryService.find("foo", ApplicationType.sink).getUri().toString(), is("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE"));
	}

	@Test
	public void testRegisterAllWithoutForce() throws Exception {
		this.appRegistryService.importAll(false, new ClassPathResource("META-INF/test-apps-overwrite.properties"));
		assertThat(this.appRegistryService.find("time", ApplicationType.source).getUri().toString(),
				is("maven://org" + ".springframework.cloud.stream.app:time-source-rabbit:1.0.0.BUILD-SNAPSHOT"));
		assertThat(this.appRegistryService.find("filter", ApplicationType.processor).getUri().toString(),
				is("maven://org" + ".springframework.cloud.stream.app:filter-processor-rabbit:1.0.0.BUILD-SNAPSHOT"));
		assertThat(this.appRegistryService.find("log", ApplicationType.sink).getUri().toString(),
				is("maven://org.springframework" + ".cloud.stream.app:log-sink-rabbit:1.0.0.BUILD-SNAPSHOT"));
		assertThat(this.appRegistryService.find("timestamp", ApplicationType.task).getUri().toString(),
				is("maven://org" + ".springframework.cloud.task.app:timestamp-task:1.0.0.BUILD-SNAPSHOT"));
	}

	@Test
	public void testRegisterAllWithForce() throws Exception {
		this.appRegistryService.importAll(true, new ClassPathResource("META-INF/test-apps-overwrite.properties"));
		assertThat(this.appRegistryService.find("time", ApplicationType.source).getUri().toString(),
				is("maven://org" + ".springframework.cloud.stream.app:time-source-kafka:1.0.0.BUILD-SNAPSHOT"));
		assertThat(this.appRegistryService.find("filter", ApplicationType.processor).getUri().toString(),
				is("maven://org" + ".springframework.cloud.stream.app:filter-processor-kafka:1.0.0.BUILD-SNAPSHOT"));
		assertThat(this.appRegistryService.find("log", ApplicationType.sink).getUri().toString(),
				is("maven://org.springframework" + ".cloud.stream.app:log-sink-kafka:1.0.0.BUILD-SNAPSHOT"));
		assertThat(this.appRegistryService.find("timestamp", ApplicationType.task).getUri().toString(),
				is("maven://org" + ".springframework.cloud.task.app:timestamp-overwrite-task:1.0.0.BUILD-SNAPSHOT"));
	}

	@Test
	public void testRegisterAllWithBadApplication() throws Exception {
		mockMvc.perform(post("/apps").param("apps", "sink-foo=maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError());
	}

	@Test
	public void testListApplications() throws Exception {
		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(4)));
	}

	@Test
	public void testListApplicationsByType() throws Exception {
		mockMvc.perform(get("/apps?type=task").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(1)));
	}

	@Test
	public void testListApplicationsBySearch() throws Exception {
		mockMvc.perform(get("/apps?search=timestamp").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(1)));
		mockMvc.perform(get("/apps?search=time").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(2)));
	}

	@Test
	public void testListApplicationsByTypeAndSearch() throws Exception {
		mockMvc.perform(get("/apps?type=task&search=time").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(1)));
		mockMvc.perform(get("/apps?type=source&search=time").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(1)));
		mockMvc.perform(get("/apps?type=sink&search=time").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(0)));
	}

	@Test
	public void testFindNonExistentApp() throws Exception {
		mockMvc.perform(get("/apps/source/foo").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString()
				.contains("NoSuchAppRegistrationException");
	}

	@Test
	public void testRegisterAndListApplications() throws Exception {
		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(4)));
		mockMvc.perform(post("/apps/processor/blubba").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/apps").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("content", hasSize(5)));
	}

	@Test
	public void testListSingleApplication() throws Exception {
		mockMvc.perform(get("/apps/source/time").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("name", is("time")))
				.andExpect(jsonPath("type", is("source")));
	}

	@Test
	@Transactional
	public void testUnregisterApplication() throws Exception {
		mockMvc.perform(post("/apps/processor/blubba").param("uri", "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
		mockMvc.perform(delete("/apps/processor/blubba").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	public void testUnregisterApplicationNotFound() throws Exception {
		mockMvc.perform(delete("/apps/processor/blubba").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testPagination() throws Exception {
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

}
