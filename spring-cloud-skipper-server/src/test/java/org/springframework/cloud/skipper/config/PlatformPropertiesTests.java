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

package org.springframework.cloud.skipper.config;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("platform-properties")
public class PlatformPropertiesTests {

	@Autowired
	private CloudFoundryPlatformProperties cloudFoundryPlatformProperties;

	@Autowired
	private LocalPlatformProperties localPlatformProperties;

	@Test
	public void deserializationTest() {
		Map<String, CloudFoundryConnectionProperties> cfAccounts = cloudFoundryPlatformProperties.getAccounts();
		assertThat(cfAccounts).hasSize(2);
		assertThat(cfAccounts).containsKeys("dev", "qa");
		assertThat(cfAccounts.get("dev").getOrg()).isEqualTo("myOrg");
		assertThat(cfAccounts.get("qa").getOrg()).isEqualTo("myOrgQA");

		Map<String, LocalDeployerProperties> localAccounts = localPlatformProperties.getAccounts();
		assertThat(localAccounts).hasSize(2);
		assertThat(localAccounts).containsKeys("localDev", "localDevDebug");
		assertThat(localAccounts.get("localDev").getShutdownTimeout()).isEqualTo(60);
		assertThat(localAccounts.get("localDevDebug").getJavaOpts()).isEqualTo("-Xdebug");
	}

}
