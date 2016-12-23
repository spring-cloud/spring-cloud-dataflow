/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 */
public class DataFlowServerConfigurationTests {

	private AnnotationConfigApplicationContext context;
	private ConfigurableEnvironment environment;
	private MutablePropertySources propertySources;

	@Before
	public void setup() {
		context = new AnnotationConfigApplicationContext();
		context.setId("testDataFlowConfig");
		context.register(
				DataFlowServerConfigurationTests.TestConfiguration.class,
				RedisAutoConfiguration.class,
				SecurityAutoConfiguration.class,
				DataFlowServerAutoConfiguration.class,
				DataFlowControllerAutoConfiguration.class,
				DataSourceAutoConfiguration.class,
				DataFlowServerConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		environment = new StandardEnvironment();
		propertySources = environment.getPropertySources();
	}

	@After
	public void teardown() {
		if(context != null && context.isActive()) {
			context.close();
		}
	}

	/**
	 * Verify that embedded server starts if h2 url is specified with default
	 * properties.
	 */
	@Test
	public void testStartEmbeddedH2Server(){
		Map myMap = new HashMap();
		myMap.put("spring.datasource.url", "jdbc:h2:tcp://localhost:19092/mem:dataflow");
		propertySources.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		context.setEnvironment(environment);

		context.refresh();
		assertTrue(context.containsBean("initH2TCPServer"));
	}

	/**
	 * Verify that embedded h2 does not start if h2 url is specified with
	 * with the spring.dataflow.embedded.database.enabled is set to false.
	 */
	@Test (expected = ConnectException.class)
	public void testDoNotStartEmbeddedH2Server() throws Throwable{
		Throwable exceptionResult = null;
		Map myMap = new HashMap();
		myMap.put("spring.datasource.url", "jdbc:h2:tcp://localhost:19092/mem:dataflow");
		myMap.put("spring.dataflow.embedded.database.enabled", "false");
		propertySources.addFirst(new MapPropertySource("EnvrionmentTestPropsource", myMap));
		context.setEnvironment(environment);
		try {
			context.refresh();
		}
		catch (BeanCreationException exception) {
			exceptionResult = exception.getRootCause();
		}
		assertNotNull(exceptionResult);
		throw exceptionResult;
	}

	/**
	 * 	Verify that the embedded server is not started if h2 string is not
	 * 	specified.
	 */
	@Test
	public void testNoServer(){
		context.refresh();
		assertFalse(context.containsBean("initH2TCPServer"));
	}

	@EnableDataFlowServer
	private static class TestConfiguration {

		@Bean
		public AppDeployer appDeployer() {
			return mock(AppDeployer.class);
		}

		@Bean
		public TaskLauncher taskLauncher() {
			return mock(TaskLauncher.class);
		}

		@Bean
		public AuthenticationManager authenticationManager() {
			return mock(AuthenticationManager.class);
		}

		@Bean
		public TaskService taskService() {
			return mock(DefaultTaskService.class);
		}
	}
}
