/*
 * Copyright 2017-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner.properties;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
public class ComposedTaskPropertiesTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void testGettersAndSetters() throws URISyntaxException{
		ComposedTaskProperties properties = new ComposedTaskProperties();
		properties.setComposedTaskProperties("aaa");
		properties.setComposedTaskArguments("bbb");
		properties.setIntervalTimeBetweenChecks(12345);
		properties.setMaxWaitTime(6789);
		properties.setDataflowServerUri(new URI("http://test"));
		properties.setGraph("ddd");
		properties.setDataflowServerUsername("foo");
		properties.setDataflowServerPassword("bar");
		properties.setDataflowServerAccessToken("foobar");
		properties.setSkipTlsCertificateVerification(true);
		assertEquals("aaa", properties.getComposedTaskProperties());
		assertEquals("bbb", properties.getComposedTaskArguments());
		assertEquals(12345, properties.getIntervalTimeBetweenChecks());
		assertEquals(6789, properties.getMaxWaitTime());
		assertEquals("http://test", properties.getDataflowServerUri().toString());
		assertEquals("ddd", properties.getGraph());
		assertEquals("foo", properties.getDataflowServerUsername());
		assertEquals("bar", properties.getDataflowServerPassword());
		assertEquals("foobar", properties.getDataflowServerAccessToken());
		assertTrue(properties.isSkipTlsCertificateVerification());
	}

	@Test
	public void testDataflowServerURIDefaults() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		assertEquals("http://localhost:9393", properties.getDataflowServerUri().toString());
	}

	@Test
	public void testSkipSslVerificationDefaults() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		assertFalse(properties.isSkipTlsCertificateVerification());
	}

	@Test
	public void testThreadDefaults() {
		ComposedTaskProperties properties = new ComposedTaskProperties();
		assertEquals(ComposedTaskProperties.SPLIT_THREAD_CORE_POOL_SIZE_DEFAULT, properties.getSplitThreadCorePoolSize());
		assertEquals(ComposedTaskProperties.SPLIT_THREAD_KEEP_ALIVE_SECONDS_DEFAULT, properties.getSplitThreadKeepAliveSeconds());
		assertEquals(ComposedTaskProperties.SPLIT_THREAD_MAX_POOL_SIZE_DEFAULT, properties.getSplitThreadMaxPoolSize());
		assertEquals(ComposedTaskProperties.SPLIT_THREAD_QUEUE_CAPACITY_DEFAULT, properties.getSplitThreadQueueCapacity());
		assertEquals("http://localhost:9393", properties.getDataflowServerUri().toString());
		assertFalse(properties.isSplitThreadAllowCoreThreadTimeout());
		assertFalse(properties.isSplitThreadWaitForTasksToCompleteOnShutdown());
		assertNull(properties.getDataflowServerUsername());
		assertNull(properties.getDataflowServerPassword());
	}

	@Test
	public void testComposedTaskAppArguments() {
		this.contextRunner
				.withInitializer(context -> {
					Map<String, Object> map = new HashMap<>();
					map.put("composed-task-app-arguments.app.AAA", "arg1");
					map.put("composed-task-app-arguments.app.AAA.1", "arg2");
					map.put("composed-task-app-arguments.app.AAA.2", "arg3");
					context.getEnvironment().getPropertySources().addLast(new SystemEnvironmentPropertySource(
						StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, map));
				})
				.withUserConfiguration(Config1.class)
				.run((context) -> {
					ComposedTaskProperties properties = context.getBean(ComposedTaskProperties.class);
					assertThat(properties.getComposedTaskAppArguments()).hasSize(3);
					assertThat(properties.getComposedTaskAppArguments()).containsEntry("app.AAA", "arg1");
					assertThat(properties.getComposedTaskAppArguments()).containsEntry("app.AAA.1", "arg2");
					assertThat(properties.getComposedTaskAppArguments()).containsEntry("app.AAA.2", "arg3");
				});
	}

	@EnableConfigurationProperties({ ComposedTaskProperties.class })
	private static class Config1 {
	}
}
