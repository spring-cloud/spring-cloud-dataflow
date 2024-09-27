/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.kubernetes;

import java.util.Map;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.kubernetes.EntryPointStyle;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesClientFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Donovan Muller
 * @author Chris Bono
 * @author Corneil du Plessis
 */

@SpringBootTest(classes = KubernetesPlatformPropertiesTests.TestConfig.class,
        properties = { "spring.cloud.kubernetes.client.namespace=default" })
@ActiveProfiles("kubernetes-platform-properties")
public class KubernetesPlatformPropertiesTests {

	@Autowired
	private KubernetesPlatformProperties kubernetesPlatformProperties;

	@Test
	public void deserializationTest() {
		Map<String, KubernetesDeployerProperties> k8sAccounts = this.kubernetesPlatformProperties.getAccounts();
		KubernetesClient devK8sClient = KubernetesClientFactory.getKubernetesClient(k8sAccounts.get("dev"));
		KubernetesClient qaK8sClient = KubernetesClientFactory.getKubernetesClient(k8sAccounts.get("qa"));
		assertThat(k8sAccounts).hasSize(2);
		assertThat(k8sAccounts).containsKeys("dev", "qa");
		assertThat(devK8sClient.getNamespace()).isEqualTo("dev1");
		assertThat(devK8sClient.getMasterUrl()).hasToString("https://192.168.0.1:8443");
		assertThat(qaK8sClient.getMasterUrl()).hasToString("https://192.168.0.2:8443");
		assertThat(k8sAccounts.get("dev").getImagePullPolicy()).isEqualTo(ImagePullPolicy.Always);
		assertThat(k8sAccounts.get("dev").getEntryPointStyle()).isEqualTo(EntryPointStyle.exec);
		assertThat(k8sAccounts.get("dev").getLimits().getCpu()).isEqualTo("4");
		assertThat(k8sAccounts.get("qa").getImagePullPolicy()).isEqualTo(ImagePullPolicy.IfNotPresent);
		assertThat(k8sAccounts.get("qa").getEntryPointStyle()).isEqualTo(EntryPointStyle.boot);
		assertThat(k8sAccounts.get("qa").getLimits().getMemory()).isEqualTo("1024m");
	}

	@Configuration
	@EnableConfigurationProperties(KubernetesPlatformProperties.class)
	static class TestConfig {
	}
}
