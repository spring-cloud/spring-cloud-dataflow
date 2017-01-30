/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.services.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.server.configuration.TaskSeviceDependencies;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class,
		TaskSeviceDependencies.class})
public class DefaultTaskServiceTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	TaskRepository taskExecutionRepository;

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Autowired
	private TaskExplorer taskExplorer;

	private UriRegistry uriRegistry;

	private ResourceLoader resourceLoader;

	private TaskLauncher taskLauncher;

	private ApplicationConfigurationMetadataResolver metadataResolver;

	private TaskService taskService;


	@Before
	public void setupMockMVC() {
		taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		uriRegistry = mock(UriRegistry.class);
		resourceLoader = mock(ResourceLoader.class);
		metadataResolver = mock(ApplicationConfigurationMetadataResolver.class);
		taskLauncher = mock(TaskLauncher.class);
		when(this.uriRegistry.find(anyString())).thenReturn(
				URI.create("http://helloworld"));
		when(this.resourceLoader.getResource(anyString())).
				thenReturn(mock(Resource.class));
		taskService =
				new DefaultTaskService(dataSourceProperties,
						taskDefinitionRepository, taskExplorer,
						taskExecutionRepository, uriRegistry, resourceLoader,
						taskLauncher, metadataResolver);
	}

	@Test
	@DirtiesContext
	public void executeSingleTaskTest() {
		when(taskLauncher.launch(anyObject())).thenReturn("0");
		assertEquals(0L, taskService.executeTask(TASK_NAME_ORIG,
				new HashMap<>(), new LinkedList<>()));
	}

	@Test
	@DirtiesContext
	public void executeMultipleTasksTest() {
		when(taskLauncher.launch(anyObject())).thenReturn("0");
		assertEquals(0L, taskService.executeTask(TASK_NAME_ORIG,
				new HashMap<>(), new LinkedList<>()));
		assertEquals(1L, taskService.executeTask(TASK_NAME_ORIG,
				new HashMap<>(), new LinkedList<>()));
	}

	@Test
	@DirtiesContext
	public void executeTaskWithNullIDReturnedTest() {
		boolean errorCaught = false;
		when(this.taskLauncher.launch(anyObject())).thenReturn(null);
		try {
			taskService.executeTask(TASK_NAME_ORIG, new HashMap<>(),
					new LinkedList<>());
		}
		catch(IllegalStateException ise) {
			errorCaught = true;
			assertEquals("Deployment ID is null for the task:myTask_ORIG", ise.getMessage());
		}
		if(!errorCaught) {
			fail();
		}
	}

	@Test
	@DirtiesContext
	public void executeTaskWithNullDefinitionTest() {
		boolean errorCaught = false;
		when(this.taskLauncher.launch(anyObject())).thenReturn("0");
		TaskService taskService =
				new DefaultTaskService(this.dataSourceProperties,
						mock(TaskDefinitionRepository.class), this.taskExplorer,
						this.taskExecutionRepository, this.uriRegistry,
						this.resourceLoader, this.taskLauncher,
						this.metadataResolver);
		try {
			taskService.executeTask(TASK_NAME_ORIG, new HashMap<>(),
					new LinkedList<>());
		}
		catch(NoSuchTaskDefinitionException ise) {
			errorCaught = true;
			assertEquals("Could not find task definition named myTask_ORIG", ise.getMessage());
		}
		if(!errorCaught) {
			fail();
		}
	}
}
