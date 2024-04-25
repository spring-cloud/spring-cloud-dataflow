/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.domain;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.TestResourceUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class SpringCloudDeployerApplicationManifestReaderTests {

	private final SpringCloudDeployerApplicationManifestReader applicationManifestReader = new SpringCloudDeployerApplicationManifestReader();

	@Test
	public void readTests() throws IOException {
		String manifestYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "manifest.yml").getInputStream(),
				Charset.defaultCharset());
		List<SpringCloudDeployerApplicationManifest> applicationSpecList = this.applicationManifestReader
				.read(manifestYaml);

		assertThat(applicationSpecList).hasSize(2);
		assertThat(applicationSpecList.get(0) instanceof SpringCloudDeployerApplicationManifest).isTrue();
		assertTimeOrLogApp(applicationSpecList.get(0));
		assertThat(applicationSpecList.get(1) instanceof SpringCloudDeployerApplicationManifest).isTrue();
		assertTimeOrLogApp(applicationSpecList.get(1));
	}

	@Test
	public void testNonMatchingManifestReader() throws IOException {
		String manifestYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "erroneous-manifest.yml").getInputStream(),
				Charset.defaultCharset());
		List<SpringCloudDeployerApplicationManifest> applicationSpecList = this.applicationManifestReader
				.read(manifestYaml);
		assertThat(applicationSpecList.isEmpty()).isTrue();
	}

	private void assertTimeOrLogApp(SpringCloudDeployerApplicationManifest applicationSpec) {
		if (applicationSpec.getMetadata().containsKey("name")) {
			String name = applicationSpec.getMetadata().get("name");
			if (name.equals("time-source")) {
				assertTime(applicationSpec);
			}
			else if (name.equals("log-sink")) {
				assertLog(applicationSpec);
			}
			else {
				fail("Unknown application name");
			}
		}
	}

	private void assertTime(SpringCloudDeployerApplicationManifest applicationSpec) {
		assertApiAndKind(applicationSpec);
		Map<String, String> metadata = applicationSpec.getMetadata();
		assertThat(metadata).containsEntry("name", "time-source")
				.containsEntry("count", "5")
				.containsEntry("type", "source");
		SpringCloudDeployerApplicationSpec spec = applicationSpec.getSpec();
		assertThat(spec.getResource())
				.isEqualTo("maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE");
		assertThat(spec.getResourceMetadata())
				.isEqualTo(
						"maven://org.springframework.cloud.stream.app:time-source-rabbit:jar:metadata:1.2.0.RELEASE");
		assertThat(spec.getApplicationProperties()).hasSize(1);
		assertThat(spec.getApplicationProperties()).containsEntry("log.level", "DEBUG");

		assertThat(spec.getDeploymentProperties()).hasSize(2);
		assertThat(spec.getDeploymentProperties()).containsEntry("memory", "2048");
		assertThat(spec.getDeploymentProperties()).containsEntry("disk", "4");

	}

	private void assertLog(SpringCloudDeployerApplicationManifest applicationSpec) {
		assertApiAndKind(applicationSpec);
		Map<String, String> metadata = applicationSpec.getMetadata();
		assertThat(metadata).containsEntry("name", "log-sink")
				.containsEntry("count", "2")
				.containsEntry("type", "sink");
		SpringCloudDeployerApplicationSpec spec = applicationSpec.getSpec();
		assertThat(spec.getResource())
				.isEqualTo("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE");
		assertThat(spec.getResourceMetadata())
				.isEqualTo("maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:1.2.0.RELEASE");
		assertThat(spec.getApplicationProperties()).hasSize(2);
		assertThat(spec.getApplicationProperties()).containsEntry("log.level", "INFO");
		assertThat(spec.getApplicationProperties()).containsEntry("log.expression", "hello baby");

		assertThat(spec.getDeploymentProperties()).hasSize(2);
		assertThat(spec.getDeploymentProperties()).containsEntry("memory", "1024");
		assertThat(spec.getDeploymentProperties()).containsEntry("disk", "2");
	}

	private void assertApiAndKind(SpringCloudDeployerApplicationManifest applicationManifest) {
		assertThat(applicationManifest.getApiVersion()).isEqualTo("skipper.spring.io/v1");
		assertThat(applicationManifest.getKind()).isIn("SpringCloudDeployerApplication", "SpringBootApp");
	}

}
