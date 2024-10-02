/*
 * Copyright 2018 the original author or authors.
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


import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.server.config.features.SchedulerConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesSchedulerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Configures the Spring Cloud Kubernetes Scheduler based on feature toggle settings and if running on
 * Kubernetes.
 *
 * @author Chris Schaefer
 */
@AutoConfiguration
@Conditional({SchedulerConfiguration.SchedulerConfigurationPropertyChecker.class})
@Profile("kubernetes")
public class KubernetesSchedulerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Primary
	public KubernetesSchedulerProperties kubernetesSchedulerProperties() {
		return new KubernetesSchedulerProperties();
	}
}
