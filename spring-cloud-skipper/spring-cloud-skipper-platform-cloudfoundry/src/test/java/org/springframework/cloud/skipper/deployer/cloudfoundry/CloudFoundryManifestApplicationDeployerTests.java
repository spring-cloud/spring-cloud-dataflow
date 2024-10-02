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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CloudFoundryManifestApplicationDeployerTests {

	@Test
	void getResourceLocation() {
		SpringCloudDeployerApplicationSpec springBootAppSpec1 = mock(SpringCloudDeployerApplicationSpec.class);
		String mavenSpecResource = "maven://org.springframework.cloud.stream.app:log-sink-rabbit";
		String mavenSpecVersion = "1.2.0.RELEASE";
		when(springBootAppSpec1.getResource()).thenReturn(mavenSpecResource);
		when(springBootAppSpec1.getVersion()).thenReturn(mavenSpecVersion);
		SpringCloudDeployerApplicationSpec springBootAppSpec2 = mock(SpringCloudDeployerApplicationSpec.class);
		String dockerSpecResource = "docker:springcloudstream/log-sink-rabbit";
		String dockerSpecVersion = "1.2.0.RELEASE";
		when(springBootAppSpec2.getResource()).thenReturn(dockerSpecResource);
		when(springBootAppSpec2.getVersion()).thenReturn(dockerSpecVersion);
		SpringCloudDeployerApplicationSpec springBootAppSpec3 = mock(SpringCloudDeployerApplicationSpec.class);
		String httpSpecResource = "https://repo.maven.apache.org/maven2/org/springframework/cloud/stream/app/"
				+ "log-sink-rabbit/1.2.0.RELEASE/log-sink-rabbit";
		when(springBootAppSpec3.getResource()).thenReturn(httpSpecResource);
		when(springBootAppSpec3.getVersion()).thenReturn("1.2.0.RELEASE");
		assertThat(CloudFoundryManifestApplicationDeployer.getResourceLocation(springBootAppSpec1.getResource(), springBootAppSpec1.getVersion()))
				.isEqualTo(String.format("%s:%s", mavenSpecResource, mavenSpecVersion));
		assertThat(CloudFoundryManifestApplicationDeployer.getResourceLocation(springBootAppSpec2.getResource(), springBootAppSpec2.getVersion()))
				.isEqualTo(String.format("%s:%s", dockerSpecResource, dockerSpecVersion));
		assertThat(CloudFoundryManifestApplicationDeployer.getResourceLocation(springBootAppSpec3.getResource(), springBootAppSpec3.getVersion()))
				.isEqualTo(httpSpecResource);
		SpringCloudDeployerApplicationSpec springBootAppSpec4 = mock(SpringCloudDeployerApplicationSpec.class);
		String mavenSpecResource2 = "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE";
		String mavenSpecVersion2 = "1.2.0.RELEASE";
		when(springBootAppSpec4.getResource()).thenReturn(mavenSpecResource2);
		when(springBootAppSpec4.getVersion()).thenReturn(mavenSpecVersion2);
		assertThat(CloudFoundryManifestApplicationDeployer.getResourceLocation(springBootAppSpec4.getResource(), springBootAppSpec4.getVersion()))
				.isEqualTo(mavenSpecResource2);
		String mavenSpecResource3 = "maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE";
		SpringCloudDeployerApplicationSpec springBootAppSpec5 = mock(SpringCloudDeployerApplicationSpec.class);
		when(springBootAppSpec5.getResource()).thenReturn(mavenSpecResource3);
		when(springBootAppSpec5.getVersion()).thenReturn(null);
		assertThat(CloudFoundryManifestApplicationDeployer.getResourceLocation(springBootAppSpec4.getResource(), springBootAppSpec4.getVersion()))
				.isEqualTo(mavenSpecResource3);
	}

}
