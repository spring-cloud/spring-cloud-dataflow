/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.InMemoryAppDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Michael Minella
 * @author Mark Fisher
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestDependencies.class)
@WebAppConfiguration
public class TaskControllerTests {

	@Autowired
	private TaskDefinitionRepository repository;

	@Autowired
	private UriRegistry registry;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private TaskLauncher taskLauncher;

	private final MavenProperties mavenProperties = new MavenProperties();

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
		when(taskLauncher.launch(any(AppDeploymentRequest.class))).thenReturn("testID");
	}

	@After
	public void tearDown() {
		repository.deleteAll();
		assertEquals(0, repository.count());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskDeploymentControllerConstructorMissingRepository() {
		new TaskDeploymentController(null, null, new InMemoryUriRegistry(),
				new MavenResourceLoader(mavenProperties), taskLauncher);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskDeploymentControllerConstructorMissingRegistry() {
		new TaskDeploymentController(new InMemoryTaskDefinitionRepository(), new InMemoryAppDeploymentRepository(), null,
				new MavenResourceLoader(mavenProperties), taskLauncher);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskDeploymentControllerConstructorMissingLauncher() {
		new TaskDeploymentController(new InMemoryTaskDefinitionRepository(), new InMemoryAppDeploymentRepository(),
				new InMemoryUriRegistry(), new MavenResourceLoader(mavenProperties), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskDefinitionControllerConstructorMissingRepository() {
		new TaskDefinitionController(null, taskLauncher);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskDefinitionControllerConstructorMissingDeployer() {
		new TaskDefinitionController(new InMemoryTaskDefinitionRepository(), null);
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

		assertEquals(1, myTask.getParameters().size());
		assertEquals("myTask", myTask.getParameters().get("spring.cloud.task.name"));
		assertEquals("task", myTask.getDslText());
		assertEquals("myTask", myTask.getName());
	}

	@Test
	public void testSaveDuplicate() throws Exception {

		repository.save(new TaskDefinition("myTask", "task"));

		mockMvc.perform(
				post("/tasks/definitions/").param("name", "myTask").param("definition", "task")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isConflict());

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
				.andExpect(status().isNotFound());
		assertEquals(0, repository.count());
	}

	@Test
	public void testMissingModule() throws Exception {
		repository.save(new TaskDefinition("myTask", "nosuchtaskmodule"));

		mockMvc.perform(
				post("/tasks/deployments/{name}", "myTask").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().is5xxServerError())
			.andExpect(content().json("[{message: \"No URI found for task.nosuchtaskmodule\"}]"));
	}

	@Test
	public void testDeployNotDefined() throws Exception {
		mockMvc.perform(
				post("/tasks/deployments/{name}", "myFoo")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(content().json("[{message: \"Could not find task definition named myFoo\"}]"));
	}

	@Test
	public void testLaunch() throws Exception {
		repository.save(new TaskDefinition("myTask", "foo"));
		this.registry.register("task.foo", new URI("maven://org.springframework.cloud:foo:1"));

		mockMvc.perform(
				post("/tasks/deployments/{name}", "myTask").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());

		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher).launch(argumentCaptor.capture());

		AppDeploymentRequest request = argumentCaptor.getValue();
		assertThat(request.getResource(), instanceOf(MavenResource.class));
		MavenResource mavenResource = (MavenResource) request.getResource();
		assertEquals("org.springframework.cloud", mavenResource.getGroupId());
		assertEquals("foo", mavenResource.getArtifactId());
		assertEquals("", mavenResource.getClassifier());
		assertEquals("jar", mavenResource.getExtension());
		assertEquals("1", mavenResource.getVersion());
		assertEquals("myTask", request.getDefinition().getProperties().get("spring.cloud.task.name"));
	}
}
