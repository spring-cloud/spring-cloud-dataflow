/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.DefaultStreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.support.MockUtils;
import org.springframework.cloud.dataflow.server.support.SkipperPackageUtils;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.Dependency;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.LogInfo;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.domain.VersionInfo;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.StreamUtils;

/**
 * @author Mark Pollack
 * @author Soby Chacko
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
class SkipperStreamDeployerTests {

	@Test
	void escapeBackslashProperties() throws IOException {

		AppRegistryService appRegistryService = mock(AppRegistryService.class);

		when(appRegistryService.appExist(eq("time"), eq(ApplicationType.source), eq("1.2.0.RELEASE")))
				.thenReturn(true);
		when(appRegistryService.appExist(eq("log"), eq(ApplicationType.sink), eq("1.2.0.RELEASE")))
				.thenReturn(true);


		HashMap<String, String> timeAppProps = new HashMap<>();
		timeAppProps.put("spring.cloud.dataflow.stream.app.type", "source");

		// Set the properties for:
		// time --foo.expression=\d --bar=\d --complex.expression="#jsonPath(payload,'$.name') matches '\d*'"
		timeAppProps.put("foo.expression", "\\d");
		timeAppProps.put("bar", "\\d");
		timeAppProps.put("complex.expression", "#jsonPath(payload,'$.name') matches '\\d*'");
		timeAppProps.put("bar.expression", "\\d \\\\ \\0 \\a \\b \\t \\n \\v \\f \\r \\e \\N \\_ \\L \\P \\");

		AppDefinition timeAppDefinition = new AppDefinition("time", timeAppProps);
		MavenResource timeResource = new MavenResource.Builder()
				.artifactId("time-source-rabbit").groupId("org.springframework.cloud.stream.app")
				.version("1.2.0.RELEASE").build();
		when(appRegistryService.getResourceVersion(timeResource)).thenReturn(timeResource.getVersion());
		AppDeploymentRequest timeAppDeploymentRequest = new AppDeploymentRequest(timeAppDefinition, timeResource);

		List<AppDeploymentRequest> appDeploymentRequests = Arrays.asList(timeAppDeploymentRequest);

		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, "1.0.1");

		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1",
				"t1: time | log",
				appDeploymentRequests,
				skipperDeployerProperties);

		SkipperClient skipperClient = MockUtils.createSkipperClientMock();

		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());

		when(streamDefinitionRepository.findById("test1")).thenReturn(Optional.of(new StreamDefinition("test1", "t1: time | log")));
		skipperStreamDeployer.deployStream(streamDeploymentRequest);

		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		verify(skipperClient).upload(uploadRequestCaptor.capture());
		assertThat(uploadRequestCaptor.getValue()).isNotNull();
		assertThat(uploadRequestCaptor.getValue().getName()).isEqualTo("test1");
		assertThat(uploadRequestCaptor.getValue().getVersion()).isEqualTo("1.0.1");

		Package pkg = SkipperPackageUtils.loadPackageFromBytes(uploadRequestCaptor);
		Package timePackage = pkg.getDependencies().get(0);

		assertThat(timePackage).isNotNull();
		assertThat(timePackage.getConfigValues().getRaw()).contains("\"foo.expression\": \"\\\\d\"");
		assertThat(timePackage.getConfigValues().getRaw()).contains("\"bar\": \"\\\\d\"");
		assertThat(timePackage.getConfigValues().getRaw()).contains("\"complex.expression\": \"#jsonPath(payload,'$.name') matches '\\\\d*'\"");
		assertThat(timePackage.getConfigValues().getRaw()).contains("\"bar.expression\": \"\\\\d \\\\\\\\ \\\\0 \\\\a \\\\b \\\\t \\\\n \\\\v \\\\f \\\\r \\\\e \\\\N \\\\_ \\\\L \\\\P \\\\\"");
	}

	@Test
	void installUploadProperties() {
		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_NAME, "package1");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, "1.0.1");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PLATFORM_NAME, "testPlatform");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_REPO_NAME, "mylocal-repo1");
		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				new ArrayList<>(),
				skipperDeployerProperties);

		SkipperClient skipperClient = MockUtils.createSkipperClientMock();

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class), new DefaultStreamDefinitionService());
		skipperStreamDeployer.deployStream(streamDeploymentRequest);
		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);
		verify(skipperClient).install(installRequestCaptor.capture());
		assertThat(installRequestCaptor.getValue().getPackageIdentifier()).isNotNull();
		assertThat(installRequestCaptor.getValue().getPackageIdentifier().getPackageName()).isEqualTo("package1");
		assertThat(installRequestCaptor.getValue().getPackageIdentifier().getPackageVersion()).isEqualTo("1.0.1");
		assertThat(installRequestCaptor.getValue().getPackageIdentifier().getRepositoryName()).isEqualTo("mylocal-repo1");
		assertThat(installRequestCaptor.getValue().getInstallProperties().getPlatformName()).isEqualTo("testPlatform");
		verify(skipperClient).upload(uploadRequestCaptor.capture());
		assertThat(uploadRequestCaptor.getValue()).isNotNull();
		assertThat(uploadRequestCaptor.getValue().getName()).isEqualTo("package1");
		assertThat(uploadRequestCaptor.getValue().getVersion()).isEqualTo("1.0.1");
		assertThat(installRequestCaptor.getValue().getPackageIdentifier().getRepositoryName()).isEqualTo("mylocal-repo1");
		assertThat(installRequestCaptor.getValue().getInstallProperties().getPlatformName()).isEqualTo("testPlatform");
	}

	@Test
	void invalidPlatformName() {
		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_NAME, "package1");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, "1.0.1");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PLATFORM_NAME, "badPlatform");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_REPO_NAME, "mylocal-repo1");
		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				new ArrayList<>(),
				skipperDeployerProperties);

		SkipperClient skipperClient = MockUtils.createSkipperClientMock();

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class), new DefaultStreamDefinitionService());
		try {
			skipperStreamDeployer.deployStream(streamDeploymentRequest);
			fail("");
		}
		catch (IllegalArgumentException expected) {
			assertThat(expected).hasMessage("No platform named 'badPlatform'");
		}
	}

	@Test
	void noPlatforms() {
		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_NAME, "package1");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, "1.0.1");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_REPO_NAME, "mylocal-repo1");
		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				new ArrayList<>(),
				skipperDeployerProperties);

		SkipperClient skipperClient = mock(SkipperClient.class);
		when(skipperClient.listDeployers()).thenReturn(new ArrayList<>());

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class), new DefaultStreamDefinitionService());
		try {
			skipperStreamDeployer.deployStream(streamDeploymentRequest);
			fail("");
		}
		catch (IllegalArgumentException expected) {
			assertThat(expected).hasMessage("No platforms configured");
		}
	}

	@Test
	void deployWithRegisteredApps() {
		AppRegistryService appRegistryService = mock(AppRegistryService.class);

		when(appRegistryService.appExist(eq("time"), eq(ApplicationType.source), eq("1.2.0.RELEASE")))
				.thenReturn(true);
		when(appRegistryService.appExist(eq("log"), eq(ApplicationType.sink), eq("1.2.0.RELEASE")))
				.thenReturn(true);

		testAppRegisteredOnStreamDeploy(appRegistryService);

		verify(appRegistryService, times(1)).appExist(eq("time"), eq(ApplicationType.source), eq("1.2.0.RELEASE"));
		verify(appRegistryService, times(1)).appExist(eq("log"), eq(ApplicationType.sink), eq("1.2.0.RELEASE"));
	}

	@Test
	void deployWithNotRegisteredApps() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			AppRegistryService appRegistryService = mock(AppRegistryService.class);

			when(appRegistryService.appExist(eq("time"), eq(ApplicationType.source), eq("1.2.0.RELEASE")))
					.thenReturn(true);
			when(appRegistryService.appExist(eq("log"), eq(ApplicationType.sink), eq("1.2.0.RELEASE")))
					.thenReturn(false);

			testAppRegisteredOnStreamDeploy(appRegistryService);
		});
	}

	private void testAppRegisteredOnStreamDeploy(AppRegistryService appRegistryService) {

		HashMap<String, String> timeAppProps = new HashMap<>();
		timeAppProps.put("spring.cloud.dataflow.stream.app.type", "source");
		AppDefinition timeAppDefinition = new AppDefinition("time", timeAppProps);
		MavenResource timeResource = new MavenResource.Builder()
				.artifactId("time-source-rabbit").groupId("org.springframework.cloud.stream.app")
				.version("1.2.0.RELEASE").build();
		when(appRegistryService.getResourceVersion(timeResource)).thenReturn(timeResource.getVersion());
		AppDeploymentRequest timeAppDeploymentRequest = new AppDeploymentRequest(timeAppDefinition, timeResource);

		HashMap<String, String> logAppProps = new HashMap<>();
		logAppProps.put("spring.cloud.dataflow.stream.app.type", "sink");
		AppDefinition logAppDefinition = new AppDefinition("log", logAppProps);
		MavenResource logResource = new MavenResource.Builder()
				.artifactId("log-sink-rabbit").groupId("org.springframework.cloud.stream.app")
				.version("1.2.0.RELEASE").build();
		when(appRegistryService.getResourceVersion(logResource)).thenReturn(logResource.getVersion());
		AppDeploymentRequest logAppDeploymentRequest = new AppDeploymentRequest(logAppDefinition, logResource);

		List<AppDeploymentRequest> appDeploymentRequests = Arrays.asList(logAppDeploymentRequest, timeAppDeploymentRequest);

		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_NAME, "package1");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PACKAGE_VERSION, "1.0.1");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_PLATFORM_NAME, "testPlatform");
		skipperDeployerProperties.put(SkipperStream.SKIPPER_REPO_NAME, "mylocal-repo1");

		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				appDeploymentRequests, skipperDeployerProperties);

		SkipperClient skipperClient = MockUtils.createSkipperClientMock();

		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());

		when(streamDefinitionRepository.findById("test1")).thenReturn(Optional.of(new StreamDefinition("test1", "t1: time | log")));

		skipperStreamDeployer.deployStream(streamDeploymentRequest);
	}

	@Test
	void stateOfUndefinedUndeployedStream() {

		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());

		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");

		// Stream is defined
		when(streamDefinitionRepository.findById(streamDefinition.getName())).thenReturn(Optional.ofNullable(null));

		// Stream is undeployed
		when(skipperClient.status(eq(streamDefinition.getName()))).thenThrow(new ReleaseNotFoundException(""));

		Map<StreamDefinition, DeploymentState> state = skipperStreamDeployer.streamsStates(Arrays.asList(streamDefinition));

		assertThat(state).isNotNull();
		assertThat(state).hasSize(1);
		assertThat(state.get(streamDefinition).equals(DeploymentState.undeployed));
	}

	@Test
	void nullCheckOnDeserializeAppStatus() {
		List<AppStatus> appStatusList = SkipperStreamDeployer.deserializeAppStatus(null);
		assertThat(appStatusList).isNotNull();
		assertThat(appStatusList).isEmpty();

		appStatusList = SkipperStreamDeployer.deserializeAppStatus("blah");
		assertThat(appStatusList).isNotNull();
		assertThat(appStatusList).isEmpty();
	}

	@Test
	void stateOfUndeployedStream() {

		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());

		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");

		// Stream is undeployed
		Info info = createInfo(StatusCode.DELETED);
		Map<String, Info> mockInfo = new HashMap<>();
		mockInfo.put("foo", info);
		when(skipperClient.status(eq(streamDefinition.getName()))).thenReturn(info);
		when(skipperClient.statuses(any())).thenReturn(mockInfo);

		Map<StreamDefinition, DeploymentState> state = skipperStreamDeployer.streamsStates(Arrays.asList(streamDefinition));
		assertThat(state).isNotNull();
		assertThat(state).hasSize(1);
		assertThat(state.get(streamDefinition).equals(DeploymentState.undeployed));

		// Stream is in failed state
		info = createInfo(StatusCode.FAILED);
		when(skipperClient.status(eq(streamDefinition.getName()))).thenReturn(info);
		mockInfo = new HashMap<>();
		mockInfo.put("foo", info);

		state = skipperStreamDeployer.streamsStates(Arrays.asList(streamDefinition));
		assertThat(state).isNotNull();
		assertThat(state).hasSize(1);
		assertThat(state.get(streamDefinition).equals(DeploymentState.failed));

		// Stream is deployed (rare case if ever...)
		info = createInfo(StatusCode.DEPLOYED);
		mockInfo = new HashMap<>();
		mockInfo.put("foo", info);

		when(skipperClient.status(eq(streamDefinition.getName()))).thenReturn(info);

		state = skipperStreamDeployer.streamsStates(Arrays.asList(streamDefinition));
		assertThat(state).isNotNull();
		assertThat(state).hasSize(1);
		assertThat(state.get(streamDefinition).equals(DeploymentState.deployed));

		// Stream is in unknown state
		info = createInfo(StatusCode.UNKNOWN);
		mockInfo = new HashMap<>();
		mockInfo.put("foo", info);

		when(skipperClient.status(eq(streamDefinition.getName()))).thenReturn(info);

		state = skipperStreamDeployer.streamsStates(Arrays.asList(streamDefinition));
		assertThat(state).isNotNull();
		assertThat(state).hasSize(1);
		assertThat(state.get(streamDefinition).equals(DeploymentState.unknown));

	}

	@Test
	void streamDeployWithLongAppName() {

		AppRegistryService appRegistryService = mock(AppRegistryService.class);

		when(appRegistryService.appExist(eq("time"), eq(ApplicationType.source), eq("1.2.0.RELEASE")))
				.thenReturn(true);
		when(appRegistryService.appExist(eq("log"), eq(ApplicationType.sink), eq("1.2.0.RELEASE")))
				.thenReturn(true);

		AppDefinition timeAppDefinition = new AppDefinition("time", new HashMap<>());
		MavenResource timeResource = new MavenResource.Builder()
				.artifactId("time-source-rabbit").groupId("org.springframework.cloud.stream.app")
				.version("1.2.0.RELEASE").build();
		when(appRegistryService.getResourceVersion(timeResource)).thenReturn(timeResource.getVersion());
		AppDeploymentRequest timeAppDeploymentRequest = new AppDeploymentRequest(timeAppDefinition, timeResource);

		List<AppDeploymentRequest> appDeploymentRequests = Arrays.asList(timeAppDeploymentRequest);

		String streamName = "asdfkdunfdnereerejrerkjelkraerkldjkfdjfkdsjflkjdflkdjflsdflsdjfldlfdlsfjdlfjdlfjdslfdnmdfndfmdsfmndsdfafdsfmdnfdske";

		String streamDSL = "time | log";

		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest(streamName, streamDSL,
								appDeploymentRequests,
				new HashMap<>());

		SkipperClient skipperClient = MockUtils.createSkipperClientMock();

		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());

		when(streamDefinitionRepository.findById(streamName)).thenReturn(Optional.of(new StreamDefinition(streamName, streamDSL)));
		try {
			skipperStreamDeployer.deployStream(streamDeploymentRequest);
			fail("Expected InvalidStreamDefinitionException");
		}
		catch (Exception e) {
			assertThat(e instanceof InvalidStreamDefinitionException).isTrue();
			assertThat(e.getMessage().equals("The runtime application name for the app time in the stream "+streamName+" should not exceed 63 in length. Currently it is: "+streamName+"-time-v{version-2digits}"));
		}
	}

	private Info createInfo(StatusCode statusCode) {
		Info info = new Info();
		Status status = new Status();
		status.setStatusCode(statusCode);
		status.setPlatformStatus(null);
		info.setStatus(status);
		return info;
	}

	@Test
	void testGetStreamStatuses() throws IOException {

		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());


		String platformStatus = StreamUtils.copyToString(
				new DefaultResourceLoader().getResource("classpath:/app-instance-state.json").getInputStream(),
				Charset.forName("UTF-8"));
		new DefaultResourceLoader().getResource("classpath:/app-instance-state.json");

		Info info = new Info();
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		status.setPlatformStatus(platformStatus);
		info.setStatus(status);

		when(skipperClient.status(eq("stream1"))).thenReturn(info);

		List<AppStatus> appStatues = skipperStreamDeployer.getStreamStatuses("stream1");
		assertThat(appStatues).isNotNull();
		assertThat(appStatues).hasSize(4);
	}

	@Test
	void stateOfDefinedUndeployedStream() {

		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());

		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");

		// Stream is defined
		when(streamDefinitionRepository.findById(streamDefinition.getName())).thenReturn(Optional.of(streamDefinition));

		// Stream is undeployed
		when(skipperClient.status(eq(streamDefinition.getName()))).thenThrow(new ReleaseNotFoundException(""));

		Map<StreamDefinition, DeploymentState> state = skipperStreamDeployer.streamsStates(Arrays.asList(streamDefinition));

		assertThat(state).isNotNull();
		assertThat(state).hasSize(1);
		assertThat(state).containsKeys(streamDefinition);
		assertThat(state).containsEntry(streamDefinition, DeploymentState.undeployed);
	}

	@Test
	@SuppressWarnings("unchecked")
	void undeployPackageAndReleaseExistAllGood() {
		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());
		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName("foo");
		when(skipperClient.search(eq(streamDefinition.getName()), eq(false)))
				.thenReturn(Arrays.asList(packageMetadata));

		skipperStreamDeployer.undeployStream(streamDefinition.getName());

		verify(skipperClient).search("foo", false);
		verify(skipperClient, times(1)).delete(eq(streamDefinition.getName()), eq(true));
		verifyNoMoreInteractions(skipperClient);
	}

	@Test
	@SuppressWarnings("unchecked")
	void undeployPackageExistsWithoutReleaseStillDeletesPackage() {
		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());
		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName("foo");
		when(skipperClient.search(eq(streamDefinition.getName()), eq(false)))
				.thenReturn(Arrays.asList(packageMetadata));
		ReleaseNotFoundException noReleaseEx = new ReleaseNotFoundException("foo");
		doThrow(noReleaseEx).when(skipperClient).delete("foo", true);

		skipperStreamDeployer.undeployStream(streamDefinition.getName());

		verify(skipperClient).search("foo", false);
		verify(skipperClient, times(1)).delete(eq(streamDefinition.getName()), eq(true));
		verify(skipperClient, times(1)).packageDelete("foo");
		verify(skipperClient, times(0)).delete(eq(streamDefinition.getName()), eq(false));
	}

	@Test
	@SuppressWarnings("unchecked")
	void undeployPackageDoesNotExistSkipsDelete() {
		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class), new DefaultStreamDefinitionService());
		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName("foobar");
		when(skipperClient.search(eq(streamDefinition.getName()), eq(false))).thenReturn(Arrays.asList(packageMetadata));

		skipperStreamDeployer.undeployStream(streamDefinition.getName());

		verify(skipperClient).search("foo", false);
		verifyNoMoreInteractions(skipperClient);
	}

	@Test
	void manifestWithRelease() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class)
				, new DefaultStreamDefinitionService());

		skipperStreamDeployer.manifest("name", 666);
		verify(skipperClient).manifest(eq("name"), eq(666));

		skipperStreamDeployer.manifest("name", -1);
		verify(skipperClient).manifest(eq("name"));
	}

	@Test
	void testManifest() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class)
				, new DefaultStreamDefinitionService());

		skipperStreamDeployer.manifest("name");
		verify(skipperClient).manifest(eq("name"));
	}

	@Test
	void testPlatformList() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		when(skipperClient.listDeployers()).thenReturn(new ArrayList<>());
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class),
				new DefaultStreamDefinitionService());
		skipperStreamDeployer.platformList();
		verify(skipperClient, times(1)).listDeployers();
	}

	@Test
	void testHistory() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		when(skipperClient.history(eq("release1"))).thenReturn(new ArrayList<>());
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class),
				new DefaultStreamDefinitionService());
		skipperStreamDeployer.history("release1");
		verify(skipperClient, times(1)).history(eq("release1"));
	}

	@Test
	void testRollback() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class),
				new DefaultStreamDefinitionService());
		skipperStreamDeployer.rollbackStream("release666", 666);
		ArgumentCaptor<RollbackRequest> rollbackRequestCaptor = ArgumentCaptor.forClass(RollbackRequest.class);
		verify(skipperClient).rollback(rollbackRequestCaptor.capture());
		assertThat(rollbackRequestCaptor.getValue().getReleaseName()).isEqualTo("release666");
		assertThat(rollbackRequestCaptor.getValue().getVersion()).isEqualTo(666);
	}

	@Test
	void getLogByReleaseName() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		when(skipperClient.getLog(eq("release1"))).thenReturn(new LogInfo(Collections.EMPTY_MAP));
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class),
				new DefaultStreamDefinitionService());
		skipperStreamDeployer.getLog("release1");
		verify(skipperClient, times(1)).getLog(eq("release1"));
	}

	@Test
	void getLogByReleaseNameAndAppName() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		when(skipperClient.getLog(eq("release1"), eq("myapp"))).thenReturn(new LogInfo(Collections.EMPTY_MAP));
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class),
				new DefaultStreamDefinitionService());
		skipperStreamDeployer.getLog("release1", "myapp");
		verify(skipperClient, times(1)).getLog(eq("release1"), eq("myapp"));
	}

	@Test
	void testEnvironmentInfo() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		AboutResource about = new AboutResource();
		about.setVersionInfo(new VersionInfo());
		about.getVersionInfo().setServer(new Dependency("d1", "v1", "check", "check2", "url"));
		when(skipperClient.info()).thenReturn(about);
		when(skipperClient.listDeployers()).thenReturn(Arrays.asList(new Deployer("d1", "t1", null, null)));

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class),
				new DefaultStreamDefinitionService());

		RuntimeEnvironmentInfo info = skipperStreamDeployer.environmentInfo();

		assertThat(info.getImplementationName()).isEqualTo("d1");
		assertThat(info.getImplementationVersion()).isEqualTo("v1");
		assertThat(info.getPlatformType()).isEqualTo("Skipper Managed");

		verify(skipperClient).info();
		verify(skipperClient).listDeployers();
	}

}
