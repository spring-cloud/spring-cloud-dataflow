/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.autoconfigure.local;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesSchedulerProperties;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
public class SchedulerPerPlatformTest {

	@Nested
	@TestPropertySource(properties = { "spring.cloud.dataflow.features.schedules-enabled=false" })
	class AllSchedulerDisabledTests extends AbstractSchedulerPerPlatformTest {

		@Test
		void localSchedulerEnabled() {
			assertThat(context.getEnvironment().containsProperty("kubernetes_service_host")).isFalse();
			assertThat(CloudPlatform.CLOUD_FOUNDRY.isActive(context.getEnvironment())).isFalse();
			assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> {
				context.getBean(Scheduler.class);
			});
		}
	}

	@Nested
	@TestPropertySource(properties = { "spring.cloud.dataflow.features.schedules-enabled=true" })
	class LocalSchedulerTests extends AbstractSchedulerPerPlatformTest {

		@Test
		void localSchedulerEnabled() {
			assertThat(context.getEnvironment().containsProperty("kubernetes_service_host")).as("K8s should be disabled").isFalse();
			assertThat(CloudPlatform.CLOUD_FOUNDRY.isActive(context.getEnvironment())).as("CF should be disabled").isFalse();

			Scheduler scheduler = context.getBean(Scheduler.class);

			assertThat(scheduler).isNotNull();
			assertThat(scheduler.getClass().getName()).contains("LocalSchedulerAutoConfiguration");
		}
	}

	@Nested
	@TestPropertySource(properties = { "spring.cloud.dataflow.features.schedules-enabled=true",
			"kubernetes_service_host=dummy", "spring.cloud.kubernetes.client.namespace=default" })
	class KubernetesSchedulerActivatedTests extends AbstractSchedulerPerPlatformTest {

		@Test
		void kubernetesSchedulerEnabled() {
			assertThat(context.getEnvironment().containsProperty("kubernetes_service_host")).as("K8s should be enabled").isTrue();
			assertThat(CloudPlatform.CLOUD_FOUNDRY.isActive(context.getEnvironment())).as("CF should be disabled").isFalse();


			KubernetesSchedulerProperties props = context.getBean(KubernetesSchedulerProperties.class);
			assertThat(props).isNotNull();
		}

	}

	@Nested
	@TestPropertySource(properties = { "spring.cloud.dataflow.features.schedules-enabled=true",
			"VCAP_APPLICATION=\"{\"instance_id\":\"123\"}\"" })
	class CloudFoundrySchedulerActivatedTests extends AbstractSchedulerPerPlatformTest {

		@Test
		void cloudFoundrySchedulerEnabled() {
			assertThat(context.getEnvironment()
					.containsProperty("kubernetes_service_host")).as("K8s should be disabled").isFalse();
			assertThat(CloudPlatform.CLOUD_FOUNDRY.isActive(context.getEnvironment())).as("CF should be enabled").isTrue();

		}
	}
}
