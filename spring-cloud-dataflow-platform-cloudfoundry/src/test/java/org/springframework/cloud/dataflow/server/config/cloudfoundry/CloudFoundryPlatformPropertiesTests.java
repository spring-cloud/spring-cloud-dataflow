/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.cloudfoundry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Donovan Muller
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = CloudFoundryPlatformPropertiesTests.TestConfig.class)
@ActiveProfiles("cloudfoundry-platform-properties")
public class CloudFoundryPlatformPropertiesTests {

	@Autowired
	private CloudFoundryPlatformProperties cloudFoundryPlatformProperties;

	@Test
	public void deserializationTest() {
		Map<String, CloudFoundryPlatformProperties.CloudFoundryProperties> cfAccounts = this.cloudFoundryPlatformProperties
				.getAccounts();
		assertThat(cfAccounts).hasSize(2);
		assertThat(cfAccounts).containsKeys("dev", "qa");
		assertThat(cfAccounts.get("dev").getConnection().getOrg()).isEqualTo("myOrg");
		assertThat(cfAccounts.get("dev").getConnection().getClientId()).isEqualTo("id1");
		assertThat(cfAccounts.get("dev").getConnection().getClientSecret()).isEqualTo("secret1");
		assertThat(cfAccounts.get("dev").getDeployment().getMemory()).isEqualTo("512m");
		assertThat(cfAccounts.get("dev").getDeployment().getDisk()).isEqualTo("2048m");
		assertThat(cfAccounts.get("dev").getDeployment().getInstances()).isEqualTo(4);
		assertThat(cfAccounts.get("dev").getDeployment().getAppNamePrefix()).isEqualTo("dev1");
		assertThat(cfAccounts.get("dev").getDeployment().getServices())
				.containsExactlyInAnyOrder("rabbit", "mysql");

		assertThat(cfAccounts.get("qa").getConnection().getOrg()).isEqualTo("myOrgQA");
		assertThat(cfAccounts.get("qa").getConnection().getClientId()).isEqualTo("id2");
		assertThat(cfAccounts.get("qa").getConnection().getClientSecret()).isEqualTo("secret2");
		assertThat(cfAccounts.get("qa").getDeployment().getMemory()).isEqualTo("756m");
		assertThat(cfAccounts.get("qa").getDeployment().getDisk()).isEqualTo("724m");
		assertThat(cfAccounts.get("qa").getDeployment().getInstances()).isEqualTo(2);
		assertThat(cfAccounts.get("qa").getDeployment().getAppNamePrefix()).isEqualTo("qa1");
		assertThat(cfAccounts.get("qa").getDeployment().getServices())
				.containsExactlyInAnyOrder("rabbitQA", "mysqlQA");
	}

	@Configuration
	@EnableConfigurationProperties(CloudFoundryPlatformProperties.class)
	static class TestConfig {
	}
}
