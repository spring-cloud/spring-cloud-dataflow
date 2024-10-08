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

import java.util.Map;

import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudFoundryApplicationManifestUtilsTests {

	@Test
	void manifestMap() {
		ApplicationManifest manifest = ApplicationManifest.builder()
				.name("name")
				.buildpack("buildpack")
				.command("command")
				.disk(1024)
				.domain("domain")
				.environmentVariable("key", "value")
				.healthCheckHttpEndpoint("endpoint")
				.healthCheckType(ApplicationHealthCheck.PROCESS)
				.host("host")
				.instances(2)
				.memory(1024)
				.noHostname(true)
				.noRoute(true)
				.randomRoute(true)
				.services("service1", "service2")
				.stack("stack")
				.timeout(100)
				.build();

		Map<String, String> map = CloudFoundryApplicationManifestUtils.getCFManifestMap(manifest);
		assertThat(map).hasSize(18);

		manifest = ApplicationManifest.builder()
				.name("name")
				.buildpack("buildpack")
				.command("command --foobar=x,y")
				.disk(1024)
				.domain("domain")
				.environmentVariable("key", "value")
				.healthCheckHttpEndpoint("endpoint")
				.healthCheckType(ApplicationHealthCheck.PROCESS)
				.host("host")
				.instances(2)
				.memory(1024)
				.noHostname(true)
				.noRoute(true)
				.randomRoute(true)
				.services("service1")
				.stack("stack")
				.timeout(100)
				.build();

		map = CloudFoundryApplicationManifestUtils.getCFManifestMap(manifest);
		assertThat(map).hasSize(17);
	}
}
