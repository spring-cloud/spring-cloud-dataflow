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

package org.springframework.cloud.dataflow.autoconfigure.local;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.cloud.scheduler.spi.core.Scheduler;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Tzolov
 */
@RunWith(Enclosed.class)
public class SchedulerPerPlatformTest {

	@TestPropertySource(properties = { "spring.cloud.dataflow.features.schedules-enabled=false" })
	public static class AllSchedulerDisabledTests extends AbstractSchedulerPerPlatformTest {

		@Test(expected = NoSuchBeanDefinitionException.class)
		public void testLocalSchedulerEnabled() {
			assertFalse(context.getEnvironment().containsProperty("kubernetes.service.host"));
			assertFalse(CloudPlatform.CLOUD_FOUNDRY.isActive(context.getEnvironment()));
			context.getBean(Scheduler.class);
		}
	}

	@TestPropertySource(properties = { "spring.cloud.dataflow.features.schedules-enabled=true" })
	public static class LocalSchedulerTests extends AbstractSchedulerPerPlatformTest {

		@Test
		public void testLocalSchedulerEnabled() {
			assertFalse("K8s should be disabled", context.getEnvironment().containsProperty("kubernetes.service.host"));
			assertFalse("CF should be disabled", CloudPlatform.CLOUD_FOUNDRY.isActive(context.getEnvironment()));

			Scheduler scheduler = context.getBean(Scheduler.class);

			assertNotNull(scheduler);
			assertTrue(scheduler.getClass().getName().contains("LocalSchedulerAutoConfiguration"));
		}
	}

	@TestPropertySource(properties = { "spring.cloud.dataflow.features.schedules-enabled=true",
			"kubernetes.service.host=dummy" })
	public static class KubernetesSchedulerActivatedTests extends AbstractSchedulerPerPlatformTest {

		@Test
		public void testLocalSchedulerEnabled() {
			assertTrue("K8s should be enabled", context.getEnvironment().containsProperty("kubernetes.service.host"));
			assertFalse("CF should be disabled", CloudPlatform.CLOUD_FOUNDRY.isActive(context.getEnvironment()));

			Scheduler scheduler = context.getBean(Scheduler.class);

			assertNotNull(scheduler);
			assertTrue(scheduler.getClass().getName().contains("KubernetesScheduler"));
		}

	}

	@TestPropertySource(properties = { "spring.cloud.dataflow.features.schedules-enabled=true",
			"VCAP_APPLICATION=dummy" })
	public static class CloudFoundrySchedulerActivatedTests extends AbstractSchedulerPerPlatformTest {

		@Test
		public void testLocalSchedulerEnabled() {
			assertFalse("K8s should be disabled", context.getEnvironment().containsProperty("kubernetes.service.host"));
			assertTrue("CF should be enabled", CloudPlatform.CLOUD_FOUNDRY.isActive(context.getEnvironment()));

			Scheduler scheduler = context.getBean(Scheduler.class);

			assertNotNull(scheduler);
			assertTrue(scheduler.getClass().getName().contains("CloudFoundryAppScheduler"));
		}
	}
}
