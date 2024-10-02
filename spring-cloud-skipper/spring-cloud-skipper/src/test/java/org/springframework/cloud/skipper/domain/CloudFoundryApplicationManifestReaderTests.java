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
package org.springframework.cloud.skipper.domain;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.TestResourceUtils;
import org.springframework.cloud.skipper.domain.CloudFoundryApplicationSpec.HealthCheckType;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CloudFoundryApplicationManifestReaderTests {

	private final CloudFoundryApplicationManifestReader applicationManifestReader = new CloudFoundryApplicationManifestReader();

	@Test
	void readTests() throws IOException {
		String manifestYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "manifest1.yml").getInputStream(),
				Charset.defaultCharset());
		List<CloudFoundryApplicationSkipperManifest> applicationSpecList = this.applicationManifestReader
				.read(manifestYaml);

		assertThat(applicationSpecList).hasSize(1);
		assertThat(applicationSpecList.get(0) instanceof CloudFoundryApplicationSkipperManifest).isTrue();
		CloudFoundryApplicationSkipperManifest m = applicationSpecList.get(0);
		assertThat(m.getSpec().getResource()).isEqualTo("https://github.com/my/entry");
		assertThat(m.getSpec().getVersion()).isEqualTo("1.0.0");
		assertThat(m.getSpec().getManifest().getBuildpack()).isEqualTo("buildpack");
		assertThat(m.getSpec().getManifest().getCommand()).isEqualTo("my command");
		assertThat(m.getSpec().getManifest().getDiskQuota()).isEqualTo("2048");
		assertThat(m.getSpec().getManifest().getDomains()).containsExactlyInAnyOrder("domain1", "domain2");
		assertThat(m.getSpec().getManifest().getEnv()).hasSize(2);
		assertThat(m.getSpec().getManifest().getEnv()).containsEntry("key1", "value1");
		assertThat(m.getSpec().getManifest().getEnv()).containsEntry("key2", "value2");
		assertThat(m.getSpec().getManifest().getHealthCheckType()).isEqualTo(HealthCheckType.process);
		assertThat(m.getSpec().getManifest().getHealthCheckHttpEndpoint()).isEqualTo("endpoint");
		assertThat(m.getSpec().getManifest().getHosts()).containsExactlyInAnyOrder("host1", "host2");
		assertThat(m.getSpec().getManifest().getInstances()).isEqualTo(1);
		assertThat(m.getSpec().getManifest().getMemory()).isEqualTo("1024");
		assertThat(m.getSpec().getManifest().getTimeout()).isEqualTo(180);
		assertThat(m.getSpec().getManifest().getNoHostname()).isFalse();
		assertThat(m.getSpec().getManifest().getNoRoute()).isFalse();
		assertThat(m.getSpec().getManifest().getRandomRoute()).isTrue();
		assertThat(m.getSpec().getManifest().getStack()).isEqualTo("stack");
		assertThat(m.getSpec().getManifest().getServices()).containsExactlyInAnyOrder("rabbit");
	}

	@Test
	void readListAlternativeFormat() throws IOException {
		String manifestYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "manifest2.yml").getInputStream(),
				Charset.defaultCharset());
		List<CloudFoundryApplicationSkipperManifest> applicationSpecList = this.applicationManifestReader
				.read(manifestYaml);

		assertThat(applicationSpecList).hasSize(1);
		assertThat(applicationSpecList.get(0) instanceof CloudFoundryApplicationSkipperManifest).isTrue();
		CloudFoundryApplicationSkipperManifest m = applicationSpecList.get(0);
		assertThat(m.getSpec().getResource()).isEqualTo("https://github.com/my/entry");
		assertThat(m.getSpec().getVersion()).isEqualTo("1.0.0");
		assertThat(m.getSpec().getManifest().getServices()).containsExactlyInAnyOrder("rabbit");
	}
}
