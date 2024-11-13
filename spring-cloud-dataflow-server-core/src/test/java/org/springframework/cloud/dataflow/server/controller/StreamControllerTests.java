/*
 * Copyright 2015-2022 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.cloud.dataflow.core.BindingPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamPropertyKeys;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.stream.StreamDeployerUtil;
import org.springframework.cloud.dataflow.server.support.SkipperPackageUtils;
import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationSpec;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Andy Clement
 * @author Christian Tzolov
 * @author Daniel Serleg
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
class StreamControllerTests {
	private final static Logger logger = LoggerFactory.getLogger(StreamControllerTests.class);

	@Autowired
	private StreamDefinitionRepository repository;

	@Autowired
	private AuditRecordRepository auditRecordRepository;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private CommonApplicationProperties appsProperties;

	@Autowired
	private SkipperClient skipperClient;

	private Info streamStatusInfo;

	@Autowired
	private StreamDefinitionService streamDefinitionService;

	@BeforeEach
	void setupMocks() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
			.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();

		streamStatusInfo = new Info();
		streamStatusInfo.setStatus(new Status());
		streamStatusInfo.getStatus().setStatusCode(StatusCode.UNKNOWN);
		when(skipperClient.status(anyString())).thenReturn(streamStatusInfo);

		Deployer deployerLocal = new Deployer("default", "local", mock(AppDeployer.class), mock(ActuatorOperations.class));
		Deployer deployerK8s = new Deployer("k8s", "kubernetes", mock(AppDeployer.class), mock(ActuatorOperations.class));
		Deployer deployerCf = new Deployer("pcf", "cloudfoundry", mock(AppDeployer.class), mock(ActuatorOperations.class));
		when(skipperClient.listDeployers()).thenReturn(Arrays.asList(deployerLocal, deployerK8s, deployerCf));

		when(skipperClient.search(anyString(), eq(false))).thenReturn(new ArrayList<PackageMetadata>());
	}

	@AfterEach
	void tearDown() {
		repository.deleteAll();
		auditRecordRepository.deleteAll();
		assertThat(repository.count()).isZero();
		assertThat(auditRecordRepository.count()).isZero();
	}

	@Test
	void constructorMissingStreamService() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> {
				new StreamDefinitionController(null, null, null, null, null);
			});
	}

	@Test
	void saveNoDeployJsonEncoded() throws Exception {
		assertThat(repository.count()).isZero();
		mockMvc.perform(post("/streams/definitions")
				.param("name", "myStream")
				.param("definition", "time | log")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated());
		assertThatStreamSavedWithoutDeploy();
	}

	@Test
	void saveNoDeployFormEncoded() throws Exception {
		assertThat(repository.count()).isZero();
		MultiValueMap<String, String> values = new LinkedMultiValueMap<>();
		values.add("name", "myStream");
		values.add("definition", "time | log");
		mockMvc.perform(post("/streams/definitions")
				.params(values)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isCreated());
		assertThatStreamSavedWithoutDeploy();
	}

	private void assertThatStreamSavedWithoutDeploy() {
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream").get();
		assertThat(myStream.getDslText()).isEqualTo("time | log");
		assertThat(myStream.getName()).isEqualTo("myStream");
		assertThat(this.streamDefinitionService.getAppDefinitions(myStream)).hasSize(2);
		StreamAppDefinition timeDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(0);
		StreamAppDefinition logDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(1);
		assertThat(timeDefinition.getProperties()).hasSize(2);
		assertThat(timeDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "myStream.time");
		assertThat(timeDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "myStream");
		assertThat(logDefinition.getProperties()).hasSize(2);
		assertThat(logDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "myStream.time");
		assertThat(logDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "myStream");
	}

	@ParameterizedTest
	@ArgumentsSource(DeploymentPropsProvider.class)
	void saveAndDeploy(Map<String, String> deploymentProps, Map<String, String> expectedPropsOnApps) throws Exception {
		assertThat(repository.count()).isZero();
		String definition = "time | log";
		String streamName = "testSaveAndDeploy-stream";
		MockHttpServletRequestBuilder requestBuilder = post("/streams/definitions")
			.param("name", streamName)
			.param("definition", definition)
			.param("deploy", "true")
			.accept(MediaType.APPLICATION_JSON);
		if (deploymentProps != null) {
			requestBuilder
				.content(new ObjectMapper().writeValueAsBytes(deploymentProps))
				.contentType(MediaType.APPLICATION_JSON);
		}
		mockMvc.perform(requestBuilder)
			.andExpect(status().isCreated());

		assertThat(repository.count()).isEqualTo(1);
		assertThat(repository.findById(streamName)).isPresent()
			.hasValueSatisfying((sd) -> {
				assertThat(sd.getName()).isEqualTo(streamName);
				assertThat(sd.getDslText()).isEqualTo(definition);
			})
			.map(this.streamDefinitionService::getAppDefinitions).get().asInstanceOf(InstanceOfAssertFactories.LIST)
			.extracting("name").containsExactly("time", "log");

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());

		List<UploadRequest> uploadRequests = uploadRequestCaptor.getAllValues();
		assertThat(uploadRequests).hasSize(1);
		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		Package timePackage = findChildPackageByName(pkg, "time");
		assertThat(timePackage).isNotNull();
		SpringCloudDeployerApplicationSpec timeSpec = parseSpec(timePackage.getConfigValues().getRaw());
		assertThat(timeSpec.getDeploymentProperties()).containsAllEntriesOf(expectedPropsOnApps);

		Package logPackage = findChildPackageByName(pkg, "log");
		assertThat(logPackage).isNotNull();
		SpringCloudDeployerApplicationSpec logAppSpec = parseSpec(logPackage.getConfigValues().getRaw());
		assertThat(logAppSpec.getDeploymentProperties()).containsAllEntriesOf(expectedPropsOnApps);
	}
	static class DeploymentPropsProvider implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
			return Stream.of(
					arguments(Collections.singletonMap("deployer.*.count", "2"),
							Collections.singletonMap("spring.cloud.deployer.count", "2")),
					arguments(Collections.emptyMap(), Collections.emptyMap()), arguments(null, Collections.emptyMap()));
		}

	}

	@Test
	void saveWithSensitiveProperties() throws Exception {
		assertThat(repository.count()).isZero();
		mockMvc.perform(post("/streams/definitions").param("name", "myStream2")
			.param("definition", "time --some.password=foobar --another-secret=kenny | log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream2").get();
		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();

		assertThat(auditRecords).hasSize(5);

		final AuditRecord auditRecord = auditRecords.get(4);

		assertThat(myStream.getDslText()).isEqualTo("time --some.password=foobar --another-secret=kenny | log");
		assertThat(auditRecord.getAuditData()).isEqualTo("time --some.password='******' --another-secret='******' | log");
		assertThat(auditRecord.getCorrelationId()).isEqualTo("myStream2");
		assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.STREAM);
		assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.CREATE);
	}

	@Test
	void findRelatedStreams() throws Exception {
		assertThat(repository.count()).isZero();
		mockMvc.perform(post("/streams/definitions").param("name", "myStream1")
			.param("definition", "time | log")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myAnotherStream1")
				.param("definition", "time | log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream2")
			.param("definition", ":myStream1 > log")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream3")
				.param("definition", ":myStream1.time > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream4")
				.param("definition", ":myAnotherStream1 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(5);

		mockMvc.perform(get("/streams/definitions/myStream1/related").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(3)))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].name", is("myStream1")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].dslText", is("time | log")))

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].name", is("myStream2")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].dslText", is(":myStream1 > log")))

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[2].name", is("myStream3")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[2].dslText", is(":myStream1.time > log")));
	}

	@Test
	void streamSearchNameContainsSubstring() throws Exception {
		mockMvc.perform(post("/streams/definitions").param("name", "foo")
			.param("definition", "time | log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(post("/streams/definitions").param("name", "foaz")
			.param("definition", "time | log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(post("/streams/definitions").param("name", "ooz")
			.param("definition", "time | log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(3);

		mockMvc.perform(get("/streams/definitions").param("search", "f")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(2)))

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].name", is("foo")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].name", is("foaz")));

		mockMvc.perform(get("/streams/definitions").param("search", "o")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(3)))

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].name", is("foo")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].name", is("foaz")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[2].name", is("ooz")));

		mockMvc.perform(get("/streams/definitions").param("search", "z")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(2)))

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].name", is("foaz")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].name", is("ooz")));
	}

	@Test
	void findRelatedStreamsGh2150() throws Exception {
		assertThat(repository.count()).isZero();
		// Bad definition, recursive reference
		mockMvc.perform(post("/streams/definitions").param("name", "mapper")
			.param("definition", ":mapper.time > log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);

		mockMvc.perform(get("/streams/definitions/mapper/related")
				.param("nested", "true")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(1)))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].dslText", is(":mapper.time > log")));
	}

	@Test
	void findRelatedStreams2Gh2150() throws Exception {
		// bad streams, recursively referencing via each other
		mockMvc.perform(post("/streams/definitions").param("name", "foo")
			.param("definition", ":bar.time > log")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "bar")
			.param("definition", ":foo.time > log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(2);

		mockMvc.perform(get("/streams/definitions/foo/related")
				.param("nested", "true")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(2)))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].dslText", is(":bar.time > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].dslText", is(":foo.time > log")));
	}

	@Test
	void methodArgumentTypeMismatchFailure() throws Exception {
		mockMvc.perform(get("/streams/definitions/myStream1/related").param("nested", "in-correct-value")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().is4xxClientError());
	}

	@Test
	void findRelatedAndNestedStreams() throws Exception {
		assertThat(repository.count()).isZero();
		mockMvc.perform(post("/streams/definitions").param("name", "myStream1")
			.param("definition", "time | log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(post("/streams/definitions").param("name", "myAnotherStream1")
				.param("definition", "time | log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream2")
			.param("definition", ":myStream1 > log")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "TapOnmyStream2")
				.param("definition", ":myStream2 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream3")
				.param("definition", ":myStream1.time > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "TapOnMyStream3")
				.param("definition", ":myStream3 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "MultipleNestedTaps")
				.param("definition", ":TapOnMyStream3 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream4")
				.param("definition", ":myAnotherStream1 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());

		mockMvc.perform(post("/streams/definitions").param("name", "myStream5")
			.param("definition", "time | log --secret=foo")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(post("/streams/definitions").param("name", "myStream6")
				.param("definition", ":myStream5.time > log --password=bar")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		assertThat(repository.count()).isEqualTo(10);
		mockMvc.perform(get("/streams/definitions/myStream1/related?nested=true").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(6)))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].dslText", is("time | log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].dslText", is(":myStream1 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[2].dslText", is(":myStream2 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[3].dslText", is(":myStream1.time > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[4].dslText", is(":myStream3 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[5].dslText", is(":TapOnMyStream3 > log")));

		mockMvc.perform(get("/streams/definitions/myStream5/related?nested=true").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(2)))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].dslText", is("time | log --secret='******'")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].dslText", is(":myStream5.time > log --password='******'")));

		mockMvc.perform(
				get("/streams/definitions/myAnotherStream1/related?nested=true").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(2)))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].dslText", is("time | log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].dslText", is(":myAnotherStream1 > log")));

		mockMvc.perform(get("/streams/definitions/myStream2/related?nested=true").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(2)))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].dslText", is(":myStream1 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].dslText", is(":myStream2 > log")));
	}

	@Test
	void findAll() throws Exception {
		assertThat(repository.count()).isZero();
		mockMvc.perform(post("/streams/definitions").param("name", "myStream1")
			.param("definition", "time --password=foo| log")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream1A")
			.param("definition", "time --foo=bar| log")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myAnotherStream1")
				.param("definition", "time | log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream2")
			.param("definition", ":myStream1 > log")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "TapOnmyStream2")
				.param("definition", ":myStream2 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream3")
				.param("definition", ":myStream1.time > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "TapOnMyStream3")
				.param("definition", ":myStream3 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "MultipleNestedTaps")
				.param("definition", ":TapOnMyStream3 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "myStream4")
				.param("definition", ":myAnotherStream1 > log").accept(MediaType.APPLICATION_JSON)).andDo(print())
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions")
				.param("name", "timelogSingleTick")
				.param("definition", "time --format='YYYY MM DD' | log")
				.param("deploy", "false"))
			.andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "timelogDoubleTick")
			.param("definition", "a: time --format=\"YYYY MM DD\" | log")
			.param("deploy", "false")).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "twoPassword")
			.param("definition", "time --password='foo'| log --password=bar")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "nameChannelPassword")
			.param("definition", "time --password='foo'> :foobar")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "twoParam")
			.param("definition", "time --password=foo --arg=foo | log")
			.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions").param("name", "twoPipeInQuotes")
			.param("definition", "time --password='fo|o' --arg=bar | log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		assertThat(repository.count()).isEqualTo(15);
		mockMvc.perform(get("/streams/definitions").accept(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())

			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList.*", hasSize(15)))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[0].dslText", is("time --password='******' | log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[1].dslText", is("time --foo=bar | log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[2].dslText", is("time | log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[3].dslText", is(":myStream1 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[4].dslText", is(":myStream2 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[5].dslText", is(":myStream1.time > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[6].dslText", is(":myStream3 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[7].dslText", is(":TapOnMyStream3 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[8].dslText", is(":myAnotherStream1 > log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[9].dslText", is("time --format='YYYY MM DD' | log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[10].dslText", is("a: time --format='YYYY MM DD' | log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[11].dslText", is("time --password='******' | log --password='******'")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[12].dslText", is("time --password='******' > :foobar")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[13].dslText", is("time --password='******' --arg=foo | log")))
			.andExpect(jsonPath("$._embedded.streamDefinitionResourceList[14].dslText", is("time --password='******' --arg=bar | log")));
	}

	@Test
	void saveInvalidAppDefinitions() throws Exception {
		mockMvc.perform(post("/streams/definitions")
				.param("name", "myStream")
				.param("definition", "foo | bar")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("_embedded.errors[0].logref", is("InvalidStreamDefinitionException")))
			.andExpect(jsonPath("_embedded.errors[0].message",
				is("""
					Application name 'foo' with type 'source' does not exist in the \
					app \
					registry.
					Application name 'bar' with type 'sink' does not exist in the app \
					registry.""")));
	}

	@Test
	void saveInvalidAppDefinitionsDueToParseException() throws Exception {
		mockMvc.perform(post("/streams/definitions").param("name", "myStream")
				.param("definition", "foo --.spring.cloud.stream.metrics.properties=spring* | bar")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("_embedded.errors[0].logref", is("InvalidStreamDefinitionException"))).andExpect(
				jsonPath("_embedded.errors[0].message", startsWith("111E:(pos 6): Unexpected token.  Expected '.' but was")));
	}

	@Test
	void saveDuplicate() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		assertThat(repository.count()).isEqualTo(1);
		mockMvc.perform(post("/streams/definitions")
			.param("name", "myStream")
			.param("definition", "time | log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void saveWithParameters() throws Exception {
		assertThat(repository.count()).isZero();
		String definition = "time --fixedDelay=500 --timeUnit=milliseconds | log";
		mockMvc.perform(post("/streams/definitions")
			.param("name", "myStream")
			.param("definition", definition)
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream").get();
		StreamAppDefinition timeDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(0);
		StreamAppDefinition logDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(1);
		assertThat(timeDefinition.getName()).isEqualTo("time");
		assertThat(logDefinition.getName()).isEqualTo("log");
		assertThat(timeDefinition.getProperties()).containsEntry("fixedDelay", "500");
		assertThat(timeDefinition.getProperties()).containsEntry("timeUnit", "milliseconds");
		assertThat(myStream.getDslText()).isEqualTo(definition);
		assertThat(myStream.getName()).isEqualTo("myStream");
	}

	@Test
	void streamWithProcessor() throws Exception {
		assertThat(repository.count()).isZero();
		String definition = "time | filter | log";
		mockMvc.perform(post("/streams/definitions")
			.param("name", "myStream")
			.param("definition", definition)
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream").get();
		assertThat(myStream.getDslText()).isEqualTo(definition);
		assertThat(myStream.getName()).isEqualTo("myStream");
		assertThat(this.streamDefinitionService.getAppDefinitions(myStream)).hasSize(3);
		StreamAppDefinition timeDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(0);
		StreamAppDefinition filterDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(1);
		StreamAppDefinition logDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(2);
		assertThat(timeDefinition.getProperties()).hasSize(2);
		assertThat(timeDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "myStream.time");
		assertThat(timeDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "myStream");
		assertThat(filterDefinition.getProperties()).hasSize(4);
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "myStream.time");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "myStream");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "myStream.filter");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "myStream");
		assertThat(logDefinition.getProperties()).hasSize(2);
		assertThat(logDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "myStream.filter");
		assertThat(logDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "myStream");
	}

	@Test
	void sourceDestinationWithSingleApp() throws Exception {
		assertThat(repository.count()).isZero();
		String definition = ":foo > log";
		mockMvc.perform(post("/streams/definitions")
			.param("name", "myStream")
			.param("definition", definition)
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream").get();
		assertThat(myStream.getDslText()).isEqualTo(definition);
		assertThat(myStream.getName()).isEqualTo("myStream");
		assertThat(this.streamDefinitionService.getAppDefinitions(myStream)).hasSize(1);
		StreamAppDefinition logDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(0);
		assertThat(logDefinition.getProperties()).hasSize(2);
		assertThat(logDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "foo");
		assertThat(logDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "myStream");
	}

	@Test
	void sourceDestinationWithTwoApps() throws Exception {
		assertThat(repository.count()).isZero();
		String definition = ":foo > filter | log";
		mockMvc.perform(post("/streams/definitions")
			.param("name", "myStream")
			.param("definition", definition)
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream").get();
		assertThat(myStream.getDslText()).isEqualTo(definition);
		assertThat(myStream.getName()).isEqualTo("myStream");
		assertThat(this.streamDefinitionService.getAppDefinitions(myStream)).hasSize(2);
		StreamAppDefinition filterDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(0);
		assertThat(filterDefinition.getProperties()).hasSize(4);
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "foo");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "myStream");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "myStream.filter");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "myStream");
		StreamAppDefinition logDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(1);
		assertThat(logDefinition.getProperties()).hasSize(2);
		assertThat(logDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "myStream.filter");
		assertThat(logDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "myStream");
	}

	@Test
	void sinkDestinationWithSingleApp() throws Exception {
		assertThat(repository.count()).isZero();
		String definition = "time > :foo";
		mockMvc.perform(post("/streams/definitions")
			.param("name", "myStream")
			.param("definition", definition)
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream").get();
		assertThat(myStream.getDslText()).isEqualTo(definition);
		assertThat(myStream.getName()).isEqualTo("myStream");
		assertThat(this.streamDefinitionService.getAppDefinitions(myStream)).hasSize(1);
		StreamAppDefinition timeDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(0);
		assertThat(timeDefinition.getProperties()).hasSize(1);
		assertThat(timeDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "foo");
	}

	@Test
	void sinkDestinationWithTwoApps() throws Exception {
		assertThat(repository.count()).isZero();
		String definition = "time | filter > :foo";
		mockMvc.perform(post("/streams/definitions")
			.param("name", "myStream")
			.param("definition", definition)
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream").get();
		assertThat(myStream.getDslText()).isEqualTo(definition);
		assertThat(myStream.getName()).isEqualTo("myStream");
		assertThat(this.streamDefinitionService.getAppDefinitions(myStream)).hasSize(2);
		StreamAppDefinition timeDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(0);
		assertThat(timeDefinition.getProperties()).hasSize(2);
		assertThat(timeDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "myStream.time");
		assertThat(timeDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, "myStream");
		StreamAppDefinition filterDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(1);
		assertThat(filterDefinition.getProperties()).hasSize(3);
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "myStream.time");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "myStream");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "foo");
	}

	@Test
	void destinationsOnBothSides() throws Exception {
		assertThat(repository.count()).isZero();
		String definition = ":bar > filter > :foo";

		mockMvc.perform(post("/streams/definitions")
				.param("name", "myStream")
				.param("definition", definition)
				.param("deploy", "true").accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertThat(repository.count()).isEqualTo(1);
		StreamDefinition myStream = repository.findById("myStream").get();
		assertThat(myStream.getDslText()).isEqualTo(definition);
		assertThat(myStream.getName()).isEqualTo("myStream");
		assertThat(this.streamDefinitionService.getAppDefinitions(myStream)).hasSize(1);
		StreamAppDefinition filterDefinition = this.streamDefinitionService.getAppDefinitions(myStream).get(0);
		assertThat(filterDefinition.getProperties()).hasSize(3);
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_DESTINATION, "bar");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.INPUT_GROUP, "myStream");
		assertThat(filterDefinition.getProperties()).containsEntry(BindingPropertyKeys.OUTPUT_DESTINATION, "foo");

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());
		ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);
		verify(skipperClient, times(1)).install(installRequestCaptor.capture());

		List<UploadRequest> uploadRequests = uploadRequestCaptor.getAllValues();
		assertThat(uploadRequests).hasSize(1);

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		Package filterPackage = findChildPackageByName(pkg, "filter");
		SpringCloudDeployerApplicationSpec filterSpec = parseSpec(filterPackage.getConfigValues().getRaw());

		assertThat(filterSpec.getResource())
			.isEqualTo("maven://org.springframework.cloud.stream.app:filter-processor-rabbit:jar");
	}

	@Test
	void destroyStream() throws Exception {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream", "time | log");
		repository.save(streamDefinition1);
		assertThat(repository.count()).isEqualTo(1);

		mockMvc.perform(delete("/streams/definitions/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		assertThat(repository.count()).isEqualTo(0);
	}

	@Test
	void destroyWithSensitiveProperties() throws Exception {
		assertThat(repository.count()).isZero();

		StreamDefinition streamDefinition1 = new StreamDefinition("myStream1234",
			"time --some.password=foobar --another-secret=kenny | log");
		repository.save(streamDefinition1);
		assertThat(repository.count()).isEqualTo(1);

		mockMvc.perform(delete("/streams/definitions/myStream1234").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		assertThat(repository.count()).isEqualTo(0);
		assertThat(streamDefinition1.getDslText()).isEqualTo("time --some.password=foobar --another-secret=kenny | log");

		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();

		assertThat(auditRecords).hasSize(6);
		final AuditRecord auditRecord1 = auditRecords.get(4);
		final AuditRecord auditRecord2 = auditRecords.get(5);

		assertThat(auditRecord1.getAuditData()).isEqualTo("time --some.password='******' --another-secret='******' | log");
		assertThat(auditRecord2.getAuditData()).isEqualTo("time --some.password='******' --another-secret='******' | log");
		assertThat(auditRecord1.getCorrelationId()).isEqualTo("myStream1234");
		assertThat(auditRecord2.getCorrelationId()).isEqualTo("myStream1234");

		assertThat(auditRecord1.getAuditOperation()).isEqualTo(AuditOperationType.STREAM);
		assertThat(auditRecord2.getAuditOperation()).isEqualTo(AuditOperationType.STREAM);

		if (AuditActionType.UNDEPLOY.equals(auditRecord1.getAuditAction())) {
			assertThat(auditRecord1.getAuditAction()).isEqualTo(AuditActionType.UNDEPLOY);
		} else {
			assertThat(auditRecord1.getAuditAction()).isEqualTo(AuditActionType.DELETE);
		}

		if (AuditActionType.UNDEPLOY.equals(auditRecord2.getAuditAction())) {
			assertThat(auditRecord2.getAuditAction()).isEqualTo(AuditActionType.UNDEPLOY);
		} else {
			assertThat(auditRecord2.getAuditAction()).isEqualTo(AuditActionType.DELETE);
		}
	}

	@Test
	void destroySingleStream() throws Exception {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream", "time | log");
		StreamDefinition streamDefinition2 = new StreamDefinition("myStream1", "time | log");
		repository.save(streamDefinition1);
		repository.save(streamDefinition2);
		assertThat(repository.count()).isEqualTo(2);

		mockMvc.perform(delete("/streams/definitions/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void displaySingleStream() throws Exception {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream", "time | log");
		repository.save(streamDefinition1);
		assertThat(repository.count()).isEqualTo(1);

		mockMvc.perform(get("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk()).andExpect(content().json("{name: \"myStream\"}"))
			.andExpect(content().json("{dslText: \"time | log\"}"));
	}

	@Test
	void displaySingleStreamWithRedaction() throws Exception {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream", "time --secret=foo | log");
		repository.save(streamDefinition1);
		assertThat(repository.count()).isEqualTo(1);

		mockMvc.perform(get("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk()).andExpect(content().json("{name: \"myStream\"}"))
			.andExpect(content().json("{dslText: \"time --secret='******' | log\"}"));
	}

	@Test
	void destroyStreamNotFound() throws Exception {
		mockMvc.perform(delete("/streams/definitions/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
		assertThat(repository.count()).isZero();
	}

	@Test
	void deploy() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(post("/streams/deployments/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());

		List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
		assertThat(updateRequests).hasSize(1);

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);
		assertThat(findChildPackageByName(pkg, "log")).isNotNull();
		assertThat(findChildPackageByName(pkg, "time")).isNotNull();
	}

	@Test
	void deployWithSensitiveData() throws Exception {
		repository.save(new StreamDefinition("myStream", "time --some.password=foobar --another-secret=kenny | log"));
		mockMvc.perform(post("/streams/deployments/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());

		List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
		assertThat(updateRequests).hasSize(1);

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		assertThat(findChildPackageByName(pkg, "log")).isNotNull();
		assertThat(findChildPackageByName(pkg, "time")).isNotNull();

		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();

		assertThat(auditRecords).hasSize(5);
		final AuditRecord auditRecord = auditRecords.get(4);

		assertThat(auditRecord.getAuditData())
			.isEqualTo("{\"streamDefinitionDslText\":\"time --some.password='******' --another-secret='******' | log\",\"deploymentProperties\":{}}");

		assertThat(auditRecord.getCorrelationId()).isEqualTo("myStream");

		assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.STREAM);
		assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.DEPLOY);
	}

	@Test
	void streamWithShortformProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time --fixed-delay=2 | log --level=WARN"));
		mockMvc.perform(post("/streams/deployments/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());

		List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
		assertThat(updateRequests).hasSize(1);

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		Package logPackage = findChildPackageByName(pkg, "log");
		assertThat(logPackage).isNotNull();
		Package timePackage = findChildPackageByName(pkg, "time");
		assertThat(timePackage).isNotNull();

		SpringCloudDeployerApplicationSpec logSpec = parseSpec(logPackage.getConfigValues().getRaw());
		assertThat(logSpec.getApplicationProperties()).containsEntry("log.consumer.level", "WARN");
		assertThat(logSpec.getApplicationProperties().get("level")).isNull();

		SpringCloudDeployerApplicationSpec timeSpec = parseSpec(timePackage.getConfigValues().getRaw());
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.integration.poller.fixed-delay", "2");
	}

	@Test
	void deployWithAppPropertiesOverride() throws Exception {
		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);

		mockMvc.perform(post("/streams/definitions")
			.queryParam("name", "myStream")
			.queryParam("definition", "time --fixed-delay=2 | log --level=WARN")
			.contentType("application/json")
		).andExpect(status().isCreated());
		Map<String, String> properties = new HashMap<>();
		properties.put("app.time.fixed-delay", "4");
		properties.put("app.log.level", "ERROR");
		properties.put("app.*.extra", "foo-bar");
		properties.put("app.time.producer.partitionKeyExpression", "payload");
		mockMvc.perform(post("/streams/deployments/myStream")
			.content(new ObjectMapper().writeValueAsBytes(properties))
			.contentType(MediaType.APPLICATION_JSON)
		).andExpect(status().isCreated());


		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());

		verify(skipperClient, times(1)).install(installRequestCaptor.capture());

		List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
		assertThat(updateRequests).hasSize(1);

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		Package logPackage = findChildPackageByName(pkg, "log");
		assertThat(logPackage).isNotNull();
		Package timePackage = findChildPackageByName(pkg, "time");
		assertThat(timePackage).isNotNull();

		SpringCloudDeployerApplicationSpec logSpec = parseSpec(logPackage.getConfigValues().getRaw());
		assertThat(logSpec.getApplicationProperties()).containsKey("log.consumer.level");
		assertThat(logSpec.getApplicationProperties()).containsKey("extra");
		assertThat(logSpec.getApplicationProperties()).containsEntry("log.consumer.level", "ERROR");
		assertThat(logSpec.getApplicationProperties()).containsEntry("extra", "foo-bar");


		SpringCloudDeployerApplicationSpec timeSpec = parseSpec(timePackage.getConfigValues().getRaw());
		assertThat(timeSpec.getApplicationProperties()).containsKey("spring.integration.poller.fixed-delay");
		assertThat(timeSpec.getApplicationProperties()).containsKey("extra");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.integration.poller.fixed-delay", "4");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("extra", "foo-bar");
	}

	@Test
	void deployWithAppPropertiesOverrideWithLabel() throws Exception {
		repository.save(new StreamDefinition("myStream", "a: time --fixed-delay=2 | b: log --level=WARN"));
		Map<String, String> properties = new HashMap<>();
		properties.put("app.a.fixed-delay", "4");
		properties.put("app.b.level", "ERROR");
		mockMvc.perform(post("/streams/deployments/myStream").content(new ObjectMapper().writeValueAsBytes(properties))
			.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());
		ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);
		verify(skipperClient, times(1)).install(installRequestCaptor.capture());

		List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
		assertThat(updateRequests).hasSize(1);

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		Package logPackage = findChildPackageByName(pkg, "b");
		assertThat(logPackage).isNotNull();
		Package timePackage = findChildPackageByName(pkg, "a");
		assertThat(timePackage).isNotNull();

		SpringCloudDeployerApplicationSpec logSpec = parseSpec(logPackage.getConfigValues().getRaw());
		logger.info("log:applicationProperties={}", logSpec.getApplicationProperties());
		logger.info("log:deploymentProperties={}", logSpec.getDeploymentProperties());
		assertThat(logSpec.getApplicationProperties()).containsEntry("log.consumer.level", "ERROR");

		SpringCloudDeployerApplicationSpec timeSpec = parseSpec(timePackage.getConfigValues().getRaw());
		logger.info("time:applicationProperties={}", timeSpec.getApplicationProperties());
		logger.info("time:deploymentProperties={}", timeSpec.getDeploymentProperties());
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.integration.poller.fixed-delay", "4");
	}

	@Test
	void duplicateDeploy() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));

		mockMvc.perform(post("/streams/deployments/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);
		assertThat(findChildPackageByName(pkg, "log")).isNotNull();
		assertThat(findChildPackageByName(pkg, "time")).isNotNull();

		streamStatusInfo.getStatus().setPlatformStatusAsAppStatusList(Arrays.asList(
			AppStatus.of("myStream.time-v1").generalState(DeploymentState.deploying).build(),
			AppStatus.of("myStream.log-v1").generalState(DeploymentState.deployed).build()));

		// Attempt to deploy already deployed stream
		mockMvc.perform(post("/streams/deployments/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
	}

	@Test
	void duplicateDeployWhenStreamIsBeingDeployed() throws Exception {
		// Mark the stream as already deployed
		streamStatusInfo.getStatus().setStatusCode(StatusCode.DEPLOYED);

		repository.save(new StreamDefinition("myStream", "time | log"));

		mockMvc.perform(post("/streams/deployments/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isConflict());
	}

	@Test
	void undeployNonDeployedStream() throws Exception {
		when(skipperClient.search(eq("myStream"), eq(false))).thenReturn(Arrays.asList(newPackageMetadata("myStream")));

		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(delete("/streams/deployments/myStream")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		verify(skipperClient, times(0)).upload(any());
		verify(skipperClient, times(0)).install(any());
		verify(skipperClient, times(1)).delete(eq("myStream"), anyBoolean());

		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();
		assertThat(auditRecords).hasSize(5);
		assertThat(auditRecords.get(4).getAuditOperation()).isEqualTo(AuditOperationType.STREAM);
		assertThat(auditRecords.get(4).getAuditAction()).isEqualTo(AuditActionType.UNDEPLOY);
	}

	@Test
	void undeployAllNonDeployedStream() throws Exception {
		when(skipperClient.search(eq("myStream1"), eq(false))).thenReturn(Arrays.asList(newPackageMetadata("myStream1")));
		when(skipperClient.search(eq("myStream2"), eq(false))).thenReturn(Arrays.asList(newPackageMetadata("myStream2")));

		repository.save(new StreamDefinition("myStream1", "time | log"));
		repository.save(new StreamDefinition("myStream2", "time | log"));
		mockMvc.perform(delete("/streams/deployments").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

		verify(skipperClient, times(0)).upload(any());
		verify(skipperClient, times(0)).install(any());
		verify(skipperClient, times(1)).delete(eq("myStream1"), anyBoolean());
		verify(skipperClient, times(1)).delete(eq("myStream2"), anyBoolean());

		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();
		assertThat(auditRecords).hasSize(6);
		assertThat(auditRecords.get(4).getAuditOperation()).isEqualTo(AuditOperationType.STREAM);
		assertThat(auditRecords.get(4).getAuditAction()).isEqualTo(AuditActionType.UNDEPLOY);
		assertThat(auditRecords.get(5).getAuditOperation()).isEqualTo(AuditOperationType.STREAM);
		assertThat(auditRecords.get(5).getAuditAction()).isEqualTo(AuditActionType.UNDEPLOY);
	}

	private PackageMetadata newPackageMetadata(String streamName) {
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName(streamName);
		return packageMetadata;
	}


	@Test
	void deployWithProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		Map<String, String> properties = new HashMap<>();
		properties.put("app.*.producer.partitionKeyExpression", "payload");
		properties.put("deployer.log.count", "2");
		properties.put("app.*.consumer.concurrency", "3");

		mockMvc.perform(post("/streams/deployments/myStream")
			.content(new ObjectMapper().writeValueAsBytes(properties))
			.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());
		ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);
		verify(skipperClient, times(1)).install(installRequestCaptor.capture());

		List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
		assertThat(updateRequests).hasSize(1);
		List<InstallRequest> installRequests = installRequestCaptor.getAllValues();
		assertThat(installRequests).hasSize(1);

		InstallRequest installRequest = installRequests.iterator().next();
		assertThat(installRequest.getInstallProperties().getPlatformName()).isEqualTo("default");
		assertThat(installRequest.getInstallProperties().getReleaseName()).isEqualTo("myStream");
		assertThat(installRequest.getPackageIdentifier().getRepositoryName()).isEqualTo("local");
		assertThat(installRequest.getPackageIdentifier().getPackageName()).isEqualTo("myStream");
		assertThat(installRequest.getPackageIdentifier().getPackageVersion()).isEqualTo("1.0.0");

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		Package logPackage = findChildPackageByName(pkg, "log");
		assertThat(logPackage).isNotNull();
		Package timePackage = findChildPackageByName(pkg, "time");
		assertThat(timePackage).isNotNull();

		SpringCloudDeployerApplicationSpec logSpec = parseSpec(logPackage.getConfigValues().getRaw());
		assertThat(logSpec.getApplicationProperties()).containsEntry(StreamPropertyKeys.INSTANCE_COUNT, "2");
		assertThat(logSpec.getApplicationProperties()).containsKey("spring.cloud.stream.bindings.input.consumer.concurrency");
		assertThat(logSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.input.consumer.concurrency", "3");

		assertThat(logSpec.getDeploymentProperties()).containsEntry(AppDeployer.COUNT_PROPERTY_KEY, "2");
		assertThat(logSpec.getDeploymentProperties()).containsEntry(AppDeployer.GROUP_PROPERTY_KEY, "myStream");

		SpringCloudDeployerApplicationSpec timeSpec = parseSpec(timePackage.getConfigValues().getRaw());
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.output.producer.partitionCount", "2");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.output.producer.partitionKeyExpression", "payload");
		assertThat(timeSpec.getDeploymentProperties()).containsEntry(AppDeployer.GROUP_PROPERTY_KEY, "myStream");
		assertThat(timeSpec.getDeploymentProperties().get(AppDeployer.INDEXED_PROPERTY_KEY)).isNull();
	}

	@Test
	void deployWithWildcardProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		Map<String, String> properties = new HashMap<>();
		properties.put("app.*.producer.partitionKeyExpression", "payload");
		properties.put("deployer.*.count", "2");
		properties.put("app.*.consumer.concurrency", "3");

		mockMvc.perform(post("/streams/deployments/myStream")
			.content(new ObjectMapper().writeValueAsBytes(properties))
			.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());
		ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);
		verify(skipperClient, times(1)).install(installRequestCaptor.capture());

		List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
		assertThat(updateRequests).hasSize(1);
		List<InstallRequest> installRequests = installRequestCaptor.getAllValues();
		assertThat(installRequests).hasSize(1);

		InstallRequest installRequest = installRequests.iterator().next();
		assertThat(installRequest.getInstallProperties().getPlatformName()).isEqualTo("default");
		assertThat(installRequest.getInstallProperties().getReleaseName()).isEqualTo("myStream");
		assertThat(installRequest.getPackageIdentifier().getRepositoryName()).isEqualTo("local");
		assertThat(installRequest.getPackageIdentifier().getPackageName()).isEqualTo("myStream");
		assertThat(installRequest.getPackageIdentifier().getPackageVersion()).isEqualTo("1.0.0");

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		Package logPackage = findChildPackageByName(pkg, "log");
		assertThat(logPackage).isNotNull();
		Package timePackage = findChildPackageByName(pkg, "time");
		assertThat(timePackage).isNotNull();

		SpringCloudDeployerApplicationSpec logSpec = parseSpec(logPackage.getConfigValues().getRaw());
		assertThat(logSpec.getApplicationProperties()).containsEntry(StreamPropertyKeys.INSTANCE_COUNT, "2");
		assertThat(logSpec.getApplicationProperties()).containsKey("spring.cloud.stream.bindings.input.consumer.concurrency");
		assertThat(logSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.input.consumer.concurrency", "3");

		assertThat(logSpec.getDeploymentProperties()).containsEntry(AppDeployer.COUNT_PROPERTY_KEY, "2");
		assertThat(logSpec.getDeploymentProperties()).containsEntry(AppDeployer.GROUP_PROPERTY_KEY, "myStream");

		SpringCloudDeployerApplicationSpec timeSpec = parseSpec(timePackage.getConfigValues().getRaw());
		assertThat(timeSpec.getApplicationProperties()).containsEntry(StreamPropertyKeys.INSTANCE_COUNT, "2");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.output.producer.partitionCount", "2");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.output.producer.partitionKeyExpression", "payload");
		assertThat(timeSpec.getDeploymentProperties()).containsEntry(AppDeployer.COUNT_PROPERTY_KEY, "2");
		assertThat(timeSpec.getDeploymentProperties()).containsEntry(AppDeployer.GROUP_PROPERTY_KEY, "myStream");
		assertThat(timeSpec.getDeploymentProperties().get(AppDeployer.INDEXED_PROPERTY_KEY)).isNull();
	}

	@Test
	void defaultApplicationPropertiesYamlResourceNoPlatform() throws Exception {
		testDefaultApplicationPropertiesResource(new DefaultResourceLoader().getResource(
			"classpath:/defaults/test-application-stream-common-properties-defaults.yml"), new HashMap<>());
	}

	@Test
	void defaultApplicationPropertiesYamlResourceNoPlatformDefault() throws Exception {
		testDefaultApplicationPropertiesResource(new DefaultResourceLoader().getResource(
				"classpath:/defaults/test-application-stream-common-properties-defaults.yml"),
			Collections.singletonMap(SkipperStream.SKIPPER_PLATFORM_NAME, "default"));
	}

	@Test
	void defaultApplicationPropertiesYamlResourceNoPlatformPcf() throws Exception {
		testDefaultApplicationPropertiesResource(new DefaultResourceLoader().getResource(
				"classpath:/defaults/test-application-stream-common-properties-defaults.yml"),
			Collections.singletonMap(SkipperStream.SKIPPER_PLATFORM_NAME, "pcf"));
	}

	@Test
	void defaultApplicationPropertiesYamlResourceNoPlatformK8s() throws Exception {
		testDefaultApplicationPropertiesResource(new DefaultResourceLoader().getResource(
				"classpath:/defaults/test-application-stream-common-properties-defaults.yml"),
			Collections.singletonMap(SkipperStream.SKIPPER_PLATFORM_NAME, "k8s"));
	}

	@Test
	void defaultApplicationPropertiesPropertyResourceNoPlatform() throws Exception {
		testDefaultApplicationPropertiesResource(new DefaultResourceLoader().getResource(
				"classpath:/defaults/test-application-stream-common-properties-defaults.properties"),
			Collections.emptyMap());
	}

	@Test
	void defaultApplicationPropertiesPropertyResourceK8s() throws Exception {
		testDefaultApplicationPropertiesResource(new DefaultResourceLoader().getResource(
				"classpath:/defaults/test-application-stream-common-properties-defaults.properties"),
			Collections.singletonMap(SkipperStream.SKIPPER_PLATFORM_NAME, "k8s"));
	}

	@Test
	void defaultApplicationPropertiesPropertyResourceDefault() throws Exception {
		testDefaultApplicationPropertiesResource(new DefaultResourceLoader().getResource(
				"classpath:/defaults/test-application-stream-common-properties-defaults.properties"),
			Collections.singletonMap(SkipperStream.SKIPPER_PLATFORM_NAME, "default"));
	}

	@Test
	void defaultApplicationPropertiesPropertyResourcePCF() throws Exception {
		testDefaultApplicationPropertiesResource(new DefaultResourceLoader().getResource(
				"classpath:/defaults/test-application-stream-common-properties-defaults.properties"),
			Collections.singletonMap(SkipperStream.SKIPPER_PLATFORM_NAME, "pcf"));
	}


	private void testDefaultApplicationPropertiesResource(Resource newResource, Map<String, String> skipperProperties) throws Exception {
		Resource oldResource = appsProperties.getStreamResource();

		try {
			repository.save(new StreamDefinition("myStream", "time | log"));
			appsProperties.setStreamResource(newResource);
			mockMvc.perform(
				post("/streams/deployments/myStream")
					.content(new ObjectMapper().writeValueAsBytes(skipperProperties))
					.contentType(MediaType.APPLICATION_JSON)
			).andExpect(status().isCreated());

			ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
			verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());
			ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);
			verify(skipperClient, times(1)).install(installRequestCaptor.capture());

			List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
			assertThat(updateRequests).hasSize(1);
			List<InstallRequest> installRequests = installRequestCaptor.getAllValues();
			assertThat(installRequests).hasSize(1);

			String platformName = skipperProperties.getOrDefault(SkipperStream.SKIPPER_PLATFORM_NAME, "default");

			InstallRequest installRequest = installRequests.iterator().next();
			assertThat(installRequest.getInstallProperties().getPlatformName()).isEqualTo(platformName);
			assertThat(installRequest.getInstallProperties().getReleaseName()).isEqualTo("myStream");
			assertThat(installRequest.getPackageIdentifier().getRepositoryName()).isEqualTo("local");
			assertThat(installRequest.getPackageIdentifier().getPackageName()).isEqualTo("myStream");
			assertThat(installRequest.getPackageIdentifier().getPackageVersion()).isEqualTo("1.0.0");

			Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

			Package logPackage = findChildPackageByName(pkg, "log");
			assertThat(logPackage).isNotNull();

			SpringCloudDeployerApplicationSpec logSpec = parseSpec(logPackage.getConfigValues().getRaw());

			// Check for the presence of defaults/test-application-stream-common-properties-defaults.yml properties.
			assertThat(logSpec.getApplicationProperties()).containsEntry("my.test.static.property", "Test");
			assertThat(logSpec.getApplicationProperties()).containsEntry("my.test.property.with.placeholder", "${my.placeholder}");
			if (platformName.equalsIgnoreCase("default")) {
				assertThat(logSpec.getApplicationProperties()).containsEntry("my.local.static.property", "TestLocal");
				assertThat(logSpec.getApplicationProperties()).containsEntry("my.local.property.with.placeholder", "${my.placeholder.local}");
			} else if (platformName.equalsIgnoreCase("k8s")) {
				assertThat(logSpec.getApplicationProperties()).containsEntry("my.kubernetes.static.property", "TestKubernetes");
				assertThat(logSpec.getApplicationProperties()).containsEntry("my.kubernetes.property.with.placeholder", "${my.placeholder.kubernetes}");
			} else if (platformName.equalsIgnoreCase("cloudfoundry")) {
				assertThat(logSpec.getApplicationProperties()).containsEntry("my.cloudfoundry.static.property", "TestCloudfoundry");
				assertThat(logSpec.getApplicationProperties()).containsEntry("my.cloudfoundry.property.with.placeholder", "${my.placeholder.cloudfoundry}");
			}

			// Default stream metrics tags are overridden and should not be set
			assertThat(logSpec.getApplicationProperties().get("management.metrics.tags.stream.name")).isNull();
			assertThat(logSpec.getApplicationProperties().get("management.metrics.tags.application.name")).isNull();
			assertThat(logSpec.getApplicationProperties().get("management.metrics.tags.application.type")).isNull();
			assertThat(logSpec.getApplicationProperties().get("management.metrics.tags.instance.index")).isNull();
			assertThat(logSpec.getApplicationProperties().get("management.metrics.tags.application.guid")).isNull();

			Package timePackage = findChildPackageByName(pkg, "time");
			assertThat(timePackage);

			SpringCloudDeployerApplicationSpec timeSpec = parseSpec(timePackage.getConfigValues().getRaw());
			assertThat(timeSpec.getDeploymentProperties()).containsEntry(AppDeployer.GROUP_PROPERTY_KEY, "myStream");
			assertThat(timeSpec.getDeploymentProperties().get(AppDeployer.INDEXED_PROPERTY_KEY)).isNull();

			// Check for the presence of defaults/test-application-stream-common-properties-defaults.yml properties.
			assertThat(timeSpec.getApplicationProperties()).containsEntry("my.test.static.property", "Test");
			assertThat(timeSpec.getApplicationProperties()).containsEntry("my.test.property.with.placeholder", "${my.placeholder}");

			// Default stream metrics tags are overridden and should not be set
			assertThat(timeSpec.getApplicationProperties().get("management.metrics.tags.stream.name")).isNull();
			assertThat(timeSpec.getApplicationProperties().get("management.metrics.tags.application.name")).isNull();
			assertThat(timeSpec.getApplicationProperties().get("management.metrics.tags.application.type")).isNull();
			assertThat(timeSpec.getApplicationProperties().get("management.metrics.tags.instance.index")).isNull();
			assertThat(timeSpec.getApplicationProperties().get("management.metrics.tags.application.guid")).isNull();
		} finally {
			this.appsProperties.setStreamResource(oldResource);
		}
	}

	@Test
	void deployWithCommonApplicationProperties() throws Exception {
		repository.save(new StreamDefinition("myStream", "time | log"));
		Map<String, String> properties = new HashMap<>();
		properties.put("app.*.producer.partitionKeyExpression", "payload");
		properties.put("deployer.*.count", "2");
		properties.put("app.*.consumer.concurrency", "3");

		mockMvc.perform(post("/streams/deployments/myStream")
			.content(new ObjectMapper().writeValueAsBytes(properties))
			.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient, times(1)).upload(uploadRequestCaptor.capture());
		ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);
		verify(skipperClient, times(1)).install(installRequestCaptor.capture());

		List<UploadRequest> updateRequests = uploadRequestCaptor.getAllValues();
		assertThat(updateRequests).hasSize(1);
		List<InstallRequest> installRequests = installRequestCaptor.getAllValues();
		assertThat(installRequests).hasSize(1);

		InstallRequest installRequest = installRequests.iterator().next();
		assertThat(installRequest.getInstallProperties().getPlatformName()).isEqualTo("default");
		assertThat(installRequest.getInstallProperties().getReleaseName()).isEqualTo("myStream");
		assertThat(installRequest.getPackageIdentifier().getRepositoryName()).isEqualTo("local");
		assertThat(installRequest.getPackageIdentifier().getPackageName()).isEqualTo("myStream");
		assertThat(installRequest.getPackageIdentifier().getPackageVersion()).isEqualTo("1.0.0");

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		Package logPackage = findChildPackageByName(pkg, "log");
		assertThat(logPackage).isNotNull();

		SpringCloudDeployerApplicationSpec logSpec = parseSpec(logPackage.getConfigValues().getRaw());

		// Default stream metrics tags for logSpec
		assertThat(logSpec.getApplicationProperties()).containsEntry("management.metrics.tags.stream.name", "${spring.cloud.dataflow.stream.name:unknown}");
		assertThat(logSpec.getApplicationProperties()).containsEntry("management.metrics.tags.application.name", "${vcap.application.application_name:${spring.cloud.dataflow.stream.app.label:unknown}}");
		assertThat(logSpec.getApplicationProperties()).containsEntry("management.metrics.tags.application.type", "${spring.cloud.dataflow.stream.app.type:unknown}");
		assertThat(logSpec.getApplicationProperties()).containsEntry("management.metrics.tags.instance.index", "${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}");
		assertThat(logSpec.getApplicationProperties()).containsEntry("management.metrics.tags.application.guid", "${spring.cloud.application.guid:unknown}");

		assertThat(logSpec.getApplicationProperties()).containsEntry(StreamPropertyKeys.INSTANCE_COUNT, "2");
		assertThat(logSpec.getApplicationProperties()).containsKey("spring.cloud.stream.bindings.input.consumer.concurrency");
		assertThat(logSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.input.consumer.concurrency", "3");
		assertThat(logSpec.getDeploymentProperties()).containsEntry(AppDeployer.COUNT_PROPERTY_KEY, "2");
		assertThat(logSpec.getDeploymentProperties()).containsEntry(AppDeployer.GROUP_PROPERTY_KEY, "myStream");

		Package timePackage = findChildPackageByName(pkg, "time");
		assertThat(timePackage).isNotNull();

		SpringCloudDeployerApplicationSpec timeSpec = parseSpec(timePackage.getConfigValues().getRaw());

		// Default stream metrics tags for logSpec
		assertThat(timeSpec.getApplicationProperties()).containsEntry("management.metrics.tags.stream.name", "${spring.cloud.dataflow.stream.name:unknown}");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("management.metrics.tags.application.name", "${vcap.application.application_name:${spring.cloud.dataflow.stream.app.label:unknown}}");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("management.metrics.tags.application.type", "${spring.cloud.dataflow.stream.app.type:unknown}");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("management.metrics.tags.instance.index", "${vcap.application.instance_index:${spring.cloud.stream.instanceIndex:0}}");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("management.metrics.tags.application.guid", "${spring.cloud.application.guid:unknown}");

		assertThat(timeSpec.getApplicationProperties()).containsEntry(StreamPropertyKeys.INSTANCE_COUNT, "2");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.output.producer.partitionCount", "2");
		assertThat(timeSpec.getApplicationProperties()).containsEntry("spring.cloud.stream.bindings.output.producer.partitionKeyExpression", "payload");
		assertThat(timeSpec.getDeploymentProperties()).containsEntry(AppDeployer.COUNT_PROPERTY_KEY, "2");
		assertThat(timeSpec.getDeploymentProperties()).containsEntry(AppDeployer.GROUP_PROPERTY_KEY, "myStream");
		assertThat(timeSpec.getDeploymentProperties().get(AppDeployer.INDEXED_PROPERTY_KEY)).isNull();
	}

	@Test
	void aggregateState() {
		assertThat(StreamDeployerUtil.aggregateState(EnumSet.of(DeploymentState.deployed, DeploymentState.failed)))
			.isEqualTo(DeploymentState.partial);
		assertThat(StreamDeployerUtil.aggregateState(EnumSet.of(DeploymentState.unknown, DeploymentState.failed)))
			.isEqualTo(DeploymentState.failed);
		assertThat(
			StreamDeployerUtil.aggregateState(
				EnumSet.of(DeploymentState.deployed, DeploymentState.failed, DeploymentState.error)))
			.isEqualTo(DeploymentState.error);
		assertThat(StreamDeployerUtil.aggregateState(EnumSet.of(DeploymentState.deployed, DeploymentState.undeployed)))
			.isEqualTo(DeploymentState.partial);
		assertThat(StreamDeployerUtil.aggregateState(EnumSet.of(DeploymentState.deployed, DeploymentState.unknown)))
			.isEqualTo(DeploymentState.partial);
		assertThat(StreamDeployerUtil.aggregateState(EnumSet.of(DeploymentState.undeployed, DeploymentState.unknown)))
			.isEqualTo(DeploymentState.partial);
		assertThat(StreamDeployerUtil.aggregateState(EnumSet.of(DeploymentState.unknown)))
			.isEqualTo(DeploymentState.undeployed);
		assertThat(StreamDeployerUtil.aggregateState(EnumSet.of(DeploymentState.deployed)))
			.isEqualTo(DeploymentState.deployed);
	}

	@Test
	void appDeploymentFailure() throws Exception {
		when(skipperClient.upload(any())).thenThrow(new RestClientException("bad"));
		repository.save(new StreamDefinition("myStream", "time | log"));
		mockMvc.perform(post("/streams/deployments/myStream").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is5xxServerError());
	}

	@Test
	void validateStream() throws Exception {
		assertThat(repository.count()).isZero();
		mockMvc.perform(post("/streams/definitions")
			.param("name", "myStream1")
			.param("definition", "time | log")
			.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(get("/streams/validation/myStream1").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk()).andExpect(content()
				.json("{\"appName\":\"myStream1\",\"appStatuses\":{\"source:time\":\"valid\",\"sink:log\":\"valid\"},\"dsl\":\"time | log\"}"));
	}

	private SpringCloudDeployerApplicationSpec parseSpec(String yamlString) throws IOException {
		YAMLMapper mapper = new YAMLMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		MappingIterator<SpringCloudDeployerApplicationManifest> it = mapper
			.readerFor(SpringCloudDeployerApplicationManifest.class).readValues(yamlString);
		return it.next().getSpec();
	}

	private Package findChildPackageByName(Package pkg, String childPackageName) {
		return pkg.getDependencies().stream()
			.filter(p -> p.getMetadata().getName().equalsIgnoreCase(childPackageName)).findFirst().get();
	}
}
