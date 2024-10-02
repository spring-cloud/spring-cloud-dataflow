/*
 * Copyright 2018-2021 the original author or authors.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.config.CloudProfileProvider;
import org.springframework.cloud.dataflow.server.config.features.ConditionalOnTasksEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Creates TaskPlatform implementations to launch/schedule tasks on Kubernetes.
 * @author Mark Pollack
 * @author David Turanski
 */
@AutoConfiguration
@EnableConfigurationProperties({KubernetesPlatformProperties.class, KubernetesPlatformTaskLauncherProperties.class})
@ConditionalOnTasksEnabled
public class KubernetesTaskPlatformAutoConfiguration {

	@Bean
	public KubernetesTaskPlatformFactory kubernetesTaskPlatformFactory(
			KubernetesPlatformProperties platformProperties,
			@Value("${spring.cloud.dataflow.features.schedules-enabled:false}") boolean schedulesEnabled,
			KubernetesPlatformTaskLauncherProperties platformTaskLauncherProperties) {
		return new KubernetesTaskPlatformFactory(platformProperties, schedulesEnabled, platformTaskLauncherProperties);
	}

	@Bean
	public TaskPlatform kubernetesTaskPlatform(KubernetesTaskPlatformFactory kubernetesTaskPlatformFactory,
			Environment environment) {
		TaskPlatform taskPlatform = kubernetesTaskPlatformFactory.createTaskPlatform();
		CloudProfileProvider cloudProfileProvider = new KubernetesCloudProfileProvider();
		if (cloudProfileProvider.isCloudPlatform(environment)) {
			taskPlatform.setPrimary(true);
		}
		return taskPlatform;
	}
}
