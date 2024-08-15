/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.single;

import java.util.Map;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.controller.AboutController;
import org.springframework.cloud.dataflow.server.controller.TaskExecutionController;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.single.dataflowapp.LocalTestDataFlowServer;
import org.springframework.cloud.dataflow.server.single.nodataflowapp.LocalTestNoDataFlowServer;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.TestSocketUtils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link LocalTestDataFlowServer}.
 *
 * @author Janne Valkealahti
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@Disabled
public class LocalConfigurationTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testConfig() {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		int randomPort = TestSocketUtils.findAvailableTcpPort();
		String dataSourceUrl = String.format("jdbc:h2:tcp://localhost:%s/mem:dataflow;DATABASE_TO_UPPER=FALSE", randomPort);
		context = app.run(new String[] { "--debug","--spring.cloud.kubernetes.enabled=false", "--server.port=0", "--spring.datasource.url=" + dataSourceUrl });
		assertNotNull(context.getBean(AppRegistryService.class));
		assertNotNull(context.getBean(TaskExecutionController.class));
		// From DataFlowControllerAutoConfiguration
		assertNotNull(context.getBean(AboutController.class));
	}

	@Test
	public void testLocalAutoConfigApplied() throws Exception {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		context = app.run(new String[] { "--spring.cloud.kubernetes.enabled=false", "--server.port=0" });
		// LocalDataFlowServerAutoConfiguration also adds docker and maven resource loaders.
		DelegatingResourceLoader delegatingResourceLoader = context.getBean(DelegatingResourceLoader.class);
		Map<String, ResourceLoader> loaders = TestUtils.readField("loaders", delegatingResourceLoader);
		assertThat(loaders.size(), is(2));
		assertThat(loaders.get("maven"), notNullValue());
		assertThat(loaders.get("docker"), notNullValue());
	}

	@Test
	public void testConfigWithStreamsDisabled() {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		context = app.run(new String[] { "--spring.cloud.kubernetes.enabled=false", "--server.port=0",
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.STREAMS_ENABLED + "=false" });
		assertNotNull(context.getBean(TaskDefinitionRepository.class));
		// The StreamDefinition repository is expected to exist.
		assertNotNull(context.getBean(StreamDefinitionRepository.class));
		try {
			context.getBean(StreamService.class);
			fail("Stream features should have been disabled.");
		}
		catch (NoSuchBeanDefinitionException e) {
		}
	}

	@Test
	public void testConfigWithTasksDisabled() {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		context = app.run(new String[] { "--spring.cloud.kubernetes.enabled=false", "--server.port=0",
				"--" + FeaturesProperties.FEATURES_PREFIX + "." + FeaturesProperties.TASKS_ENABLED + "=false" });
		assertNotNull(context.getBean(StreamDefinitionRepository.class));
		// The TaskDefinition repository is expected to exist.
		assertNotNull(context.getBean(TaskDefinitionRepository.class));
		try {
			context.getBean(TaskExecutionService.class);
			fail("Task features should have been disabled.");
		}
		catch (NoSuchBeanDefinitionException e) {
		}
	}

	@Test
	public void testNoDataflowConfig() {
		SpringApplication app = new SpringApplication(LocalTestNoDataFlowServer.class);
		context = app.run(new String[] { "--spring.cloud.kubernetes.enabled=false", "--server.port=0", "--spring.jpa.database=H2", "--spring.flyway.enabled=false" });
		assertThat(context.containsBean("appRegistry"), is(false));
	}
}
