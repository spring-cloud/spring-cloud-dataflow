/*
 * Copyright 2019 the original author or authors.
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

import java.util.Optional;

import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.cloud.dataflow.core.AbstractTaskPlatformFactory;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesClientFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.scheduler.spi.core.Scheduler;
import org.springframework.cloud.scheduler.spi.kubernetes.KubernetesScheduler;
import org.springframework.cloud.scheduler.spi.kubernetes.KubernetesSchedulerProperties;

/**
 * @author David Turanski
 **/
public class KubernetesTaskPlatformFactory extends AbstractTaskPlatformFactory<KubernetesPlatformProperties> {

	private final static String PLATFORM_TYPE = "Kubernetes";

	private final Optional<KubernetesSchedulerProperties> schedulerProperties;

	private final boolean schedulesEnabled;

	public KubernetesTaskPlatformFactory(
			KubernetesPlatformProperties platformProperties,
			Optional<KubernetesSchedulerProperties> schedulerProperties,
			boolean schedulesEnabled) {
		super(platformProperties, PLATFORM_TYPE);
		this.schedulerProperties = schedulerProperties;
		this.schedulesEnabled = schedulesEnabled;
	}

	@Override
	public Launcher createLauncher(String account) {
		KubernetesDeployerProperties kubernetesProperties = this.platformProperties.accountProperties(account);
		ContainerFactory containerFactory = new DefaultContainerFactory(
				this.platformProperties.accountProperties(account));
		KubernetesClient kubernetesClient =
				KubernetesClientFactory.getKubernetesClient(this.platformProperties.accountProperties(account));

		KubernetesTaskLauncher kubernetesTaskLauncher = new KubernetesTaskLauncher(
				kubernetesProperties, kubernetesClient, containerFactory);

		Scheduler scheduler = getScheduler(schedulerProperties, kubernetesClient);
		Launcher launcher = new Launcher(account, PLATFORM_TYPE, kubernetesTaskLauncher, scheduler);

		launcher.setDescription(
				String.format("master url = [%s], namespace = [%s], api version = [%s]",
						kubernetesClient.getMasterUrl(), kubernetesClient.getNamespace(),
						kubernetesClient.getApiVersion()));

		return launcher;
	}

	private Scheduler getScheduler(Optional<KubernetesSchedulerProperties> kubernetesSchedulerProperties,
			KubernetesClient kubernetesClient) {
		Scheduler scheduler = null;

		if (schedulesEnabled) {
			KubernetesSchedulerProperties schedulerProperties = kubernetesSchedulerProperties
					.orElseGet(KubernetesSchedulerProperties::new);
			scheduler = new KubernetesScheduler(kubernetesClient, schedulerProperties);
		}

		return scheduler;
	}
}
