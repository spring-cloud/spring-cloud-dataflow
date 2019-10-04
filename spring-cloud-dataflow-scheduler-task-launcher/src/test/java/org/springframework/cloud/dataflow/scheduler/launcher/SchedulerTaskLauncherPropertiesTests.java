/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.scheduler.launcher;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.cloud.dataflow.scheduler.launcher.configuration.SchedulerTaskLauncherProperties;

public class SchedulerTaskLauncherPropertiesTests {

	public static final String TASK_NAME = "TASK_NAME";

	public static final String PROPERTY_PREFIX = "PROPERTY_PREFIX";

	public static final String PLATFORM_NAME = "PLATFORM_NAME";

	public static final String DATAFLOW_SERVER_URI = "DATAFLOW_SERVER_URI";

	@Test
	public void testProperties() {

		SchedulerTaskLauncherProperties properties = new SchedulerTaskLauncherProperties();
		properties.setTaskName(TASK_NAME);
		properties.setTaskLauncherPropertyPrefix(PROPERTY_PREFIX);
		properties.setPlatformName(PLATFORM_NAME);
		properties.setDataflowServerUri(DATAFLOW_SERVER_URI);
		Assert.assertEquals(TASK_NAME, properties.getTaskName());
		Assert.assertEquals(PROPERTY_PREFIX, properties.getTaskLauncherPropertyPrefix());
		Assert.assertEquals(PLATFORM_NAME, properties.getPlatformName());
		Assert.assertEquals(DATAFLOW_SERVER_URI, properties.getDataflowServerUri());
	}

}
