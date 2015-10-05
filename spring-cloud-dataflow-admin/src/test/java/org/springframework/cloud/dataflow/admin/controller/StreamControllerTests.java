/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.admin.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.dataflow.admin.AdminApplication;
import org.springframework.cloud.dataflow.admin.configuration.TestDependencies;
import org.springframework.cloud.dataflow.admin.repository.InMemoryStreamDefinitionRepository;
import org.springframework.cloud.dataflow.admin.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.core.BindingProperties;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.module.deployer.local.LocalModuleDeployer;
import org.springframework.cloud.dataflow.module.registry.InMemoryModuleRegistry;
import org.springframework.cloud.stream.module.launcher.ModuleLaunchRequest;
import org.springframework.cloud.stream.module.launcher.ModuleLauncher;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {AdminApplication.class, TestDependencies.class})
@WebAppConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class StreamControllerTests {

	@Autowired
	private StreamDefinitionRepository repository;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private ModuleLauncher moduleLauncher;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@After
	public void tearDown() {
		repository.deleteAll();
		assertEquals(0, repository.count());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingRepository() {
		new StreamController(null, new InMemoryModuleRegistry(), new LocalModuleDeployer(moduleLauncher));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingRegistry() {
		new StreamController(new InMemoryStreamDefinitionRepository(), null, new LocalModuleDeployer(moduleLauncher));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingDeployer() {
		new StreamController(new InMemoryStreamDefinitionRepository(), new InMemoryModuleRegistry(), null);
	}

	@Test
	public void testSave() throws Exception {
		assertEquals(0, repository.count());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", "time | log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isCreated());
		assertEquals(1, repository.count());
		StreamDefinition myStream = repository.findOne("myStream");
		assertEquals("time | log", myStream.getDslText());
		assertEquals("myStream", myStream.getName());
		assertEquals(2, myStream.getModuleDefinitions().size());
		ModuleDefinition timeDefinition = myStream.getModuleDefinitions().get(0);
		ModuleDefinition logDefinition = myStream.getModuleDefinitions().get(1);
		assertEquals(1, timeDefinition.getParameters().size());
		assertEquals("myStream.0", timeDefinition.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		assertEquals(1, logDefinition.getParameters().size());
		assertEquals("myStream.0", logDefinition.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
	}

	@Test
	public void testSaveDuplicate() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		assertEquals(1, repository.count());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", "time | log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().is5xxServerError());
		assertEquals(1, repository.count());
	}

	@Test
	public void testSaveWithParameters() throws Exception {
		assertEquals(0, repository.count());
		String definition = "time --fixedDelay=500 --timeUnit=milliseconds | log";
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream")
						.param("definition", definition)
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		assertEquals(1, repository.count());
		StreamDefinition myStream = repository.findOne("myStream");
		ModuleDefinition timeDefinition = myStream.getModuleDefinitions().get(0);
		ModuleDefinition logDefinition = myStream.getModuleDefinitions().get(1);
		assertEquals("time", timeDefinition.getName());
		assertEquals("log", logDefinition.getName());
		assertEquals("500", timeDefinition.getParameters().get("fixedDelay"));
		assertEquals("milliseconds", timeDefinition.getParameters().get("timeUnit"));
		assertEquals(definition, myStream.getDslText());
		assertEquals("myStream", myStream.getName());
	}

	@Test
	public void testStreamWithProcessor() throws Exception {
		assertEquals(0, repository.count());
		String definition = "time | filter | log";
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", definition)
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isCreated());
		assertEquals(1, repository.count());
		StreamDefinition myStream = repository.findOne("myStream");
		assertEquals(definition, myStream.getDslText());
		assertEquals("myStream", myStream.getName());
		assertEquals(3, myStream.getModuleDefinitions().size());
		ModuleDefinition timeDefinition = myStream.getModuleDefinitions().get(0);
		ModuleDefinition filterDefinition = myStream.getModuleDefinitions().get(1);
		ModuleDefinition logDefinition = myStream.getModuleDefinitions().get(2);
		assertEquals(1, timeDefinition.getParameters().size());
		assertEquals("myStream.0", timeDefinition.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		assertEquals(2, filterDefinition.getParameters().size());
		assertEquals("myStream.0", filterDefinition.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("myStream.1", filterDefinition.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		assertEquals(1, logDefinition.getParameters().size());
		assertEquals("myStream.1", logDefinition.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
	}

	@Test
	public void testSourceChannelWithSingleModule() throws Exception {
		assertEquals(0, repository.count());
		String definition = "queue:foo > log";
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", definition)
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isCreated());
		assertEquals(1, repository.count());
		StreamDefinition myStream = repository.findOne("myStream");
		assertEquals(definition, myStream.getDslText());
		assertEquals("myStream", myStream.getName());
		assertEquals(1, myStream.getModuleDefinitions().size());
		ModuleDefinition logDefinition = myStream.getModuleDefinitions().get(0);
		assertEquals(1, logDefinition.getParameters().size());
		assertEquals("queue:foo", logDefinition.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
	}

	@Test
	public void testSourceChannelWithTwoModules() throws Exception {
		assertEquals(0, repository.count());
		String definition = "queue:foo > filter | log";
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", definition)
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isCreated());
		assertEquals(1, repository.count());
		StreamDefinition myStream = repository.findOne("myStream");
		assertEquals(definition, myStream.getDslText());
		assertEquals("myStream", myStream.getName());
		assertEquals(2, myStream.getModuleDefinitions().size());
		ModuleDefinition filterDefinition = myStream.getModuleDefinitions().get(0);
		assertEquals(2, filterDefinition.getParameters().size());
		assertEquals("queue:foo", filterDefinition.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("myStream.0", filterDefinition.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		ModuleDefinition logDefinition = myStream.getModuleDefinitions().get(1);
		assertEquals(1, logDefinition.getParameters().size());
		assertEquals("myStream.0", logDefinition.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
	}

	@Test
	public void testSinkChannelWithSingleModule() throws Exception {
		assertEquals(0, repository.count());
		String definition = "time > queue:foo";
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", definition)
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isCreated());
		assertEquals(1, repository.count());
		StreamDefinition myStream = repository.findOne("myStream");
		assertEquals(definition, myStream.getDslText());
		assertEquals("myStream", myStream.getName());
		assertEquals(1, myStream.getModuleDefinitions().size());
		ModuleDefinition timeDefinition = myStream.getModuleDefinitions().get(0);
		assertEquals(1, timeDefinition.getParameters().size());
		assertEquals("queue:foo", timeDefinition.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
	}

	@Test
	public void testSinkChannelWithTwoModules() throws Exception {
		assertEquals(0, repository.count());
		String definition = "time | filter > queue:foo";
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", definition)
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isCreated());
		assertEquals(1, repository.count());
		StreamDefinition myStream = repository.findOne("myStream");
		assertEquals(definition, myStream.getDslText());
		assertEquals("myStream", myStream.getName());
		assertEquals(2, myStream.getModuleDefinitions().size());
		ModuleDefinition timeDefinition = myStream.getModuleDefinitions().get(0);
		assertEquals(1, timeDefinition.getParameters().size());
		assertEquals("myStream.0", timeDefinition.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
		ModuleDefinition filterDefinition = myStream.getModuleDefinitions().get(1);
		assertEquals(2, filterDefinition.getParameters().size());
		assertEquals("myStream.0", filterDefinition.getParameters().get(BindingProperties.INPUT_BINDING_KEY));
		assertEquals("queue:foo", filterDefinition.getParameters().get(BindingProperties.OUTPUT_BINDING_KEY));
	}

	@Test
	public void testDestroyStream() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		assertEquals(1, repository.count());
		mockMvc.perform(
				delete("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		assertEquals(0, repository.count());
	}

	@Test
	public void testDestroyStreamNotFound() throws Exception {
		mockMvc.perform(
				delete("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().is5xxServerError());
		assertEquals(0, repository.count());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testDeploy() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<List> requests = ArgumentCaptor.forClass(List.class);
		verify(moduleLauncher, times(2)).launch(requests.capture());
		Iterator<List> iterator = requests.getAllValues().iterator();
		assertThat(iterator.hasNext(), equalTo(true));
		List list1 = iterator.next();
		assertEquals(1, list1.size());
		ModuleLaunchRequest request1 = (ModuleLaunchRequest) list1.get(0);
		assertThat(request1.getModule(), containsString("log-sink"));
		assertThat(iterator.hasNext(), equalTo(true));
		List list2 = iterator.next();
		assertEquals(1, list2.size());
		ModuleLaunchRequest request2 = (ModuleLaunchRequest) list2.get(0);
		assertThat(request2.getModule(), containsString("time-source"));
		assertThat(iterator.hasNext(), equalTo(false));
	}
}
