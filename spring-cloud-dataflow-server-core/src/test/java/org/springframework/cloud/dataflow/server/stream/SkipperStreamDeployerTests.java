/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.stream;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.support.ResourceUtils;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_NAME;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_VERSION;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PLATFORM_NAME;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_REPO_NAME;

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
		skipperDeployerProperties.put(SKIPPER_PLATFORM_NAME, "platform1");
		skipperDeployerProperties.put(SKIPPER_REPO_NAME, "mylocal-repo1");
		StreamDeploymentRequest streamDeploymentRequest = new StreamDeploymentRequest("test1", "time | log",
				new ArrayList<>(),
				skipperDeployerProperties);
		SkipperClient skipperClient = mock(SkipperClient.class);
		SkipperStreamDeployer skipperStreamDeployer = new SkipperStreamDeployer(skipperClient, mock
				(StreamDeploymentRepository.class));
		skipperStreamDeployer.deployStream(streamDeploymentRequest);
		ArgumentCaptor<UploadRequest> uploadRequestCaptor = ArgumentCaptor.forClass(UploadRequest.class);
		ArgumentCaptor<InstallRequest> installRequestCaptor = ArgumentCaptor.forClass(InstallRequest.class);
		verify(skipperClient).install(installRequestCaptor.capture());
		assertThat(installRequestCaptor.getValue().getPackageIdentifier()).isNotNull();
		assertThat(installRequestCaptor.getValue().getPackageIdentifier().getPackageName()).isEqualTo("package1");
		assertThat(installRequestCaptor.getValue().getPackageIdentifier().getPackageVersion()).isEqualTo("1.0.1");
		assertThat(installRequestCaptor.getValue().getPackageIdentifier().getRepositoryName()).isEqualTo("mylocal-repo1");
		assertThat(installRequestCaptor.getValue().getInstallProperties().getPlatformName()).isEqualTo("platform1");
		verify(skipperClient).upload(uploadRequestCaptor.capture());
		assertThat(uploadRequestCaptor.getValue()).isNotNull();
		assertThat(uploadRequestCaptor.getValue().getName()).isEqualTo("package1");
		assertThat(uploadRequestCaptor.getValue().getVersion()).isEqualTo("1.0.1");
		assertThat(installRequestCaptor.getValue().getPackageIdentifier().getRepositoryName()).isEqualTo("mylocal-repo1");
		assertThat(installRequestCaptor.getValue().getInstallProperties().getPlatformName()).isEqualTo("platform1");
	}

	@Test
	public void testFileResourceProcessing() throws MalformedURLException {
		Resource resource = new UrlResource("file:/springcloudstream/file-source-kafka-10-1.2.0.RELEASE.jar");
		assertThat(ResourceUtils.getResourceWithoutVersion(resource)).isEqualTo("file:/springcloudstream/file-source-kafka-10");
		assertThat(ResourceUtils.getResourceVersion(resource)).isEqualTo("1.2.0.RELEASE");

		resource = new UrlResource("file:/springcloudstream/file-source-kafka-10-1.2.0.BUILD-SNAPSHOT.jar");
		assertThat(ResourceUtils.getResourceWithoutVersion(resource)).isEqualTo("file:/springcloudstream/file-source-kafka-10");
		assertThat(ResourceUtils.getResourceVersion(resource)).isEqualTo("1.2.0.BUILD-SNAPSHOT");

		resource = new UrlResource("http://springcloudstream/file-source-kafka-10-1.2.0.RELEASE.jar");
		assertThat(ResourceUtils.getResourceWithoutVersion(resource)).isEqualTo("http://springcloudstream/file-source-kafka-10");
		assertThat(ResourceUtils.getResourceVersion(resource)).isEqualTo("1.2.0.RELEASE");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFileResourceWithoutVersion() throws MalformedURLException {
		Resource resource = new UrlResource("http://springcloudstream/filesourcekafkacrap.jar");
		assertThat(ResourceUtils.getResourceWithoutVersion(resource)).isEqualTo("http://springcloudstream/filesourcekafkacrap.jar");
	}

}
