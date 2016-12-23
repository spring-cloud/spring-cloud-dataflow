/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.local;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.springframework.analytics.metrics.FieldValueCounterRepository;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.local.dataflowapp.LocalTestDataFlowServer;
import org.springframework.cloud.dataflow.server.local.nodataflowapp.LocalTestNoDataFlowServer;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.SocketUtils;

/**
 * Tests for {@link LocalTestDataFlowServer}.
 *
 * @author Janne Valkealahti
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class LocalConfigurationTests {

	private static final String APP_DEPLOYER_BEAN_NAME = "appDeployer";

	private static final String TASK_LAUNCHER_BEAN_NAME = "taskLauncher";

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		context.close();
	}

	@Test
	public void testConfig() {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		int randomPort = SocketUtils.findAvailableTcpPort();
		String dataSourceUrl = String.format("jdbc:h2:tcp://localhost:%s/mem:dataflow", randomPort);
		context = app.run(new String[] { "--server.port=0",
				"--spring.datasource.url=" + dataSourceUrl});
		assertThat(context.containsBean(APP_DEPLOYER_BEAN_NAME), is(true));
		assertThat(context.getBean(APP_DEPLOYER_BEAN_NAME), instanceOf(LocalAppDeployer.class));
		assertThat(context.containsBean(TASK_LAUNCHER_BEAN_NAME), is(true));
		assertThat(context.getBean(TASK_LAUNCHER_BEAN_NAME), instanceOf(LocalTaskLauncher.class));
		assertNotNull(context.getBean(AppRegistry.class));
	}

	@Test
	public void testLocalAutoConfigApplied() throws Exception {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		context = app.run(new String[] { "--server.port=0" });

		// default on DataFlowControllerAutoConfiguration only adds maven,
		// LocalDataFlowServerAutoConfiguration also adds docker so test on those.
		DelegatingResourceLoader delegatingResourceLoader = context.getBean(DelegatingResourceLoader.class);
		Map<String, ResourceLoader> loaders = TestUtils.readField("loaders", delegatingResourceLoader);
		assertThat(loaders.size(), is(2));
		assertThat(loaders.get("maven"), notNullValue());
		assertThat(loaders.get("docker"), notNullValue());
	}

	@Test
	public void testConfigWithStreamsDisabled() {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		context = app.run(new String[] { "--server.port=0",
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.STREAMS_ENABLED + "=false"});
		assertNotNull(context.getBean(TaskDefinitionRepository.class));
		assertNotNull(context.getBean(DeploymentIdRepository.class));
		assertNotNull(context.getBean(FieldValueCounterRepository.class));
		try {
			context.getBean(StreamDefinitionRepository.class);
			fail("Stream features should have been disabled.");
		}
		catch (NoSuchBeanDefinitionException e) {
		}
	}

	@Test
	public void testConfigWithTasksDisabled() {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		context = app.run(new String[] { "--server.port=0",
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.TASKS_ENABLED + "=false"});
		assertNotNull(context.getBean(StreamDefinitionRepository.class));
		assertNotNull(context.getBean(DeploymentIdRepository.class));
		assertNotNull(context.getBean(FieldValueCounterRepository.class));
		try {
			context.getBean(TaskDefinitionRepository.class);
			fail("Task features should have been disabled.");
		}
		catch (NoSuchBeanDefinitionException e) {
		}
	}

	@Test
	public void testConfigWithAnalyticsDisabled() {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		context = app.run(new String[]{"--server.port=0",
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.ANALYTICS_ENABLED + "=false"});;
		assertNotNull(context.getBean(StreamDefinitionRepository.class));
		assertNotNull(context.getBean(TaskDefinitionRepository.class));
		assertNotNull(context.getBean(DeploymentIdRepository.class));
		try {
			context.getBean(FieldValueCounterRepository.class);
			fail("Task features should have been disabled.");
		}
		catch (NoSuchBeanDefinitionException e) {
		}
	}

	@Test
	public void testNoDataflowConfig() {
		SpringApplication app = new SpringApplication(LocalTestNoDataFlowServer.class);
		context = app.run(new String[] { "--server.port=0" });
		// we still have deployer beans
		assertThat(context.containsBean(APP_DEPLOYER_BEAN_NAME), is(true));
		assertThat(context.containsBean(TASK_LAUNCHER_BEAN_NAME), is(true));
		assertThat(context.containsBean("appRegistry"), is(false));
	}
}
