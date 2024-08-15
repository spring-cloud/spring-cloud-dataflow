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
package org.springframework.cloud.dataflow.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.dataflow.core.DefaultStreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.deployer.spi.local.LocalTaskLauncher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author David Turanski
 * @author Corneil du Plessis
 **/
class LocalPlatformTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
		this.context = null;
	}

	@Test
	void defaultLocalPlatform() {
		this.context = new SpringApplicationBuilder(TestConfig.class)
				.web(WebApplicationType.SERVLET)
				.bannerMode(Banner.Mode.OFF)
				.properties(testProperties())
				.run();
		Map<String, TaskPlatform> taskPlatforms = context.getBeansOfType(TaskPlatform.class);
		assertThat(taskPlatforms).hasSize(1);
		TaskPlatform taskPlatform = taskPlatforms.values().iterator().next();
		assertThat(taskPlatform.getName()).isEqualTo("Local");
		assertThat(taskPlatform.getLaunchers()).hasSize(1);
		assertThat(taskPlatform.getLaunchers().get(0).getName()).isEqualTo("default");
		assertThat(taskPlatform.getLaunchers().get(0).getType()).isEqualTo("Local");
		assertThat(taskPlatform.getLaunchers().get(0).getTaskLauncher()).isInstanceOf(LocalTaskLauncher.class);
	}

	@Test
	void multipleLocalPlatformAccounts() {
		this.context = new SpringApplicationBuilder(TestConfig.class)
				.web(WebApplicationType.SERVLET)
				.bannerMode(Banner.Mode.OFF)
				.properties(testProperties(
						"spring.cloud.dataflow.task.platform.local.accounts[big].javaOpts=-Xmx2048m",
						"spring.cloud.dataflow.task.platform.local.accounts[small].javaOpts=-Xmx1024m"))
				.run();
		Map<String, TaskPlatform> taskPlatforms = context.getBeansOfType(TaskPlatform.class);
		assertThat(taskPlatforms).hasSize(1);
		TaskPlatform taskPlatform = taskPlatforms.values().iterator().next();
		assertThat(taskPlatform.getName()).isEqualTo("Local");
		assertThat(taskPlatform.getLaunchers()).hasSize(2);
		assertThat(taskPlatform.getLaunchers()).extracting("type").containsExactlyInAnyOrder("Local","Local");
		assertThat(taskPlatform.getLaunchers()).extracting("name").containsExactlyInAnyOrder("big", "small");
	}

	@SpringBootApplication
	@EnableDataFlowServer
	static class TestConfig {
		@Bean
		@ConditionalOnMissingBean
		public StreamDefinitionService streamDefinitionService() {
			return new DefaultStreamDefinitionService();
		}
	}

	private String[] testProperties(String... additional) {
		String[] common = {
				"spring.cloud.dataflow.features.streams-enabled=false",
				"spring.main.allow-bean-definition-overriding=true",
				"spring.jmx.enabled=false"
		};

		if (additional.length > 0) {
			int newLength = common.length + additional.length;
			String[] props = Arrays.copyOf(common, newLength);

			for (int i = common.length; i < newLength; i++) {
				props[i] = additional[i - common.length];
			}
			return props;
		}
		return common;
	}
}
