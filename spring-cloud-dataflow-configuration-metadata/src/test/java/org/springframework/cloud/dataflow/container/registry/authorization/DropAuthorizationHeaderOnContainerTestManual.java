/*
 * Copyright 2024 the original author or authors.
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
 * This test is aimed at performing a manual test against a deployed container registry;
 * In order to invoke this test populate the fields of DropAuthorizationHeaderOnContainerTestManual.TestApplication
 * named registryDomainName, registryUser, registrySecret and imageNameAndTag
 *
 * The image should be one built with spring-boot:build-image or paketo so that is has a label named 'org.springframework.boot.version'
 * For docker hub use:
 *   registryDomainName="registry-1.docker.io",
 *   registryUser="docker user"
 *   registrySecret="docker access token"
 *   imageNameAndTag="springcloudstream/s3-sink-rabbit:5.0.0"
 *
 * @author Corneil du Plessis
 */
public class DropAuthorizationHeaderOnContainerTestManual {

	private static final String registryDomainName = "registry-1.docker.io";
	private static final String registryUser = "<docker-user>";
	private static final String registrySecret = "<docker-access-token>";
	private static final String imageNameAndTag = "springcloudstream/s3-sink-rabbit:5.0.0";

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
		DefaultContainerImageMetadataResolver imageMetadataResolver = context.getBean(DefaultContainerImageMetadataResolver.class);
		Map<String, String> imageLabels = imageMetadataResolver.getImageLabels(registryDomainName + "/" + imageNameAndTag);
		System.out.println("imageLabels:" + imageLabels.keySet());
		assertThat(imageLabels).containsKey("org.springframework.boot.version");
	}

	@Import({ContainerRegistryAutoConfiguration.class, ApplicationConfigurationMetadataResolverAutoConfiguration.class})
	@AutoConfigureWebClient
	static class TestApplication {

		@Bean
		@Primary
		ContainerRegistryProperties containerRegistryProperties() {
			ContainerRegistryProperties properties = new ContainerRegistryProperties();
			ContainerRegistryConfiguration registryConfiguration = new ContainerRegistryConfiguration();
			registryConfiguration.setRegistryHost(registryDomainName);
			registryConfiguration.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
			registryConfiguration.setUser(registryUser);
			registryConfiguration.setSecret(registrySecret);
			properties.setRegistryConfigurations(Collections.singletonMap(registryDomainName, registryConfiguration));

			return properties;
		}
	}
}
