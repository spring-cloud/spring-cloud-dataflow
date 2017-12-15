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
import org.springframework.cloud.deployer.spi.kubernetes.EntryPointStyle;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.skipper.server.config.PlatformPropertiesTests.TestConfig;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("platform-properties")
public class PlatformPropertiesTests {

	@Autowired
	private CloudFoundryPlatformProperties cloudFoundryPlatformProperties;

	@Autowired
	private LocalPlatformProperties localPlatformProperties;

	@Autowired
	private KubernetesPlatformProperties kubernetesPlatformProperties;

	@Test
	public void deserializationTest() {
		Map<String, CloudFoundryPlatformProperties.CloudFoundryProperties> cfAccounts = this.cloudFoundryPlatformProperties
				.getAccounts();
		assertThat(cfAccounts).hasSize(2);
		assertThat(cfAccounts).containsKeys("dev", "qa");
		assertThat(cfAccounts.get("dev").getConnection().getOrg()).isEqualTo("myOrg");
		assertThat(cfAccounts.get("qa").getConnection().getOrg()).isEqualTo("myOrgQA");
		assertThat(cfAccounts.get("dev").getDeployment().getMemory()).isEqualTo("512m");
		assertThat(cfAccounts.get("dev").getDeployment().getDisk()).isEqualTo("2048m");
		assertThat(cfAccounts.get("dev").getDeployment().getInstances()).isEqualTo(4);
		assertThat(cfAccounts.get("dev").getDeployment().getServices()).containsExactly("rabbit", "mysql");
		assertThat(cfAccounts.get("qa").getDeployment().getMemory()).isEqualTo("756m");
		assertThat(cfAccounts.get("qa").getDeployment().getDisk()).isEqualTo("724m");
		assertThat(cfAccounts.get("qa").getDeployment().getInstances()).isEqualTo(2);
		assertThat(cfAccounts.get("qa").getDeployment().getServices()).containsExactly("rabbitQA", "mysqlQA");
		Map<String, LocalDeployerProperties> localAccounts = this.localPlatformProperties.getAccounts();
		assertThat(localAccounts).hasSize(2);
		assertThat(localAccounts).containsKeys("localDev", "localDevDebug");
		assertThat(localAccounts.get("localDev").getShutdownTimeout()).isEqualTo(60);
		assertThat(localAccounts.get("localDevDebug").getJavaOpts()).isEqualTo("-Xdebug");

		Map<String, KubernetesDeployerProperties> k8sAccounts = this.kubernetesPlatformProperties.getAccounts();
		assertThat(k8sAccounts).hasSize(2);
		assertThat(k8sAccounts).containsKeys("dev", "qa");
		assertThat(k8sAccounts.get("dev").getNamespace()).isEqualTo("devNamespace");
		assertThat(k8sAccounts.get("dev").getImagePullPolicy()).isEqualTo(ImagePullPolicy.Always);
		assertThat(k8sAccounts.get("dev").getEntryPointStyle()).isEqualTo(EntryPointStyle.exec);
		assertThat(k8sAccounts.get("dev").getLimits().getCpu()).isEqualTo("4");
		assertThat(k8sAccounts.get("qa").getNamespace()).isEqualTo("qaNamespace");
		assertThat(k8sAccounts.get("qa").getImagePullPolicy()).isEqualTo(ImagePullPolicy.IfNotPresent);
		assertThat(k8sAccounts.get("qa").getEntryPointStyle()).isEqualTo(EntryPointStyle.boot);
		assertThat(k8sAccounts.get("qa").getLimits().getMemory()).isEqualTo("1024m");
	}

	@Configuration
	@ImportAutoConfiguration(classes = { EmbeddedDataSourceConfiguration.class, HibernateJpaAutoConfiguration.class,
			StateMachineJpaRepositoriesAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	static class TestConfig {
	}
}
