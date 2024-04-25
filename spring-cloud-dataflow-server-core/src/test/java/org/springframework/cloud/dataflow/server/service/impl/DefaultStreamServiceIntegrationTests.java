/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.support.MockUtils;
import org.springframework.cloud.dataflow.server.support.SkipperPackageUtils;
import org.springframework.cloud.dataflow.server.support.TestResourceUtils;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author Chris Bono
 */
@SpringBootTest(classes = TestDependencies.class)
@TestPropertySource(properties = { "spring.main.banner-mode=off"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class DefaultStreamServiceIntegrationTests {

	@Autowired
	private StreamService streamService;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private AppRegistryService appRegistryService;

	@Autowired
	private AuditRecordService auditRecordService;

	@MockBean
	private SkipperClient skipperClient;

	@BeforeEach
	public void before() throws URISyntaxException {
		createTickTock();
		this.skipperClient = MockUtils.configureMock(this.skipperClient);
	}

	@AfterEach
	public void destroyStream() {
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName("ticktock");
		when(this.skipperClient.search(anyString(), anyBoolean())).thenReturn(Collections.singletonList(packageMetadata));
		streamService.undeployStream("ticktock");
		streamDefinitionRepository.deleteAll();
	}

	@Test
	public void validateSkipperDeploymentProperties() {

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		// override log version to 1.2.0.RELEASE
		deploymentProperties.put("badthing.version.log", "1.2.0.RELEASE");

		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));
		try {
			streamService.deployStream("ticktock", deploymentProperties);
			fail("Expected an IllegalArgumentException to be thrown.");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Only deployment property keys starting with 'app.' or 'deployer.'" +
					"  or 'version.' allowed, got 'badthing.version.log'");
		}
	}

	@Test
	public void testInstallVersionOverride() throws IOException {

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		// override log to 1.2.0.RELEASE
		deploymentProperties.put("version.log", "1.2.0.RELEASE");
		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));

		streamService.deployStream("ticktock", deploymentProperties);

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient).upload(uploadRequestCaptor.capture());

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		// ExpectedYaml will have version: 1.2.0.RELEASE and not 1.1.1.RELEASE
		String expectedYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "install.yml").getInputStream(),
				Charset.defaultCharset());
		Package logPackage = null;
		for (Package subpkg : pkg.getDependencies()) {
			if (subpkg.getMetadata().getName().equals("log")) {
				logPackage = subpkg;
			}
		}
		assertThat(logPackage).isNotNull();
		String actualYaml = logPackage.getConfigValues().getRaw();

		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);

		Object actualYamlLoaded = yaml.load(actualYaml);
		Object expectedYamlLoaded = yaml.load(expectedYaml);

		assertThat(actualYamlLoaded).isEqualTo(expectedYamlLoaded);
	}

	@Test
	public void testUpdateStreamDslOnDeploy() throws IOException {

		// Create stream
		String originalDsl = "time --fixed-delay=100 --spring.cloud.config.password=5150 | log --level=DEBUG";
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", originalDsl);
		this.streamDefinitionRepository.deleteById(streamDefinition.getName());
		this.streamDefinitionRepository.save(streamDefinition);

		StreamDefinition streamDefinitionBeforeDeploy = this.streamDefinitionRepository.findById("ticktock").get();
		assertThat(streamDefinitionBeforeDeploy.getDslText()).isEqualTo(originalDsl);

		String expectedReleaseManifest = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "deployManifest.yml").getInputStream(),
				Charset.defaultCharset());
		Release release = new Release();
		Manifest manifest = new Manifest();
		manifest.setData(expectedReleaseManifest);
		release.setManifest(manifest);
		when(skipperClient.install(isA(InstallRequest.class))).thenReturn(release);
		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		deploymentProperties.put("version.log", "1.2.0.RELEASE");

		streamService.deployStream("ticktock", deploymentProperties);

		assertThatAuditRecordDataIsRedacted(AuditActionType.DEPLOY);
		assertThatAuditRecordDataIsRedacted(AuditActionType.UPDATE);

		String expectedUpdatedDsl = "time --spring.cloud.config.password=5150 --trigger.fixed-delay=100 | log --log.level=DEBUG";
		StreamDefinition streamDefinitionAfterDeploy = this.streamDefinitionRepository.findById("ticktock").get();
		assertThat(streamDefinitionAfterDeploy.getDslText()).isEqualTo(expectedUpdatedDsl);
	}

	private void assertThatAuditRecordDataIsRedacted(AuditActionType auditActionType) {
		Page<AuditRecord> auditRecords = this.auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionTypeAndDate(
				PageRequest.of(0, 1),
				new AuditActionType[]{ auditActionType },
				new AuditOperationType[]{ AuditOperationType.STREAM },
				Instant.now().minusSeconds(5),
				Instant.now().plusSeconds(1)
		);
		assertThat(auditRecords.getNumberOfElements()).isEqualTo(1);
		assertThat(auditRecords.get().map(AuditRecord::getAuditData).findFirst())
				.hasValueSatisfying((auditData) -> assertThat(auditData)
					.contains("--spring.cloud.config.password='******'")
					.doesNotContain("--spring.cloud.config.password='5150'"));
	}

	@Test
	public void testUpdateStreamDslOnUpgrade() throws IOException {

		// Create stream
		StreamDefinition streamDefinition = new StreamDefinition("ticktock",
				"time --fixed-delay=100 | log --level=DEBUG", "time | log", "demo");
		this.streamDefinitionRepository.deleteById(streamDefinition.getName());
		this.streamDefinitionRepository.save(streamDefinition);

		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));

		streamService.deployStream("ticktock", createSkipperDeploymentProperties());

		StreamDefinition streamDefinitionBeforeDeploy = this.streamDefinitionRepository.findById("ticktock").get();
		assertThat(streamDefinitionBeforeDeploy.getDslText())
				.isEqualTo("time --fixed-delay=100 | log --level=DEBUG");
		assertThat(streamDefinitionBeforeDeploy.getDescription()).isEqualTo("demo");

		String expectedReleaseManifest = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "upgradeManifest.yml").getInputStream(),
				Charset.defaultCharset());
		Release release = new Release();
		Manifest manifest = new Manifest();
		manifest.setData(expectedReleaseManifest);
		release.setManifest(manifest);
		when(skipperClient.upgrade(isA(UpgradeRequest.class))).thenReturn(release);

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		deploymentProperties.put("version.log", "1.2.0.RELEASE");
		streamService.updateStream("ticktock",
				new UpdateStreamRequest("ticktock", new PackageIdentifier(), deploymentProperties));

		StreamDefinition streamDefinitionAfterDeploy = this.streamDefinitionRepository.findById("ticktock").get();
		assertThat(streamDefinitionAfterDeploy.getDslText())
				.isEqualTo("time --trigger.fixed-delay=200 | log --log.level=INFO");
		assertThat(streamDefinitionAfterDeploy.getOriginalDslText()).isEqualTo("time | log");
		assertThat(streamDefinitionAfterDeploy.getDescription()).isEqualTo("demo");
	}

	@Test
	public void testUpdateStreamDslOnRollback() throws IOException {

		// Create stream
		StreamDefinition streamDefinition = new StreamDefinition("ticktock",
				"time --fixed-delay=100 | log --level=DEBUG");
		this.streamDefinitionRepository.deleteById(streamDefinition.getName());
		this.streamDefinitionRepository.save(streamDefinition);

		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));

		streamService.deployStream("ticktock", createSkipperDeploymentProperties());

		// Update Stream
		StreamDefinition streamDefinitionBeforeDeploy = this.streamDefinitionRepository.findById("ticktock").get();
		assertThat(streamDefinitionBeforeDeploy.getDslText())
				.isEqualTo("time --fixed-delay=100 | log --level=DEBUG");

		String expectedReleaseManifest = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "upgradeManifest.yml").getInputStream(),
				Charset.defaultCharset());
		Release release = new Release();
		Manifest manifest = new Manifest();
		manifest.setData(expectedReleaseManifest);
		release.setManifest(manifest);
		when(skipperClient.upgrade(isA(UpgradeRequest.class))).thenReturn(release);

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		deploymentProperties.put("version.log", "1.2.0.RELEASE");
		streamService.updateStream("ticktock",
				new UpdateStreamRequest("ticktock", new PackageIdentifier(), deploymentProperties));

		StreamDefinition streamDefinitionAfterDeploy = this.streamDefinitionRepository.findById("ticktock").get();
		assertThat(streamDefinitionAfterDeploy.getDslText())
				.isEqualTo("time --trigger.fixed-delay=200 | log --log.level=INFO");

		// Rollback Stream
		String rollbackReleaseManifest = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "rollbackManifest.yml").getInputStream(),
				Charset.defaultCharset());
		Release rollbackRelease = new Release();
		Manifest rollbackManifest = new Manifest();
		rollbackManifest.setData(rollbackReleaseManifest);
		rollbackRelease.setManifest(rollbackManifest);
		when(skipperClient.rollback(isA(RollbackRequest.class))).thenReturn(rollbackRelease);

		streamService.rollbackStream("ticktock", 0);
		StreamDefinition streamDefinitionAfterRollback = this.streamDefinitionRepository.findById("ticktock").get();
		assertThat(streamDefinitionAfterRollback.getDslText())
				.isEqualTo("time --trigger.fixed-delay=100 | log --log.level=DEBUG");
	}

	@Test
	public void testDeployHasActuatorProps() throws IOException {

		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		streamService.deployStream("ticktock", deploymentProperties);

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient).upload(uploadRequestCaptor.capture());

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);

		// ExpectedYaml will have version: 1.2.0.RELEASE and not 1.1.1.RELEASE
		Package logPackage = null;
		for (Package subpkg : pkg.getDependencies()) {
			if (subpkg.getMetadata().getName().equals("log")) {
				logPackage = subpkg;
			}
		}
		assertThat(logPackage).isNotNull();
		String actualYaml = logPackage.getConfigValues().getRaw();

		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);

		Object actualYamlLoaded = yaml.load(actualYaml);

		assertThat(actualYamlLoaded).isInstanceOf(Map.class)
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.extractingByKey("spec", InstanceOfAssertFactories.MAP)
				.extractingByKey("applicationProperties", InstanceOfAssertFactories.MAP)
				.containsEntry("management.endpoints.web.exposure.include", "health,info,bindings");
	}

	@Test
	public void testStreamInfo() throws IOException {

		// Create stream
		StreamDefinition streamDefinition = new StreamDefinition("ticktock",
				"time --fixed-delay=100 | log --level=DEBUG");
		this.streamDefinitionRepository.deleteById(streamDefinition.getName());
		this.streamDefinitionRepository.save(streamDefinition);

		when(skipperClient.status(eq("ticktock"))).thenThrow(new ReleaseNotFoundException(""));

		Map<String, String> deploymentProperties = createSkipperDeploymentProperties();
		this.streamService.deployStream("ticktock", deploymentProperties);
		String releaseManifest = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "deployManifest.yml").getInputStream(),
				Charset.defaultCharset());
		String deploymentProps = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "deploymentProps.json").getInputStream(),
				Charset.defaultCharset());
		when(skipperClient.manifest(streamDefinition.getName())).thenReturn(releaseManifest);
		StreamDeployment streamDeployment = this.streamService.info(streamDefinition.getName());
		assertThat(streamDeployment.getStreamName()).isEqualTo(streamDefinition.getName());
		// TODO: this is unreliable to test json like this as field order may change
		assertThat(streamDeployment.getDeploymentProperties()).contains(deploymentProps);
	}

	private Map<String, String> createSkipperDeploymentProperties() {
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put(SkipperStream.SKIPPER_PACKAGE_NAME, "ticktock");
		deploymentProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, "1.0.0");
		return deploymentProperties;
	}

	private void createTickTock() throws URISyntaxException {
		String timeUri = "maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE";
		appRegistryService.save("time", ApplicationType.source, "1.2.0.RELEASE", new URI(timeUri), null, null);
		String logUri = "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.1.1.RELEASE";
		appRegistryService.save("log", ApplicationType.sink, "1.2.0.RELEASE", new URI(logUri), null, null);

		// Create stream
		StreamDefinition streamDefinition = new StreamDefinition("ticktock", "time | log");
		this.streamDefinitionRepository.save(streamDefinition);
	}
}
