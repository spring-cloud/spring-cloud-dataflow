/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.config.web.WebConfiguration;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.dataflow.server.support.TestUtils;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.task.repository.TaskRepository;
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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public class DataFlowServerConfigurationTests {

	private AnnotationConfigApplicationContext context;

	private ConfigurableEnvironment environment;

	private MutablePropertySources propertySources;

	@Before
	public void setup() {
		context = new AnnotationConfigApplicationContext();
		context.setId("testDataFlowConfig");
		context.register(DataFlowServerConfigurationTests.TestConfiguration.class, RedisAutoConfiguration.class,
				SecurityAutoConfiguration.class, DataFlowServerAutoConfiguration.class,
				DataFlowControllerAutoConfiguration.class, DataSourceAutoConfiguration.class,
				DataFlowServerConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				WebClientAutoConfiguration.class, HibernateJpaAutoConfiguration.class, WebConfiguration.class);
		environment = new StandardEnvironment();
		propertySources = environment.getPropertySources();
	}

	@After
	public void teardown() {
		if (context != null) {
			context.close();
		}
	}

	/**
	 * Verify that embedded server starts if h2 url is specified with default properties.
	 */
	@Test
	@Ignore
	public void testStartEmbeddedH2Server() {
		Map myMap = new HashMap();
		myMap.put("spring.datasource.url", "jdbc:h2:tcp://localhost:19092/mem:dataflow");
		myMap.put("spring.dataflow.embedded.database.enabled", "true");
		propertySources.addFirst(new MapPropertySource("EnvironmentTestPropsource", myMap));
		context.setEnvironment(environment);

		context.refresh();
		assertTrue(context.containsBean("initH2TCPServer"));
	}

	/**
	 * Verify that embedded h2 does not start if h2 url is specified with with the
	 * spring.dataflow.embedded.database.enabled is set to false.
	 *
	 * @throws Throwable if any error occurs and should be handled by the caller.
	 */
	@Test(expected = ConnectException.class)
	public void testDoNotStartEmbeddedH2Server() throws Throwable {
		Throwable exceptionResult = null;
		Map myMap = new HashMap();
		myMap.put("spring.datasource.url", "jdbc:h2:tcp://localhost:19092/mem:dataflow");
		myMap.put("spring.dataflow.embedded.database.enabled", "false");
		myMap.put("spring.jpa.database", "H2");
		propertySources.addFirst(new MapPropertySource("EnvironmentTestPropsource", myMap));
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
	 * Verify that the embedded server is not started if h2 string is not specified.
	 */
	@Test
	@Ignore
	public void testNoServer() {
		context.refresh();
		assertFalse(context.containsBean("initH2TCPServer"));
	}

	@Test
	public void testSkipperConfig() throws Exception {
		EnvironmentTestUtils.addEnvironment(context, "spring.cloud.skipper.client.serverUri:http://fakehost:1234/api",
				"spring.cloud.dataflow.features.skipper-enabled:true");
		this.context.refresh();
		SkipperClient skipperClient = context.getBean(SkipperClient.class);
		Object baseUri = TestUtils.readField("baseUri", skipperClient);
		assertNotNull(baseUri);
		assertTrue(baseUri.equals("http://fakehost:1234/api"));
		try {
			this.context.getBean(StreamDeploymentRepository.class);
			fail("StreamDeploymentRepository shouldn't exist. Exception expected");
		}
		catch (NoSuchBeanDefinitionException e) {
		}
	}

	@Test
	public void testAppDeployerConfig() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "spring.cloud.dataflow.features.skipper-enabled:false");
		this.context.refresh();
		StreamDeploymentRepository streamDeploymentRepository = this.context.getBean(StreamDeploymentRepository.class);
		assertNotNull(streamDeploymentRepository);
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

		@Bean
		public TaskRepository taskRepository() {
			return mock(TaskRepository.class);
		}
	}
}
