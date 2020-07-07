/*
 * Copyright 2019-2020 the original author or authors.
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

import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.dataflow.core.AbstractTaskPlatformFactory;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesClientFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesScheduler;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesSchedulerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;

/**
 * @author David Turanski
 * @author Glenn Renfro
 **/
public class KubernetesTaskPlatformFactory extends AbstractTaskPlatformFactory<KubernetesPlatformProperties> {

	private final boolean schedulesEnabled;

	public KubernetesTaskPlatformFactory(
			KubernetesPlatformProperties platformProperties,
			boolean schedulesEnabled) {
		super(platformProperties, KUBERNETES_PLATFORM_TYPE);
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

		KubernetesSchedulerProperties kubernetesSchedulerProperties = new KubernetesSchedulerProperties();
		BeanUtils.copyProperties(kubernetesProperties, kubernetesSchedulerProperties);
		Scheduler scheduler = getScheduler(kubernetesSchedulerProperties, kubernetesClient);

		Launcher launcher = new Launcher(account, KUBERNETES_PLATFORM_TYPE, kubernetesTaskLauncher, scheduler);

		launcher.setDescription(
				String.format("master url = [%s], namespace = [%s], api version = [%s]",
						kubernetesClient.getMasterUrl(), kubernetesClient.getNamespace(),
						kubernetesClient.getApiVersion()));

		return launcher;
	}

	private Scheduler getScheduler(KubernetesSchedulerProperties kubernetesSchedulerProperties,
			KubernetesClient kubernetesClient) {
		Scheduler scheduler = null;

		if (schedulesEnabled) {
			scheduler = new KubernetesScheduler(kubernetesClient, kubernetesSchedulerProperties);
		}

		return scheduler;
	}
}
