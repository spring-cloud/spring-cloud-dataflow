/*
 * Copyright 2019-2021 the original author or authors.
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

import java.util.List;

import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.cloud.dataflow.core.AbstractTaskPlatformFactory;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesClientFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesScheduler;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncherProperties;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;

/**
 * @author David Turanski
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 **/
public class KubernetesTaskPlatformFactory extends AbstractTaskPlatformFactory<KubernetesPlatformProperties> {

	private final KubernetesPlatformTaskLauncherProperties platformTaskLauncherProperties;

	private final boolean schedulesEnabled;

	public KubernetesTaskPlatformFactory(
			KubernetesPlatformProperties platformProperties,
			boolean schedulesEnabled,
			KubernetesPlatformTaskLauncherProperties kubernetesPlatformTaskLauncherProperties) {
		super(platformProperties, KUBERNETES_PLATFORM_TYPE);
		this.schedulesEnabled = schedulesEnabled;
		this.platformTaskLauncherProperties = kubernetesPlatformTaskLauncherProperties;
	}

	@Override
	public Launcher createLauncher(String account) {
		KubernetesDeployerProperties kubernetesProperties = this.platformProperties.accountExists(account) ?
				this.platformProperties.accountProperties(account) : new KubernetesDeployerProperties();
		KubernetesTaskLauncherProperties taskLauncherProperties = (this.platformTaskLauncherProperties.accountExists(account)) ?
					this.platformTaskLauncherProperties.accountProperties(account) : new KubernetesTaskLauncherProperties();
		ContainerFactory containerFactory = new DefaultContainerFactory(kubernetesProperties);
		KubernetesClient kubernetesClient = KubernetesClientFactory.getKubernetesClient(kubernetesProperties);

		KubernetesTaskLauncher kubernetesTaskLauncher = new KubernetesTaskLauncher(
				kubernetesProperties, taskLauncherProperties, kubernetesClient, containerFactory);

		Scheduler scheduler = getScheduler(kubernetesProperties, kubernetesClient);

		Launcher launcher = new Launcher(account, KUBERNETES_PLATFORM_TYPE, kubernetesTaskLauncher, scheduler);

		launcher.setDescription(
				String.format("master url = [%s], namespace = [%s], api version = [%s]",
						kubernetesClient.getMasterUrl(), kubernetesClient.getNamespace(),
						kubernetesClient.getApiVersion()));

		return launcher;
	}

	@Override
	protected List<Launcher> createLaunchers() {
		List<Launcher> launchers = super.createLaunchers();
		for (String account : this.platformTaskLauncherProperties.getAccounts().keySet()) {
			try {
				if (!this.platformProperties.accountExists(account)) {
					launchers.add(createLauncher(account));
				}
			}
			catch (Exception e) {
				logger.error("{} platform account [{}] could not be registered: {}",
						this.platformType, account, e);
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		return launchers;
	}

	private Scheduler getScheduler(KubernetesDeployerProperties kubernetesDeployerProperties,
			KubernetesClient kubernetesClient) {
		Scheduler scheduler = null;

		if (schedulesEnabled) {
			scheduler = new KubernetesScheduler(kubernetesClient, kubernetesDeployerProperties);
		}

		return scheduler;
	}
}
