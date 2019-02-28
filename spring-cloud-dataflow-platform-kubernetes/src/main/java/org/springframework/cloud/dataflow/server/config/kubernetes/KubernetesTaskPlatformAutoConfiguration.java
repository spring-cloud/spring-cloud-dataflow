/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.config.kubernetes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesClientFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Mark Pollack
 */
@Configuration
@EnableConfigurationProperties(KubernetesPlatformProperties.class)
@Profile("kubernetes")
public class KubernetesTaskPlatformAutoConfiguration {

	@Bean
	public TaskPlatform kubernetesTaskPlatform(KubernetesPlatformProperties kubernetesPlatformProperties) {
		List<Launcher> launchers = new ArrayList<>();
		Map<String, KubernetesDeployerProperties> k8sDeployerPropertiesMap = kubernetesPlatformProperties
				.getAccounts();
		k8sDeployerPropertiesMap.forEach((key, value) -> {
			Launcher launcher = createAndSaveKubernetesTaskLaunchers(key, value);
			launchers.add(launcher);
		});

		return new TaskPlatform("Kubernetes", launchers);
	}

	protected Launcher createAndSaveKubernetesTaskLaunchers(String account,
			KubernetesDeployerProperties kubernetesProperties) {
		KubernetesClient kubernetesClient = KubernetesClientFactory.getKubernetesClient(kubernetesProperties);
		ContainerFactory containerFactory = new DefaultContainerFactory(
				kubernetesProperties);
		KubernetesTaskLauncher kubernetesTaskLauncher = new KubernetesTaskLauncher(
				kubernetesProperties, kubernetesClient, containerFactory);
		Launcher launcher = new Launcher(account, "kubernetes", kubernetesTaskLauncher);
		launcher.setDescription(
				String.format("master url = [%s], namespace = [%s], api version = [%s]",
						kubernetesClient.getMasterUrl(), kubernetesClient.getNamespace(),
						kubernetesClient.getApiVersion()));
		return launcher;
	}

}
