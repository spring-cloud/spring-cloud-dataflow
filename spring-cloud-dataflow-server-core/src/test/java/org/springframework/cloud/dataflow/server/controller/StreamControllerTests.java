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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.deployed;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.error;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.failed;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.partial;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.undeployed;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.unknown;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.dataflow.artifact.registry.AppRegistry;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.InMemoryDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.InMemoryStreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.registry.InMemoryUriRegistry;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
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
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestDependencies.class)
@WebAppConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class StreamControllerTests {

	@Autowired
	private StreamDefinitionRepository repository;

	@Autowired
	private DeploymentIdRepository deploymentIdRepository;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppDeployer appDeployer;

	private final AppRegistry appRegistry = new AppRegistry(
			new InMemoryUriRegistry(),
			new MavenResourceLoader(new MavenProperties())
	);

	@Before
	public void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultRequest(
				get("/").accept(MediaType.APPLICATION_JSON)).build();
		when(appDeployer.deploy(any(AppDeploymentRequest.class))).thenReturn("testID");
	}

	@After
	public void tearDown() {
		repository.deleteAll();
		assertEquals(0, repository.count());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingRepository() {
		StreamDeploymentController deploymentController = new StreamDeploymentController(new InMemoryStreamDefinitionRepository(),
				new InMemoryDeploymentIdRepository(), appRegistry, appDeployer);
		new StreamDefinitionController(null, null, deploymentController, appDeployer);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingDeploymentController() {
		new StreamDefinitionController(new InMemoryStreamDefinitionRepository(), new InMemoryDeploymentIdRepository(), null, appDeployer);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingDeployer() {
		StreamDeploymentController deploymentController = new StreamDeploymentController(new InMemoryStreamDefinitionRepository(),
				new InMemoryDeploymentIdRepository(), appRegistry, appDeployer);
		new StreamDefinitionController(new InMemoryStreamDefinitionRepository(), new InMemoryDeploymentIdRepository(), deploymentController, null);
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
		assertEquals(2, timeDefinition.getParameters().size());
		assertEquals("myStream.time", timeDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", timeDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertEquals(2, logDefinition.getParameters().size());
		assertEquals("myStream.time", logDefinition.getParameters().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", logDefinition.getParameters().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void testSaveDuplicate() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		assertEquals(1, repository.count());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", "time | log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isConflict());
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
		assertEquals(2, timeDefinition.getParameters().size());
		assertEquals("myStream.time", timeDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", timeDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertEquals(4, filterDefinition.getParameters().size());
		assertEquals("myStream.time", filterDefinition.getParameters().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getParameters().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("myStream.filter", filterDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertEquals(2, logDefinition.getParameters().size());
		assertEquals("myStream.filter", logDefinition.getParameters().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", logDefinition.getParameters().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void testSourceDestinationWithSingleModule() throws Exception {
		assertEquals(0, repository.count());
		String definition = ":foo > log";
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
		assertEquals(2, logDefinition.getParameters().size());
		assertEquals("foo", logDefinition.getParameters().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", logDefinition.getParameters().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void testSourceDestinationWithTwoModules() throws Exception {
		assertEquals(0, repository.count());
		String definition = ":foo > filter | log";
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
		assertEquals(4, filterDefinition.getParameters().size());
		assertEquals("foo", filterDefinition.getParameters().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getParameters().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("myStream.filter", filterDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		ModuleDefinition logDefinition = myStream.getModuleDefinitions().get(1);
		assertEquals(2, logDefinition.getParameters().size());
		assertEquals("myStream.filter", logDefinition.getParameters().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", logDefinition.getParameters().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void testSinkDestinationWithSingleModule() throws Exception {
		assertEquals(0, repository.count());
		String definition = "time > :foo";
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
		assertEquals("foo", timeDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_DESTINATION));
	}

	@Test
	public void testSinkDestinationWithTwoModules() throws Exception {
		assertEquals(0, repository.count());
		String definition = "time | filter > :foo";
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
		assertEquals(2, timeDefinition.getParameters().size());
		assertEquals("myStream.time", timeDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", timeDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		ModuleDefinition filterDefinition = myStream.getModuleDefinitions().get(1);
		assertEquals(3, filterDefinition.getParameters().size());
		assertEquals("myStream.time", filterDefinition.getParameters().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getParameters().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("foo", filterDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_DESTINATION));
	}

	@Test
	public void testDestinationsOnBothSides() throws Exception {
		assertEquals(0, repository.count());
		String definition = ":bar > filter > :foo";
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", definition)
						.param("deploy", "true")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
						.andExpect(status().isCreated());
		assertEquals(1, repository.count());
		StreamDefinition myStream = repository.findOne("myStream");
		assertEquals(definition, myStream.getDslText());
		assertEquals("myStream", myStream.getName());
		assertEquals(1, myStream.getModuleDefinitions().size());
		ModuleDefinition filterDefinition = myStream.getModuleDefinitions().get(0);
		assertEquals(3, filterDefinition.getParameters().size());
		assertEquals("bar", filterDefinition.getParameters().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getParameters().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("foo", filterDefinition.getParameters().get(BindingPropertyKeys.OUTPUT_DESTINATION));

		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer).deploy(captor.capture());
		AppDeploymentRequest request = captor.getValue();
		assertThat(request.getDefinition().getName(), is("filter"));
		assertThat(request.getResource(), instanceOf(MavenResource.class));
		assertThat(((MavenResource) request.getResource()).getArtifactId(), is("filter-processor"));
	}

	@Test
	public void testDestroyStream() throws Exception {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream", "time | log");
		repository.save(streamDefinition1);
		for (ModuleDefinition moduleDefinition : streamDefinition1.getModuleDefinitions()) {
			deploymentIdRepository.save(DeploymentKey.forApp(moduleDefinition),
					streamDefinition1.getName() + "." + moduleDefinition.getName());
		}
		assertEquals(1, repository.count());
		AppStatus status = mock(AppStatus.class);
		when(status.getState()).thenReturn(DeploymentState.unknown);
		when(appDeployer.status("myStream.time")).thenReturn(status);
		when(appDeployer.status("myStream.log")).thenReturn(status);
		mockMvc.perform(
				delete("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		assertEquals(0, repository.count());
	}

	@Test
	public void testDestroySingleStream() throws Exception {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream", "time | log");
		StreamDefinition streamDefinition2 = new StreamDefinition("myStream1", "time | log");
		repository.save(streamDefinition1);
		repository.save(streamDefinition2);
		for (ModuleDefinition moduleDefinition : streamDefinition1.getModuleDefinitions()) {
			deploymentIdRepository.save(DeploymentKey.forApp(moduleDefinition),
					streamDefinition1.getName() + "." + moduleDefinition.getName());
		}
		assertEquals(2, repository.count());
		AppStatus status = mock(AppStatus.class);
		when(status.getState()).thenReturn(DeploymentState.unknown);
		when(appDeployer.status("myStream.time")).thenReturn(status);
		when(appDeployer.status("myStream.log")).thenReturn(status);
		mockMvc.perform(
				delete("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		assertEquals(1, repository.count());
	}

	@Test
	public void testDisplaySingleStream() throws Exception {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream", "time | log");
		for (ModuleDefinition moduleDefinition : streamDefinition1.getModuleDefinitions()) {
			deploymentIdRepository.save(DeploymentKey.forApp(moduleDefinition),
					streamDefinition1.getName() + "." + moduleDefinition.getName());
		}
		repository.save(streamDefinition1);
		assertEquals(1, repository.count());
		AppStatus status = mock(AppStatus.class);
		when(status.getState()).thenReturn(DeploymentState.unknown);
		when(appDeployer.status("myStream.time")).thenReturn(status);
		when(appDeployer.status("myStream.log")).thenReturn(status);
		mockMvc.perform(
				get("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().json("{name: \"myStream\"}"))
				.andExpect(content().json("{dslText: \"time | log\"}"));
	}

	@Test
	public void testDestroyStreamNotFound() throws Exception {
		mockMvc.perform(
				delete("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isNotFound());
		assertEquals(0, repository.count());
	}

	@Test
	public void testDeploy() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("log"));
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
	}

	@Test
	public void testDeployWithProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").param("properties",
						"module.time.producer.partitionKeyExpression=payload," +
								"module.log.count=2," +
								"module.log.consumer.concurrency=3")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("log"));
		Map<String, String> logDeploymentProps = logRequest.getEnvironmentProperties();
		assertEquals(logDeploymentProps.get("spring.cloud.stream.instanceCount"), "2");
		assertEquals(logDeploymentProps.get("spring.cloud.stream.bindings.input.consumer.partitioned"), "true");
		assertEquals(logDeploymentProps.get("spring.cloud.stream.bindings.input.consumer.concurrency"), "3");
		assertEquals(logDeploymentProps.get("count"), "2");
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
		Map<String, String> timeDeploymentProps = timeRequest.getEnvironmentProperties();
		assertEquals(timeDeploymentProps.get("spring.cloud.stream.bindings.output.producer.partitionCount"), "2");
		assertEquals(timeDeploymentProps.get("spring.cloud.stream.bindings.output.producer.partitionKeyExpression"), "payload");
	}

	@Test
	public void testDeployWithWildcardProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").param("properties",
						"module.*.producer.partitionKeyExpression=payload," +
								"module.*.count=2," +
								"module.*.consumer.concurrency=3")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("log"));
		Map<String, String> logDeploymentProps = logRequest.getEnvironmentProperties();
		assertEquals(logDeploymentProps.get("spring.cloud.stream.instanceCount"), "2");
		assertEquals(logDeploymentProps.get("spring.cloud.stream.bindings.input.consumer.partitioned"), "true");
		assertEquals(logDeploymentProps.get("spring.cloud.stream.bindings.input.consumer.concurrency"), "3");
		assertEquals(logDeploymentProps.get("count"), "2");
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
		Map<String, String> timeDeploymentProps = timeRequest.getEnvironmentProperties();
		assertEquals(timeDeploymentProps.get("spring.cloud.stream.bindings.output.producer.partitionCount"), "2");
		assertEquals(timeDeploymentProps.get("spring.cloud.stream.bindings.output.producer.partitionKeyExpression"), "payload");
	}

	@Test
	public void testAggregateState() {
		assertThat(StreamDefinitionController.aggregateState(EnumSet.of(deployed, failed)), is(failed));
		assertThat(StreamDefinitionController.aggregateState(EnumSet.of(unknown, failed)), is(failed));
		assertThat(StreamDefinitionController.aggregateState(EnumSet.of(deployed, failed, error)), is(error));
		assertThat(StreamDefinitionController.aggregateState(EnumSet.of(deployed, undeployed)), is(partial));
		assertThat(StreamDefinitionController.aggregateState(EnumSet.of(deployed, unknown)), is(partial));
		assertThat(StreamDefinitionController.aggregateState(EnumSet.of(undeployed, unknown)), is(partial));
		assertThat(StreamDefinitionController.aggregateState(EnumSet.of(unknown)), is(undeployed));
	}

}
