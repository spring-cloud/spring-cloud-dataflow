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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.dataflow.admin.configuration.TestDependencies;
import org.springframework.cloud.dataflow.admin.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.admin.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.artifact.registry.InMemoryArtifactRegistry;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Michael Minella
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestDependencies.class)
@WebAppConfiguration
public class TaskControllerTests {

	@Autowired
	private TaskDefinitionRepository repository;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	@Qualifier("taskModuleDeployer")
	private ModuleDeployer moduleDeployer;

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
		new TaskController(null, new InMemoryArtifactRegistry(), moduleDeployer);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingRegistry() {
		new TaskController(new InMemoryTaskDefinitionRepository(), null, moduleDeployer);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingDeployer() {
		new TaskController(new InMemoryTaskDefinitionRepository(), new InMemoryArtifactRegistry(), null);
	}

	@Test
	public void testSave() throws Exception {
		assertEquals(0, repository.count());

		mockMvc.perform(
				post("/tasks/definitions/").param("name", "myTask").param("definition", "task")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isOk());

		assertEquals(1, repository.count());

		TaskDefinition myTask = repository.findOne("myTask");

		assertTrue(CollectionUtils.isEmpty(myTask.getParameters()));
		assertEquals("task", myTask.getDslText());
		assertEquals("myTask", myTask.getName());
	}

	@Test
	public void testSaveDuplicate() throws Exception {

		repository.save(new TaskDefinition("myTask", "task"));

		//TODO: Make this into an elegant error...
		mockMvc.perform(
				post("/tasks/definitions/").param("name", "myTask").param("definition", "task")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().is5xxServerError());

		assertEquals(1, repository.count());
	}

	@Test
	public void testSaveWithParameters() throws Exception {
		assertEquals(0, repository.count());

		mockMvc.perform(
				post("/tasks/definitions/").param("name", "myTask")
						.param("definition", "task --foo=bar --bar=baz")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());

		assertEquals(1, repository.count());

		TaskDefinition myTask = repository.findOne("myTask");

		assertEquals("bar", myTask.getParameters().get("foo"));
		assertEquals("baz", myTask.getParameters().get("bar"));
		assertEquals("task --foo=bar --bar=baz", myTask.getDslText());
		assertEquals("task", myTask.getModuleDefinition().getName());
		assertEquals("myTask", myTask.getName());
	}

	@Test
	public void testDestroyTask() throws Exception {
		repository.save(new TaskDefinition("myTask", "task"));

		mockMvc.perform(
				delete("/tasks/definitions/myTask").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());

		assertEquals(0, repository.count());
	}

	@Test
	public void testDestroyTaskNotFound() throws Exception {
		mockMvc.perform(
				delete("/tasks/definitions/myTask").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());

		assertEquals(0, repository.count());
	}

	@Test
	public void testDeploy() throws Exception {
		repository.save(new TaskDefinition("myTask", "timestamp"));

		mockMvc.perform(
				post("/tasks/deployments/myTask").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());

		ArgumentCaptor<ModuleDeploymentRequest> captor = ArgumentCaptor.forClass(ModuleDeploymentRequest.class);
		verify(moduleDeployer).deploy(captor.capture());
		ModuleDeploymentRequest request = captor.getValue();
		assertThat(request.getDefinition().getName(), is("timestamp"));
	}
}
