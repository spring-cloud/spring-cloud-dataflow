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

package org.springframework.cloud.skipper.server.config;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author Donovan Muller
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = PlatformPropertiesTests.TestConfig.class)
@ActiveProfiles("platform-properties")
public class PlatformPropertiesTests {

	@Autowired
	private LocalPlatformProperties localPlatformProperties;

	@Test
	public void deserializationTest() {
		Map<String, LocalDeployerProperties> localAccounts = this.localPlatformProperties.getAccounts();
		assertThat(localAccounts).hasSize(2);
		assertThat(localAccounts).containsKeys("localDev", "localDevDebug");
		assertThat(localAccounts.get("localDev").getShutdownTimeout()).isEqualTo(60);
		assertThat(localAccounts.get("localDevDebug").getJavaOpts()).isEqualTo("-Xdebug");
	}

	@Configuration
	@ImportAutoConfiguration(classes = { EmbeddedDataSourceConfiguration.class, HibernateJpaAutoConfiguration.class,
			StateMachineJpaRepositoriesAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	static class TestConfig {
	}
}
