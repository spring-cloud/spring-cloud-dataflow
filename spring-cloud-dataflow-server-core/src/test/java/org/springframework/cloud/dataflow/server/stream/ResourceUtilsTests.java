/*
 * Copyright 2018 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Mark Pollack
 * @author Soby Chacko
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
class ResourceUtilsTests {

	private AppResourceCommon appResourceService = new AppResourceCommon(new MavenProperties(), null);

	@Test
	void mavenResourceProcessing() {
		MavenResource mavenResource = new MavenResource.Builder()
				.artifactId("timestamp-task")
				.groupId("org.springframework.cloud.task.app")
				.version("1.0.0.RELEASE")
				.build();
		String resourceWithoutVersion = appResourceService.getResourceWithoutVersion(mavenResource);
		assertThat(resourceWithoutVersion).isEqualTo("maven://org.springframework.cloud.task.app:timestamp-task:jar");
		assertThat(appResourceService.getResourceVersion(mavenResource)).isEqualTo("1.0.0.RELEASE");
	}

	@Test
	void dockerResourceProcessing() {
		DockerResource dockerResource = new DockerResource("springcloudstream/file-source-kafka-10:1.2.0.RELEASE");
		assertThat(appResourceService.getResourceWithoutVersion(dockerResource)).isEqualTo("docker:springcloudstream/file-source-kafka-10");
		assertThat(appResourceService.getResourceVersion(dockerResource)).isEqualTo("1.2.0.RELEASE");
	}

	@Test
	void dockerResourceProcessingWithHostIP() {
		DockerResource dockerResource = new DockerResource("192.168.99.100:80/myrepo/rabbitsink:current");
		assertThat(appResourceService.getResourceWithoutVersion(dockerResource)).isEqualTo("docker:192.168.99.100:80/myrepo/rabbitsink");
		assertThat(appResourceService.getResourceVersion(dockerResource)).isEqualTo("current");
	}

	@Test
	void invalidDockerResourceProcessing() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			DockerResource dockerResource = new DockerResource("springcloudstream:file-source-kafka-10:1.2.0.RELEASE");
			appResourceService.getResourceWithoutVersion(dockerResource);
		});
	}

	@Test
	void fileResourceProcessing() throws MalformedURLException {
		Resource resource = new UrlResource("file:/springcloudstream/file-source-kafka-10-1.2.0.RELEASE.jar");
		assertThat(appResourceService.getResourceWithoutVersion(resource)).isEqualTo("file:/springcloudstream/file-source-kafka-10");
		assertThat(appResourceService.getResourceVersion(resource)).isEqualTo("1.2.0.RELEASE");

		resource = new UrlResource("file:/springcloudstream/file-source-kafka-10-1.2.0.BUILD-SNAPSHOT.jar");
		assertThat(appResourceService.getResourceWithoutVersion(resource)).isEqualTo("file:/springcloudstream/file-source-kafka-10");
		assertThat(appResourceService.getResourceVersion(resource)).isEqualTo("1.2.0.BUILD-SNAPSHOT");

		resource = new UrlResource("https://springcloudstream/file-source-kafka-10-1.2.0.RELEASE.jar");
		assertThat(appResourceService.getResourceWithoutVersion(resource)).isEqualTo("https://springcloudstream/file-source-kafka-10");
		assertThat(appResourceService.getResourceVersion(resource)).isEqualTo("1.2.0.RELEASE");
	}

	@Test
	void fileResourceWithoutVersion() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			Resource resource = new UrlResource("https://springcloudstream/filesourcekafkacrap.jar");
			assertThat(appResourceService.getResourceWithoutVersion(resource)).isEqualTo("https://springcloudstream/filesourcekafkacrap.jar");
		});
	}
}
