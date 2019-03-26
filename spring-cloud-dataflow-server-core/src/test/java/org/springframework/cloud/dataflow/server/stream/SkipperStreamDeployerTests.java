/*
 * Copyright 2017-2018 the original author or authors.
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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.support.ResourceUtils;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.hateoas.Resources;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_NAME;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_VERSION;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PLATFORM_NAME;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_REPO_NAME;
import static org.springframework.cloud.dataflow.server.support.MockUtils.createSkipperClientMock;

/**
 * @author Mark Pollack
 * @author Soby Chacko
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
public class SkipperStreamDeployerTests {

	@Test
	public void testMavenResourceProcessing() {
		MavenResource mavenResource = new MavenResource.Builder()
				.artifactId("timestamp-task")
				.groupId("org.springframework.cloud.task.app")
				.version("1.0.0.RELEASE")
				.build();
		String resourceWithoutVersion = ResourceUtils.getResourceWithoutVersion(mavenResource);
		assertThat(resourceWithoutVersion).isEqualTo("maven://org.springframework.cloud.task.app:timestamp-task");
		assertThat(ResourceUtils.getResourceVersion(mavenResource)).isEqualTo("1.0.0.RELEASE");
	}

	@Test
	public void testDockerResourceProcessing() {
		DockerResource dockerResource = new DockerResource("springcloudstream/file-source-kafka-10:1.2.0.RELEASE");
		assertThat(ResourceUtils.getResourceWithoutVersion(dockerResource)).isEqualTo("docker:springcloudstream/file-source-kafka-10");
		assertThat(ResourceUtils.getResourceVersion(dockerResource)).isEqualTo("1.2.0.RELEASE");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidDockerResourceProcessing() {
		DockerResource dockerResource = new DockerResource("springcloudstream:file-source-kafka-10:1.2.0.RELEASE");
		ResourceUtils.getResourceWithoutVersion(dockerResource);
	}

	@Test
	public void testInstallUploadProperties() {
		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SKIPPER_PACKAGE_NAME, "package1");
		skipperDeployerProperties.put(SKIPPER_PACKAGE_VERSION, "1.0.1");
		skipperDeployerProperties.put(SKIPPER_PLATFORM_NAME, "testPlatform");
		skipperDeployerProperties.put(SKIPPER_REPO_NAME, "mylocal-repo1");
		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				new ArrayList<>(),
				skipperDeployerProperties);

		SkipperClient skipperClient = createSkipperClientMock();

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class));
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
	public void testInvalidPlatformName() {
		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SKIPPER_PACKAGE_NAME, "package1");
		skipperDeployerProperties.put(SKIPPER_PACKAGE_VERSION, "1.0.1");
		skipperDeployerProperties.put(SKIPPER_PLATFORM_NAME, "badPlatform");
		skipperDeployerProperties.put(SKIPPER_REPO_NAME, "mylocal-repo1");
		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				new ArrayList<>(),
				skipperDeployerProperties);

		SkipperClient skipperClient = createSkipperClientMock();

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class));
		try {
			skipperStreamDeployer.deployStream(streamDeploymentRequest);
			fail();
		} catch (IllegalArgumentException expected) {
			assertThat(expected).hasMessage("No platform named 'badPlatform'");
		}
	}

	@Test
	public void testNoPlatforms() {
		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SKIPPER_PACKAGE_NAME, "package1");
		skipperDeployerProperties.put(SKIPPER_PACKAGE_VERSION, "1.0.1");
		skipperDeployerProperties.put(SKIPPER_REPO_NAME, "mylocal-repo1");
		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				new ArrayList<>(),
				skipperDeployerProperties);

		SkipperClient skipperClient = mock(SkipperClient.class);
		when(skipperClient.listDeployers()).thenReturn(new Resources<>(new ArrayList<>(), new ArrayList<>()));

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class));
		try {
			skipperStreamDeployer.deployStream(streamDeploymentRequest);
			fail();
		} catch (IllegalArgumentException expected) {
			assertThat(expected).hasMessage("No platforms configured");
		}
	}

	@Test
	public void testDeployeWithRegisteredApps() {
		AppRegistryService appRegistryService = mock(AppRegistryService.class);

		when(appRegistryService.appExist(eq("time"), eq(ApplicationType.source), eq("1.2.0.RELEASE")))
				.thenReturn(true);
		when(appRegistryService.appExist(eq("log"), eq(ApplicationType.sink), eq("1.2.0.RELEASE")))
				.thenReturn(true);

		testAppRegisteredOnStreamDeploy(appRegistryService);

		verify(appRegistryService, times(1)).appExist(eq("time"), eq(ApplicationType.source), eq("1.2.0.RELEASE"));
		verify(appRegistryService, times(1)).appExist(eq("log"), eq(ApplicationType.sink), eq("1.2.0.RELEASE"));
	}

	@Test(expected = IllegalStateException.class)
	public void testDeployeWithNotRegisteredApps() {
		AppRegistryService appRegistryService = mock(AppRegistryService.class);

		when(appRegistryService.appExist(eq("time"), eq(ApplicationType.source), eq("1.2.0.RELEASE")))
				.thenReturn(true);
		when(appRegistryService.appExist(eq("log"), eq(ApplicationType.sink), eq("1.2.0.RELEASE")))
				.thenReturn(false);

		testAppRegisteredOnStreamDeploy(appRegistryService);
	}

	private void testAppRegisteredOnStreamDeploy(AppRegistryService appRegistryService) {

		HashMap<String, String> timeAppProps = new HashMap<>();
		timeAppProps.put("spring.cloud.dataflow.stream.app.type", "source");
		AppDefinition timeAppDefinition = new AppDefinition("time", timeAppProps);
		MavenResource timeResource = new MavenResource.Builder()
				.artifactId("time-source-rabbit").groupId("org.springframework.cloud.stream.app")
				.version("1.2.0.RELEASE").build();
		AppDeploymentRequest timeAppDeploymentRequest = new AppDeploymentRequest(timeAppDefinition, timeResource);

		HashMap<String, String> logAppProps = new HashMap<>();
		logAppProps.put("spring.cloud.dataflow.stream.app.type", "sink");
		AppDefinition logAppDefinition = new AppDefinition("log", logAppProps);
		MavenResource logResource = new MavenResource.Builder()
				.artifactId("log-sink-rabbit").groupId("org.springframework.cloud.stream.app")
				.version("1.2.0.RELEASE").build();
		AppDeploymentRequest logAppDeploymentRequest = new AppDeploymentRequest(logAppDefinition, logResource);

		List<AppDeploymentRequest> appDeploymentRequests = Arrays.asList(logAppDeploymentRequest, timeAppDeploymentRequest);

		Map<String, String> skipperDeployerProperties = new HashMap<>();
		skipperDeployerProperties.put(SKIPPER_PACKAGE_NAME, "package1");
		skipperDeployerProperties.put(SKIPPER_PACKAGE_VERSION, "1.0.1");
		skipperDeployerProperties.put(SKIPPER_PLATFORM_NAME, "testPlatform");
		skipperDeployerProperties.put(SKIPPER_REPO_NAME, "mylocal-repo1");

		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				appDeploymentRequests, skipperDeployerProperties);

		SkipperClient skipperClient = createSkipperClientMock();

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class),
				appRegistryService, mock(ForkJoinPool.class));

		skipperStreamDeployer.deployStream(streamDeploymentRequest);
	}



	@Test
	public void testStateOfUndefinedUndeployedStream() {

		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class));

		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");

		// Stream is defined
		when(streamDefinitionRepository.findOne(streamDefinition.getName())).thenReturn(null);

		// Stream is undeployed
		when(skipperClient.status(eq(streamDefinition.getName()))).thenThrow(new ReleaseNotFoundException(""));

		Map<StreamDefinition, DeploymentState> state = skipperStreamDeployer.state(Arrays.asList(streamDefinition));

		assertThat(state).isNotNull();
		assertThat(state.size()).isEqualTo(0);
	}

	@Test
	public void testStateOfDefinedUndeployedStream() {

		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class));

		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");

		// Stream is defined
		when(streamDefinitionRepository.findOne(streamDefinition.getName())).thenReturn(streamDefinition);

		// Stream is undeployed
		when(skipperClient.status(eq(streamDefinition.getName()))).thenThrow(new ReleaseNotFoundException(""));

		Map<StreamDefinition, DeploymentState> state = skipperStreamDeployer.state(Arrays.asList(streamDefinition));

		assertThat(state).isNotNull();
		assertThat(state.size()).isEqualTo(1);
		assertThat(state).containsKeys(streamDefinition);
		assertThat(state.get(streamDefinition)).isEqualTo(DeploymentState.undeployed);
	}

	@Test
	public void testUndepoySkippedForUndefinedStream() {
		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class));

		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");

		when(skipperClient.status(eq(streamDefinition.getName()))).thenThrow(new ReleaseNotFoundException(""));

		skipperStreamDeployer.undeployStream(streamDefinition.getName());

		verify(skipperClient, times(0)).delete(eq(streamDefinition.getName()), eq(true));
	}

	@Test
	public void testUndepoyForDefinedStream() {
		AppRegistryService appRegistryService = mock(AppRegistryService.class);
		SkipperClient skipperClient = mock(SkipperClient.class);
		StreamDefinitionRepository streamDefinitionRepository = mock(StreamDefinitionRepository.class);

		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				streamDefinitionRepository, appRegistryService, mock(ForkJoinPool.class));

		StreamDefinition streamDefinition = new StreamDefinition("foo", "foo|bar");

		Info info = new Info();
		info.setStatus(new Status());
		AppStatus fooAppStatus = AppStatus.of("foo").generalState(DeploymentState.deployed).build();
		AppStatus barAppStatus = AppStatus.of("bar").generalState(DeploymentState.deployed).build();
		info.getStatus().setPlatformStatusAsAppStatusList(Arrays.asList(fooAppStatus, barAppStatus));
		when(skipperClient.status(eq(streamDefinition.getName()))).thenReturn(info);

		skipperStreamDeployer.undeployStream(streamDefinition.getName());
		verify(skipperClient, times(1)).delete(eq(streamDefinition.getName()), eq(true));
	}

	@Test
	public void testFileResourceProcessing() throws MalformedURLException {
		Resource resource = new UrlResource("file:/springcloudstream/file-source-kafka-10-1.2.0.RELEASE.jar");
		assertThat(ResourceUtils.getResourceWithoutVersion(resource)).isEqualTo("file:/springcloudstream/file-source-kafka-10");
		assertThat(ResourceUtils.getResourceVersion(resource)).isEqualTo("1.2.0.RELEASE");

		resource = new UrlResource("file:/springcloudstream/file-source-kafka-10-1.2.0.BUILD-SNAPSHOT.jar");
		assertThat(ResourceUtils.getResourceWithoutVersion(resource)).isEqualTo("file:/springcloudstream/file-source-kafka-10");
		assertThat(ResourceUtils.getResourceVersion(resource)).isEqualTo("1.2.0.BUILD-SNAPSHOT");

		resource = new UrlResource("https://springcloudstream/file-source-kafka-10-1.2.0.RELEASE.jar");
		assertThat(ResourceUtils.getResourceWithoutVersion(resource)).isEqualTo("http://springcloudstream/file-source-kafka-10");
		assertThat(ResourceUtils.getResourceVersion(resource)).isEqualTo("1.2.0.RELEASE");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFileResourceWithoutVersion() throws MalformedURLException {
		Resource resource = new UrlResource("https://springcloudstream/filesourcekafkacrap.jar");
		assertThat(ResourceUtils.getResourceWithoutVersion(resource)).isEqualTo("https://springcloudstream/filesourcekafkacrap.jar");
	}

	@Test
	public void testManifest() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class));

		skipperStreamDeployer.manifest("name", 666);
		verify(skipperClient).manifest(eq("name"), eq(666));

		skipperStreamDeployer.manifest("name");
		verify(skipperClient).manifest(eq("name"));
	}

	@Test
	public void testPlatformList() {
		SkipperClient skipperClient = mock(SkipperClient.class);
		when(skipperClient.listDeployers()).thenReturn(new Resources<>(new ArrayList<>(), new ArrayList<>()));
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient,
				mock(StreamDefinitionRepository.class), mock(AppRegistryService.class), mock(ForkJoinPool.class));
		skipperStreamDeployer.platformList();
		verify(skipperClient, times(1)).listDeployers();
	}

}
