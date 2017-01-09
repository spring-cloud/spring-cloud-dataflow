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

package org.springframework.cloud.dataflow.server.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamPropertyKeys;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
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
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class StreamControllerTests {

	@Autowired
	private StreamDefinitionRepository repository;

	@Autowired
	private DeploymentIdRepository deploymentIdRepository;

	@Autowired
	private ApplicationConfigurationMetadataResolver metadataResolver;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppDeployer appDeployer;

	@Autowired
	private CommonApplicationProperties appsProperties;

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
		StreamDeploymentController deploymentController = new StreamDeploymentController(
				new InMemoryStreamDefinitionRepository(), new InMemoryDeploymentIdRepository(), appRegistry,
				appDeployer, metadataResolver, new CommonApplicationProperties());
		new StreamDefinitionController(null, null, deploymentController, appDeployer, appRegistry);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingDeploymentController() {
		new StreamDefinitionController(new InMemoryStreamDefinitionRepository(), new InMemoryDeploymentIdRepository(), null, appDeployer, appRegistry);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorMissingDeployer() {
		StreamDeploymentController deploymentController = new StreamDeploymentController(
				new InMemoryStreamDefinitionRepository(), new InMemoryDeploymentIdRepository(), appRegistry,
				appDeployer, metadataResolver, new CommonApplicationProperties());
		new StreamDefinitionController(new InMemoryStreamDefinitionRepository(), new InMemoryDeploymentIdRepository(),
				deploymentController, null, appRegistry);
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
		assertEquals(2, myStream.getAppDefinitions().size());
		StreamAppDefinition timeDefinition = myStream.getAppDefinitions().get(0);
		StreamAppDefinition logDefinition = myStream.getAppDefinitions().get(1);
		assertEquals(2, timeDefinition.getProperties().size());
		assertEquals("myStream.time", timeDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", timeDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertEquals(2, logDefinition.getProperties().size());
		assertEquals("myStream.time", logDefinition.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", logDefinition.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void testFindRelatedStreams() throws Exception {
		assertEquals(0, repository.count());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream1").param("definition", "time | log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myAnotherStream1").param("definition", "time | log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream2").param("definition", ":myStream1 > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream3").param("definition", ":myStream1.time > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream4").param("definition", ":myAnotherStream1 > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		assertEquals(5, repository.count());
		String response = mockMvc.perform(
				get("/streams/definitions/myStream1/related")
						.accept(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString();
		assertTrue(response.contains(":myStream1 > log"));
		assertTrue(response.contains(":myStream1.time > log"));
		assertTrue(response.contains("time | log"));
		assertTrue(response.contains("\"totalElements\":3"));
	}

	@Test
	public void testMethodArgumentTypeMismatchFailure() throws Exception {
		mockMvc.perform(get("/streams/definitions/myStream1/related")
				.param("nested", "in-correct-value")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());
	}

	@Test
	public void testFindRelatedAndNestedStreams() throws Exception {
		assertEquals(0, repository.count());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream1").param("definition", "time | log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myAnotherStream1").param("definition", "time | log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream2").param("definition", ":myStream1 > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "TapOnmyStream2").param("definition", ":myStream2 > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream3").param("definition", ":myStream1.time > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "TapOnMyStream3").param("definition", ":myStream3 > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "MultipleNestedTaps").param("definition", ":TapOnMyStream3 > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream4").param("definition", ":myAnotherStream1 > log")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		assertEquals(8, repository.count());
		String response = mockMvc.perform(
				get("/streams/definitions/myStream1/related?nested=true")
						.accept(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString();
		assertTrue(response.contains(":myStream1 > log"));
		assertTrue(response.contains(":myStream1.time > log"));
		assertTrue(response.contains("time | log"));
		assertTrue(response.contains("\"totalElements\":6"));
		String response2 = mockMvc.perform(
				get("/streams/definitions/myAnotherStream1/related?nested=true")
						.accept(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString();
		assertTrue(response2.contains(":myAnotherStream1 > log"));
		assertTrue(response2.contains("time | log"));
		assertTrue(response2.contains("\"totalElements\":2"));
		String response3 = mockMvc.perform(
				get("/streams/definitions/myStream2/related?nested=true")
						.accept(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString();
		assertTrue(response3.contains(":myStream1 > log"));
		assertTrue(response3.contains(":myStream2 > log"));
		assertTrue(response3.contains("\"totalElements\":2"));
	}

	@Test
	public void testSaveInvalidAppDefintions() throws Exception {
		String response = mockMvc.perform(
				post("/streams/definitions/").param("name", "myStream").param("definition", "foo | bar")
							.accept(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString();
		assertTrue(response.contains("IllegalArgumentException"));
		assertTrue(response.contains("Application name 'foo' with type 'source' does not exist in the app registry."));
		assertTrue(response.contains("Application name 'bar' with type 'sink' does not exist in the app registry."));
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
		StreamAppDefinition timeDefinition = myStream.getAppDefinitions().get(0);
		StreamAppDefinition logDefinition = myStream.getAppDefinitions().get(1);
		assertEquals("time", timeDefinition.getName());
		assertEquals("log", logDefinition.getName());
		assertEquals("500", timeDefinition.getProperties().get("fixedDelay"));
		assertEquals("milliseconds", timeDefinition.getProperties().get("timeUnit"));
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
		assertEquals(3, myStream.getAppDefinitions().size());
		StreamAppDefinition timeDefinition = myStream.getAppDefinitions().get(0);
		StreamAppDefinition filterDefinition = myStream.getAppDefinitions().get(1);
		StreamAppDefinition logDefinition = myStream.getAppDefinitions().get(2);
		assertEquals(2, timeDefinition.getProperties().size());
		assertEquals("myStream.time", timeDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", timeDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertEquals(4, filterDefinition.getProperties().size());
		assertEquals("myStream.time", filterDefinition.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("myStream.filter", filterDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		assertEquals(2, logDefinition.getProperties().size());
		assertEquals("myStream.filter", logDefinition.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", logDefinition.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void testSourceDestinationWithSingleApp() throws Exception {
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
		assertEquals(1, myStream.getAppDefinitions().size());
		StreamAppDefinition logDefinition = myStream.getAppDefinitions().get(0);
		assertEquals(2, logDefinition.getProperties().size());
		assertEquals("foo", logDefinition.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", logDefinition.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void testSourceDestinationWithTwoApps() throws Exception {
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
		assertEquals(2, myStream.getAppDefinitions().size());
		StreamAppDefinition filterDefinition = myStream.getAppDefinitions().get(0);
		assertEquals(4, filterDefinition.getProperties().size());
		assertEquals("foo", filterDefinition.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("myStream.filter", filterDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		StreamAppDefinition logDefinition = myStream.getAppDefinitions().get(1);
		assertEquals(2, logDefinition.getProperties().size());
		assertEquals("myStream.filter", logDefinition.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", logDefinition.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
	}

	@Test
	public void testSinkDestinationWithSingleApp() throws Exception {
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
		assertEquals(1, myStream.getAppDefinitions().size());
		StreamAppDefinition timeDefinition = myStream.getAppDefinitions().get(0);
		assertEquals(1, timeDefinition.getProperties().size());
		assertEquals("foo", timeDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
	}

	@Test
	public void testSinkDestinationWithTwoApps() throws Exception {
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
		assertEquals(2, myStream.getAppDefinitions().size());
		StreamAppDefinition timeDefinition = myStream.getAppDefinitions().get(0);
		assertEquals(2, timeDefinition.getProperties().size());
		assertEquals("myStream.time", timeDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
		assertEquals("myStream", timeDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS));
		StreamAppDefinition filterDefinition = myStream.getAppDefinitions().get(1);
		assertEquals(3, filterDefinition.getProperties().size());
		assertEquals("myStream.time", filterDefinition.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("foo", filterDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));
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
		assertEquals(1, myStream.getAppDefinitions().size());
		StreamAppDefinition filterDefinition = myStream.getAppDefinitions().get(0);
		assertEquals(3, filterDefinition.getProperties().size());
		assertEquals("bar", filterDefinition.getProperties().get(BindingPropertyKeys.INPUT_DESTINATION));
		assertEquals("myStream", filterDefinition.getProperties().get(BindingPropertyKeys.INPUT_GROUP));
		assertEquals("foo", filterDefinition.getProperties().get(BindingPropertyKeys.OUTPUT_DESTINATION));

		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer).deploy(captor.capture());
		AppDeploymentRequest request = captor.getValue();
		assertThat(request.getDefinition().getName(), is("filter"));
		assertThat(request.getResource(), instanceOf(MavenResource.class));
		assertThat(((MavenResource) request.getResource()).getArtifactId(), is("filter-processor-rabbit"));
	}

	@Test
	public void testDestroyStream() throws Exception {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream", "time | log");
		repository.save(streamDefinition1);
		for (StreamAppDefinition appDefinition : streamDefinition1.getAppDefinitions()) {
			deploymentIdRepository.save(DeploymentKey.forStreamAppDefinition(appDefinition),
					streamDefinition1.getName() + "." + appDefinition.getName());
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
		for (StreamAppDefinition appDefinition : streamDefinition1.getAppDefinitions()) {
			deploymentIdRepository.save(DeploymentKey.forStreamAppDefinition(appDefinition),
					streamDefinition1.getName() + "." + appDefinition.getName());
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
		for (StreamAppDefinition appDefinition : streamDefinition1.getAppDefinitions()) {
			deploymentIdRepository.save(DeploymentKey.forStreamAppDefinition(appDefinition),
					streamDefinition1.getName() + "." + appDefinition.getName());
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
		assertEquals("true", logRequest.getDeploymentProperties().get(AppDeployer.INDEXED_PROPERTY_KEY));
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
	}

	@Test
	public void testStreamWithShortformProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time --fixed-delay=2 | log --level=WARN"));
		mockMvc.perform(
				post("/streams/deployments/myStream")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("log"));
		Map<String, String> logAppProps = logRequest.getDefinition().getProperties();
		assertEquals("WARN", logAppProps.get("log.level"));
		assertEquals("true", logRequest.getDeploymentProperties().get(AppDeployer.INDEXED_PROPERTY_KEY));
		assertNull(logAppProps.get("level"));
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
		Map<String, String> timeAppProps = timeRequest.getDefinition().getProperties();
		assertEquals("2", timeAppProps.get("trigger.fixed-delay"));
		assertNull(timeAppProps.get("fixed-delay"));
	}

	@Test
	public void testDeployWithAppPropertiesOverride() throws Exception {
		repository.save(new StreamDefinition("myStream", "time --fixed-delay=2 | log --level=WARN"));
		mockMvc.perform(
				post("/streams/deployments/myStream").param("properties",
						"app.time.fixed-delay=4," +
								"app.log.level=ERROR")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("log"));
		Map<String, String> logAppProps = logRequest.getDefinition().getProperties();
		assertEquals("true", logRequest.getDeploymentProperties().get(AppDeployer.INDEXED_PROPERTY_KEY));
		assertEquals("ERROR", logAppProps.get("log.level"));
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
		Map<String, String> timeAppProps = timeRequest.getDefinition().getProperties();
		assertEquals("4", timeAppProps.get("trigger.fixed-delay"));
	}

	@Test
	public void testDeployWithAppPropertiesOverrideWithLabel() throws Exception {
		repository.save(new StreamDefinition("myStream", "a: time --fixed-delay=2 | b: log --level=WARN"));
		mockMvc.perform(
				post("/streams/deployments/myStream").param("properties",
						"app.a.fixed-delay=4," +
								"app.b.level=ERROR")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("b"));
		assertEquals("true", logRequest.getDeploymentProperties().get(AppDeployer.INDEXED_PROPERTY_KEY));
		Map<String, String> logAppProps = logRequest.getDefinition().getProperties();
		assertEquals("ERROR", logAppProps.get("log.level"));
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("a"));
		Map<String, String> timeAppProps = timeRequest.getDefinition().getProperties();
		assertEquals("4", timeAppProps.get("trigger.fixed-delay"));
	}

	@Test
	public void testDuplicateDeploy() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		when(appDeployer.status("testID")).thenReturn(AppStatus.of("testID").with(new AppInstanceStatus() {
			@Override
			public String getId() {
				return "testID";
			}

			@Override
			public DeploymentState getState() {
				return DeploymentState.valueOf("deployed");
			}

			@Override
			public Map<String, String> getAttributes() {
				return null;
			}
		}).build());
		mockMvc.perform(
				post("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isConflict());
	}

	@Test
	public void testDuplicateDeployWhenStreamIsBeingDeployed() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		when(appDeployer.status("testID")).thenReturn(AppStatus.of("testID").with(new AppInstanceStatus() {
			@Override
			public String getId() {
				return "testID";
			}

			@Override
			public DeploymentState getState() {
				return DeploymentState.valueOf("deploying");
			}

			@Override
			public Map<String, String> getAttributes() {
				return null;
			}
		}).build());
		mockMvc.perform(
				post("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isConflict());
	}

	@Test
	public void testUndeployNonDeployedStream() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				delete("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(appDeployer, times(0)).undeploy(captor.capture());
	}

	@Test
	public void testUndeployAllNonDeployedStream() throws Exception {
		repository.save(new StreamDefinition("myStream1", "time | log"));
		repository.save(new StreamDefinition("myStream2", "time | log"));
		mockMvc.perform(
				delete("/streams/deployments").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(appDeployer, times(0)).undeploy(captor.capture());
	}

	@Test
	public void testDeployWithProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").param("properties",
						"app.time.producer.partitionKeyExpression=payload," +
								"app.log.count=2," +
								"app.log.consumer.concurrency=3")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("log"));
		Map<String, String> logAppProps = logRequest.getDefinition().getProperties();
		assertEquals("true", logAppProps.get("spring.cloud.stream.bindings.input.consumer.partitioned"));
		assertEquals("3", logAppProps.get("spring.cloud.stream.bindings.input.consumer.concurrency"));
		assertEquals("2", logAppProps.get(StreamPropertyKeys.INSTANCE_COUNT));
		Map<String, String> logDeploymentProps = logRequest.getDeploymentProperties();
		assertEquals(logDeploymentProps.get(AppDeployer.INDEXED_PROPERTY_KEY), "true");
		assertEquals("2", logDeploymentProps.get(AppDeployer.COUNT_PROPERTY_KEY));
		assertEquals("myStream", logDeploymentProps.get(AppDeployer.GROUP_PROPERTY_KEY));
		assertEquals("true", logDeploymentProps.get(AppDeployer.INDEXED_PROPERTY_KEY));
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
		Map<String, String> timeAppProps = timeRequest.getDefinition().getProperties();
		assertEquals("2", timeAppProps.get("spring.cloud.stream.bindings.output.producer.partitionCount"));
		assertEquals("payload", timeAppProps.get("spring.cloud.stream.bindings.output.producer.partitionKeyExpression"));
		Map<String, String> timeDeploymentProps = timeRequest.getDeploymentProperties();
		assertNull(timeDeploymentProps.get(AppDeployer.COUNT_PROPERTY_KEY));
		assertEquals("myStream", timeDeploymentProps.get(AppDeployer.GROUP_PROPERTY_KEY));
		assertNull(timeDeploymentProps.get(AppDeployer.INDEXED_PROPERTY_KEY));
	}

	@Test
	public void testDeployWithWildcardProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").param("properties",
						"app.*.producer.partitionKeyExpression=payload," +
								"app.*.count=2," +
								"app.*.consumer.concurrency=3")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("log"));
		Map<String, String> logAppProps = logRequest.getDefinition().getProperties();
		assertEquals("2", logAppProps.get(StreamPropertyKeys.INSTANCE_COUNT));
		assertEquals("true", logAppProps.get("spring.cloud.stream.bindings.input.consumer.partitioned"));
		assertEquals("3", logAppProps.get("spring.cloud.stream.bindings.input.consumer.concurrency"));
		Map<String, String> logDeploymentProps = logRequest.getDeploymentProperties();
		assertEquals("2", logDeploymentProps.get(AppDeployer.COUNT_PROPERTY_KEY));
		assertEquals("myStream", logDeploymentProps.get(AppDeployer.GROUP_PROPERTY_KEY));
		assertEquals("true", logDeploymentProps.get(AppDeployer.INDEXED_PROPERTY_KEY));
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
		Map<String, String> timeAppProps = timeRequest.getDefinition().getProperties();
		assertEquals("2", timeAppProps.get("spring.cloud.stream.bindings.output.producer.partitionCount"));
		assertEquals("payload", timeAppProps.get("spring.cloud.stream.bindings.output.producer.partitionKeyExpression"));
		Map<String, String> timeDeploymentProps = timeRequest.getDeploymentProperties();
		assertEquals("2", timeDeploymentProps.get(AppDeployer.COUNT_PROPERTY_KEY));
		assertEquals("myStream", timeDeploymentProps.get(AppDeployer.GROUP_PROPERTY_KEY));
		assertNull(timeDeploymentProps.get(AppDeployer.INDEXED_PROPERTY_KEY));
	}

	@Test
	public void testDeployWithCommonApplicationProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		assertThat(appsProperties.getStream().values(), empty());
		appsProperties.getStream().put("spring.cloud.stream.fake.binder.host","fakeHost");
		appsProperties.getStream().put("spring.cloud.stream.fake.binder.port","fakePort");
		mockMvc.perform(
				post("/streams/deployments/myStream").param("properties",
						"app.*.producer.partitionKeyExpression=payload," +
								"app.*.count=2," +
								"app.*.consumer.concurrency=3")
						.accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
		List<AppDeploymentRequest> requests = captor.getAllValues();
		assertEquals(2, requests.size());
		AppDeploymentRequest logRequest = requests.get(0);
		assertThat(logRequest.getDefinition().getName(), is("log"));
		Map<String, String> logAppProps = logRequest.getDefinition().getProperties();
		assertEquals("2", logAppProps.get(StreamPropertyKeys.INSTANCE_COUNT));
		assertEquals("fakeHost", logAppProps.get("spring.cloud.stream.fake.binder.host"));
		assertEquals("fakePort", logAppProps.get("spring.cloud.stream.fake.binder.port"));
		assertEquals("true", logAppProps.get("spring.cloud.stream.bindings.input.consumer.partitioned"));
		assertEquals("3", logAppProps.get("spring.cloud.stream.bindings.input.consumer.concurrency"));
		Map<String, String> logDeploymentProps = logRequest.getDeploymentProperties();
		assertEquals("2", logDeploymentProps.get(AppDeployer.COUNT_PROPERTY_KEY));
		assertEquals("myStream", logDeploymentProps.get(AppDeployer.GROUP_PROPERTY_KEY));
		assertEquals("true", logDeploymentProps.get(AppDeployer.INDEXED_PROPERTY_KEY));
		AppDeploymentRequest timeRequest = requests.get(1);
		assertThat(timeRequest.getDefinition().getName(), is("time"));
		Map<String, String> timeAppProps = timeRequest.getDefinition().getProperties();
		assertEquals("2", timeAppProps.get("spring.cloud.stream.bindings.output.producer.partitionCount"));
		assertEquals("payload", timeAppProps.get("spring.cloud.stream.bindings.output.producer.partitionKeyExpression"));
		Map<String, String> timeDeploymentProps = timeRequest.getDeploymentProperties();
		assertEquals("2", timeDeploymentProps.get(AppDeployer.COUNT_PROPERTY_KEY));
		assertEquals("myStream", timeDeploymentProps.get(AppDeployer.GROUP_PROPERTY_KEY));
		assertNull(timeDeploymentProps.get(AppDeployer.INDEXED_PROPERTY_KEY));
		appsProperties.getStream().clear();
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

	@Test
	public void testAppDeploymentFailure() throws Exception {
		when(appDeployer.deploy(any(AppDeploymentRequest.class))).thenThrow(new RuntimeException());
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(
				post("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated());
		ArgumentCaptor<AppDeploymentRequest> captor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(appDeployer, times(2)).deploy(captor.capture());
	}

}
