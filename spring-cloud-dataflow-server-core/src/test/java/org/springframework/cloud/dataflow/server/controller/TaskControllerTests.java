/*
 * Copyright 2015-2017 the original author or authors.
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

import java.net.URI;
import java.util.Arrays;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.InMemoryTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Michael Minella
 * @author Mark Fisher
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TaskControllerTests {

	@Autowired
	TaskService taskService;

	@Autowired
	private TaskDefinitionRepository repository;

	@Autowired
	private UriRegistry registry;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private TaskLauncher taskLauncher;

	@Autowired
	private AppRegistry appRegistry;

	@Before
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		when(taskLauncher.launch(any(AppDeploymentRequest.class))).thenReturn("testID");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskDefinitionControllerConstructorMissingRepository() {
		new TaskDefinitionController(null, null, taskLauncher, appRegistry, taskService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskDefinitionControllerConstructorMissingDeployer() {
		new TaskDefinitionController(new InMemoryTaskDefinitionRepository(), null, null, appRegistry, taskService);
	}

	@Test
	public void testTaskLaunchWithNullIDReturned() throws Exception {
		when(taskLauncher.launch(any(AppDeploymentRequest.class))).thenReturn(null);
		repository.save(new TaskDefinition("myTask", "foo"));
		this.registry.register("task.foo", new URI("maven://org.springframework.cloud:foo:1"));

		mockMvc.perform(post("/tasks/executions").param("name", "myTask").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isInternalServerError());
	}

	@Test
	public void testSaveErrorNotInRegistry() throws Exception {
		assertEquals(0, repository.count());

		mockMvc.perform(post("/tasks/definitions/").param("name", "myTask").param("definition", "task")
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isNotFound());

		assertEquals(0, repository.count());
	}

	@Test
	public void testSave() throws Exception {
		assertEquals(0, repository.count());
		appRegistry.save("task", ApplicationType.task, new URI("https://fake.example.com/"), null);
		mockMvc.perform(post("/tasks/definitions/").param("name", "myTask").param("definition", "task")
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());

		assertEquals(1, repository.count());

		TaskDefinition myTask = repository.findOne("myTask");

		assertEquals(1, myTask.getProperties().size());
		assertEquals("myTask", myTask.getProperties().get("spring.cloud.task.name"));
		assertEquals("task", myTask.getDslText());
		assertEquals("myTask", myTask.getName());
	}

	@Test
	public void testSaveDuplicate() throws Exception {
		appRegistry.save("task", ApplicationType.task, new URI("https://fake.example.com/"), null);
		repository.save(new TaskDefinition("myTask", "task"));
		mockMvc.perform(post("/tasks/definitions/").param("name", "myTask").param("definition", "task")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
		assertEquals(1, repository.count());
	}

	@Test
	public void testSaveWithParameters() throws Exception {

		appRegistry.save("task", ApplicationType.task, new URI("https://fake.example.com/"), null);
		mockMvc.perform(post("/tasks/definitions/").param("name", "myTask")
				.param("definition", "task --foo=bar --bar=baz").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());

		assertEquals(1, repository.count());

		TaskDefinition myTask = repository.findOne("myTask");

		assertEquals("bar", myTask.getProperties().get("foo"));
		assertEquals("baz", myTask.getProperties().get("bar"));
		assertEquals("task --foo=bar --bar=baz", myTask.getDslText());
		assertEquals("task", myTask.getRegisteredAppName());
		assertEquals("myTask", myTask.getName());
	}

	@Test
	public void testSaveCompositeTaskWithParameters() throws Exception {

		appRegistry.save("task", ApplicationType.task, new URI("https://fake.example.com/"), null);
		mockMvc.perform(post("/tasks/definitions/").param("name", "myTask")
				.param("definition", "t1: task --foo='bar rab' && t2: task --foo='one two'").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());

		assertEquals(3, repository.count());

		TaskDefinition myTask1 = repository.findOne("myTask-t1");
		assertEquals("bar rab", myTask1.getProperties().get("foo"));
		assertEquals("task --foo='bar rab'", myTask1.getDslText());
		assertEquals("task", myTask1.getRegisteredAppName());
		assertEquals("myTask-t1", myTask1.getName());

		TaskDefinition myTask2 = repository.findOne("myTask-t2");
		assertEquals("one two", myTask2.getProperties().get("foo"));
		assertEquals("task --foo='one two'", myTask2.getDslText());
		assertEquals("task", myTask2.getRegisteredAppName());
		assertEquals("myTask-t2", myTask2.getName());
	}

	@Test
	public void testDestroyTask() throws Exception {
		repository.save(new TaskDefinition("myTask", "task"));

		mockMvc.perform(delete("/tasks/definitions/myTask").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());

		assertEquals(0, repository.count());
		Mockito.verify(taskLauncher).destroy("myTask");
	}

	@Test
	public void testDestroyTaskNotFound() throws Exception {
		mockMvc.perform(delete("/tasks/definitions/myTask").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isNotFound());
		assertEquals(0, repository.count());
	}

	@Test
	public void testMissingApplication() throws Exception {
		repository.save(new TaskDefinition("myTask", "no-such-task-app"));

		mockMvc.perform(post("/tasks/executions").param("name", "myTask").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().is5xxServerError())
				.andExpect(content().json("[{message: \"Unknown task app: no-such-task-app\"}]"));
	}

	@Test
	public void testTaskNotDefined() throws Exception {
		mockMvc.perform(post("/tasks/executions")
				.param("name", "myFoo").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isNotFound())
				.andExpect(content().json("[{message: \"Could not find task definition named myFoo\"}]"));
	}

	@Test
	public void testLaunch() throws Exception {
		repository.save(new TaskDefinition("myTask", "foo"));
		this.registry.register("task.foo", new URI("file:src/test/resources/apps/foo-task"));

		mockMvc.perform(post("/tasks/executions").param("name", "myTask").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());

		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

		AppDeploymentRequest request = argumentCaptor.getValue();
		assertEquals("myTask", request.getDefinition().getProperties().get("spring.cloud.task.name"));
	}

	@Test
	public void testLaunchWithAppProperties() throws Exception {

		repository.save(new TaskDefinition("myTask2", "foo2 --common.prop2=wizz"));
		this.registry.register("task.foo2", new URI("file:src/test/resources/apps/foo-task"));

		mockMvc.perform(post("/tasks/executions").param("name", "myTask2")
				.accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isCreated());

		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

		AppDeploymentRequest request = argumentCaptor.getValue();
		assertThat(request.getDefinition().getProperties(), hasEntry("common.prop2", "wizz"));
		assertEquals("myTask2", request.getDefinition().getProperties().get("spring.cloud.task.name"));
	}

	@Test
	public void testLaunchWithArguments() throws Exception {
		repository.save(new TaskDefinition("myTask3", "foo3"));
		this.registry.register("task.foo3", new URI("file:src/test/resources/apps/foo-task"));

		mockMvc.perform(post("/tasks/executions")
				//.param("name", "myTask3")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content(EntityUtils.toString(new UrlEncodedFormEntity(Arrays.asList(
						new BasicNameValuePair("name", "myTask3"),
						new BasicNameValuePair("arguments", "--foobar=jee --foobar2=jee2,foo=bar --foobar3='jee3 jee3'")))))
				.accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isCreated());

		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

		AppDeploymentRequest request = argumentCaptor.getValue();
		assertThat(request.getCommandlineArguments().size(), is(3 + 1)); // +1 for spring.cloud.task.executionid
		assertThat(request.getCommandlineArguments().get(0), is("--foobar=jee"));
		assertThat(request.getCommandlineArguments().get(1), is("--foobar2=jee2,foo=bar"));
		assertThat(request.getCommandlineArguments().get(2), is("--foobar3=jee3 jee3"));
		assertEquals("myTask3", request.getDefinition().getProperties().get("spring.cloud.task.name"));
	}

	@Test
	public void testDisplaySingleTask() throws Exception {
		TaskDefinition taskDefinition = new TaskDefinition("myTask", "timestamp");
		repository.save(taskDefinition);
		assertEquals(1, repository.count());
		mockMvc.perform(get("/tasks/definitions/myTask").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().json("{name: \"myTask\"}")).andExpect(content().json("{dslText: \"timestamp\"}"));
	}

	@Test
	public void testDisplaySingleTaskNotFound() throws Exception {
		mockMvc.perform(get("/tasks/definitions/myTask").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}
}
