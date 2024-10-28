/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.container.registry.authorization;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolverAutoConfiguration;
import org.springframework.cloud.dataflow.configuration.metadata.container.DefaultContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryAutoConfiguration;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryConfiguration;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adam J. Weigold
 * @author Corneil du Plessis
 */
public class DropAuthorizationHeaderOnContainerTestManual {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void clean() {
		if (context != null) {
			context.close();
		}
		context = null;
	}

	@Test
	void testContainerImageLabels() {
		context = new AnnotationConfigApplicationContext(TestApplication.class);
		ContainerRegistryProperties registryProperties = context.getBean(ContainerRegistryProperties.class);
		DefaultContainerImageMetadataResolver imageMetadataResolver = context.getBean(DefaultContainerImageMetadataResolver.class);
		String imageNameAndTag = "springcloudstream/s3-sink-rabbit:5.0.0";
		Map<String, String> imageLabels = imageMetadataResolver.getImageLabels(registryProperties.getDefaultRegistryHost() + "/" + imageNameAndTag);
		System.out.println("imageLabels:" + imageLabels.keySet());
		assertThat(imageLabels).containsKey("org.springframework.boot.spring-configuration-metadata.json");
	}

	@Import({ContainerRegistryAutoConfiguration.class, ApplicationConfigurationMetadataResolverAutoConfiguration.class})
	@AutoConfigureWebClient
	static class TestApplication {
		@Bean
		@Primary
		ContainerRegistryProperties containerRegistryProperties() {
			ContainerRegistryProperties properties = new ContainerRegistryProperties();
			ContainerRegistryConfiguration registryConfiguration = new ContainerRegistryConfiguration();
			registryConfiguration.setRegistryHost("registry-1.docker.io");
			registryConfiguration.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
			registryConfiguration.setUser("<Docker username>");
			registryConfiguration.setSecret("<Docker PAT>");
			properties.setRegistryConfigurations(Collections.singletonMap("registry-1.docker.io", registryConfiguration));

			return properties;
		}
	}
}
