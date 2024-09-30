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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncherProperties;
import org.springframework.cloud.deployer.spi.kubernetes.RestartPolicy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 **/
class KubernetesTaskPlatformFactoryTests {

	@Test
	void kubernetesTaskPlatformNoScheduler() {
		KubernetesPlatformProperties platformProperties = new KubernetesPlatformProperties();
		KubernetesDeployerProperties deployerProperties = new KubernetesDeployerProperties();
		KubernetesTaskLauncherProperties taskLauncherProperties = new KubernetesTaskLauncherProperties();
		platformProperties.setAccounts(Collections.singletonMap("k8s", deployerProperties));
		KubernetesPlatformTaskLauncherProperties platformTaskLauncherProperties = new KubernetesPlatformTaskLauncherProperties();
		platformTaskLauncherProperties.setAccounts(Collections.singletonMap("k8s", taskLauncherProperties));
		KubernetesTaskPlatformFactory kubernetesTaskPlatformFactory = new KubernetesTaskPlatformFactory(
				platformProperties, false, platformTaskLauncherProperties);

		TaskPlatform taskPlatform = kubernetesTaskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getName()).isEqualTo("Kubernetes");
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		assertThat(taskPlatform.getLaunchers().get(0).getScheduler()).isNull();
		assertThat(taskPlatform.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(KubernetesTaskLauncher.class);
		assertThat(taskPlatform.getLaunchers().get(0).getName()).isEqualTo("k8s");
		assertThat(taskPlatform.getLaunchers().get(0).getType()).isEqualTo("Kubernetes");
		assertThat(taskPlatform.getLaunchers().get(0).getDescription()).matches("^master url = \\[.+\\], namespace = "
			+ "\\[.+\\], api version = \\[.+\\]$");

	}

	@Test
	void kubernetesTaskPlatformWithScheduler() {
		KubernetesPlatformProperties platformProperties = new KubernetesPlatformProperties();
		KubernetesDeployerProperties deployerProperties = new KubernetesDeployerProperties();
		deployerProperties.getLimits().setMemory("5555Mi");
		KubernetesTaskLauncherProperties taskLauncherProperties = new KubernetesTaskLauncherProperties();
		taskLauncherProperties.setBackoffLimit(5);
		taskLauncherProperties.setRestartPolicy(RestartPolicy.Never);
		platformProperties.setAccounts(Collections.singletonMap("k8s", deployerProperties));
		KubernetesPlatformTaskLauncherProperties platformTaskLauncherProperties = new KubernetesPlatformTaskLauncherProperties();
		KubernetesTaskPlatformFactory kubernetesTaskPlatformFactory = new KubernetesTaskPlatformFactory(
				platformProperties, true, platformTaskLauncherProperties);

		TaskPlatform taskPlatform = kubernetesTaskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getName()).isEqualTo("Kubernetes");
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		Launcher taskLauncher = taskPlatform.getLaunchers().get(0);
		KubernetesDeployerProperties properties = (KubernetesDeployerProperties) ReflectionTestUtils.getField(taskLauncher.getScheduler(), "properties");
		assertThat(properties.getLimits().getMemory()).isEqualTo("5555Mi");

		assertThat(taskLauncher.getScheduler()).isNotNull();
		assertThat(taskLauncher.getTaskLauncher()).isInstanceOf(KubernetesTaskLauncher.class);
		assertThat(taskLauncher.getName()).isEqualTo("k8s");
		assertThat(taskLauncher.getType()).isEqualTo("Kubernetes");
		assertThat(taskLauncher.getDescription()).matches("^master url = \\[.+\\], namespace = "

				+ "\\[.+\\], api version = \\[.+\\]$");
		KubernetesTaskLauncherProperties taskLauncherProps = (KubernetesTaskLauncherProperties) ReflectionTestUtils.getField(taskLauncher.getTaskLauncher(), "taskLauncherProperties");
	}

	@Test
	void kubernetesTaskPlatformWithMultipleAccounts() {
		KubernetesPlatformProperties platformProperties = new KubernetesPlatformProperties();
		KubernetesDeployerProperties deployerProperties = new KubernetesDeployerProperties();
		deployerProperties.getLimits().setMemory("5555Mi");
		KubernetesTaskLauncherProperties taskLauncherProperties = new KubernetesTaskLauncherProperties();
		taskLauncherProperties.setBackoffLimit(5);
		taskLauncherProperties.setRestartPolicy(RestartPolicy.Never);
		platformProperties.setAccounts(Collections.singletonMap("k8s", deployerProperties));
		KubernetesPlatformTaskLauncherProperties platformTaskLauncherProperties = new KubernetesPlatformTaskLauncherProperties();
		platformTaskLauncherProperties.setAccounts(Collections.singletonMap("test", taskLauncherProperties));
		KubernetesTaskPlatformFactory kubernetesTaskPlatformFactory = new KubernetesTaskPlatformFactory(
				platformProperties, true, platformTaskLauncherProperties);

		TaskPlatform taskPlatform = kubernetesTaskPlatformFactory.createTaskPlatform();
		assertThat(taskPlatform.getName()).isEqualTo("Kubernetes");
		assertThat(taskPlatform.getLaunchers()).hasSize(2);
		for (Launcher taskLauncher: taskPlatform.getLaunchers()) {
			assertThat(taskLauncher.getName().equals("k8s") || taskLauncher.getName().equals("test")).isTrue();
			if (taskLauncher.getName().equals("k8s")) {
				KubernetesDeployerProperties properties = (KubernetesDeployerProperties) ReflectionTestUtils.getField(taskLauncher.getScheduler(), "properties");
				assertThat(properties.getLimits().getMemory()).isEqualTo("5555Mi");

				assertThat(taskLauncher.getScheduler()).isNotNull();
				assertThat(taskLauncher.getTaskLauncher()).isInstanceOf(KubernetesTaskLauncher.class);
				assertThat(taskLauncher.getName()).isEqualTo("k8s");
				assertThat(taskLauncher.getType()).isEqualTo("Kubernetes");
				assertThat(taskLauncher.getDescription()).matches("^master url = \\[.+\\], namespace = "

						+ "\\[.+\\], api version = \\[.+\\]$");
			}
			else if (taskLauncher.getName().equals("test")) {
				KubernetesTaskLauncherProperties taskLauncherProps = (KubernetesTaskLauncherProperties) ReflectionTestUtils.getField(taskLauncher.getTaskLauncher(), "taskLauncherProperties");

				assertThat(taskLauncherProps.getBackoffLimit()).isEqualTo(5);
				assertThat(taskLauncherProps.getRestartPolicy()).isEqualTo(RestartPolicy.Never);
			}
		}

	}
}
