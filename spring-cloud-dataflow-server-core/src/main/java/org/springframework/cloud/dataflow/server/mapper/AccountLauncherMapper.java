/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.mapper;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.cloud.dataflow.core.Account;
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
 * Account Objects Mapper
 * 
 * @author Carlos Miquel Garrido
 */
public class AccountLauncherMapper {

	private ObjectMapper om = new ObjectMapper();

	/**
	 * Kubernetes platform constant
	 */
	private static final String KUBERNETES_PLATFORM_TYPE = "Kubernetes";

	/**
	 * Schedules enabled in configuration
	 */
	private final boolean schedulesEnabled;

	/**
	 * Account Mapper constructor
	 * @param schedulesEnabled Schedules enabled in configuration
	 */
	public AccountLauncherMapper(boolean schedulesEnabled) {
		this.schedulesEnabled = schedulesEnabled;
	}

	/**
	 * Generate Launcher from Account info.
	 * @param account Account info
	 * @return Generated Launcher
	 */
	public Launcher map(Account account) {

		KubernetesDeployerProperties kubernetesProperties = mapProperties(account);
		KubernetesTaskLauncherProperties taskLauncherProperties = new KubernetesTaskLauncherProperties();
		ContainerFactory containerFactory = new DefaultContainerFactory(kubernetesProperties);
		KubernetesClient kubernetesClient = KubernetesClientFactory.getKubernetesClient(kubernetesProperties);

		KubernetesTaskLauncher kubernetesTaskLauncher = new KubernetesTaskLauncher(
				kubernetesProperties, taskLauncherProperties, kubernetesClient, containerFactory);

		Scheduler scheduler = getScheduler(kubernetesProperties, kubernetesClient);

		Launcher launcher = new Launcher(account.getAccountName(), KUBERNETES_PLATFORM_TYPE, kubernetesTaskLauncher,
				scheduler);

		launcher.setDescription(
				String.format("master url = [%s], namespace = [%s], api version = [%s]",
						kubernetesClient.getMasterUrl(), kubernetesClient.getNamespace(),
						kubernetesClient.getApiVersion()));

		return launcher;
	}

	private Scheduler getScheduler(KubernetesDeployerProperties kubernetesDeployerProperties,
			KubernetesClient kubernetesClient) {
		Scheduler scheduler = null;

		if (schedulesEnabled) {
			scheduler = new KubernetesScheduler(kubernetesClient, kubernetesDeployerProperties);
		}

		return scheduler;
	}

	/**
	 * Map account Properties to KubernetesDeployerProperties object.
	 */
	private KubernetesDeployerProperties mapProperties(Account account) {
		KubernetesDeployerProperties kubernetesDeployerProperties;
		try {
			kubernetesDeployerProperties = om.readValue(account.getDeploymentProperties(),
					KubernetesDeployerProperties.class);
		}
		catch (JsonProcessingException e) {
			kubernetesDeployerProperties = new KubernetesDeployerProperties();
		}
		return kubernetesDeployerProperties;
	}

	/**
	 * Map multiple Account Info to Kubernetes launchers.
	 * @param accounts Accounts Info
	 * @return List of Launchers
	 */
	public Iterable<Launcher> map(Iterable<Account> accounts) {
		List<Launcher> response = new ArrayList<>();
		for (Account account : accounts) {
			response.add(map(account));
		}
		return response;
	}

}
