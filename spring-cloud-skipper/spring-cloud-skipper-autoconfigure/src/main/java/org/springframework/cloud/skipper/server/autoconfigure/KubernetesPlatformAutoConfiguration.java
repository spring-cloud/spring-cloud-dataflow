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
package org.springframework.cloud.skipper.server.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesActuatorTemplate;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAppDeployer;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesClientFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.skipper.deployer.kubernetes.KubernetesPlatformProperties;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * @author Donovan Muller
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 */
@AutoConfiguration
@EnableConfigurationProperties(KubernetesPlatformProperties.class)
public class KubernetesPlatformAutoConfiguration {

	private static final Logger logger = LoggerFactory
			.getLogger(KubernetesPlatformAutoConfiguration.class);

	@Bean
	public Platform kubernetesPlatform(
			KubernetesPlatformProperties kubernetesPlatformProperties, RestTemplate actuatorRestTemplate) {
		List<Deployer> deployers = new ArrayList<>();
		Map<String, KubernetesDeployerProperties> k8ConnectionProperties = kubernetesPlatformProperties
				.getAccounts();
		k8ConnectionProperties.forEach((key, value) -> {
			Deployer deployer = createAndSaveKubernetesAppDeployers(key, value, actuatorRestTemplate);
			deployers.add(deployer);
		});

		return new Platform("Kubernetes", deployers);
	}

	@Bean
	@ConditionalOnMissingBean
	RestTemplate actuatorRestTemplate() {
		return new RestTemplate();
	}

	protected Deployer createAndSaveKubernetesAppDeployers(String account,
			KubernetesDeployerProperties kubernetesProperties, RestTemplate restTemplate) {
		KubernetesClient kubernetesClient = KubernetesClientFactory.getKubernetesClient(kubernetesProperties);
		ContainerFactory containerFactory = new DefaultContainerFactory(
				kubernetesProperties);
		KubernetesAppDeployer kubernetesAppDeployer = new KubernetesAppDeployer(
				kubernetesProperties, kubernetesClient, containerFactory);
		ActuatorOperations actuatorOperations =
				new KubernetesActuatorTemplate(restTemplate, kubernetesAppDeployer,
						kubernetesProperties.getAppAdmin());
		Deployer deployer = new Deployer(account, "kubernetes", kubernetesAppDeployer, actuatorOperations);
		deployer.setDescription(
				"master url = [%s], namespace = [%s], api version = [%s]".formatted(
						kubernetesClient.getMasterUrl(), kubernetesClient.getNamespace(),
						kubernetesClient.getApiVersion()));
		return deployer;
	}
}
