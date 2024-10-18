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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolverAutoConfiguration;
import org.springframework.cloud.dataflow.configuration.metadata.container.DefaultContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryAutoConfiguration;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryConfiguration;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryProperties;
import org.springframework.cloud.dataflow.container.registry.authorization.support.S3SignedRedirectRequestServerApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Adam J. Weigold
 * @author Corneil du Plessis
 */
//TODO: Boot3x followup
@Disabled("TODO: Netty HttpClient configuration for both http and https required")
public class DropAuthorizationHeaderOnInsecureS3RequestRedirectStrategyTest {
	private final static Logger logger = LoggerFactory.getLogger(DropAuthorizationHeaderOnInsecureS3RequestRedirectStrategyTest.class);
	private AnnotationConfigApplicationContext context;
	private ConfigurableApplicationContext application;
	private static int serverPort;
	@BeforeEach
	void setup() {
			 serverPort = TestSocketUtils.findAvailableTcpPort();

			logger.info("Setting S3 Signed Redirect Server port to " + serverPort);

			this.application = new SpringApplicationBuilder(S3SignedRedirectRequestServerApplication.class).build()
				.run("--server.port=" + serverPort);
			logger.info("S3 Signed Redirect Server Server is UP! at " + serverPort);
	}
	@AfterEach
	void clean() {
		if (context != null) {
			context.close();
		}
		context = null;
	}

	@Test
	void redirect() {
		context = new AnnotationConfigApplicationContext(TestApplication.class);

		final DefaultContainerImageMetadataResolver imageMetadataResolver =
			context.getBean(DefaultContainerImageMetadataResolver.class);

		Map<String, String> imageLabels = imageMetadataResolver.getImageLabels("localhost:" +
			serverPort + "/test/s3-redirect-image:1.0.0");

		assertThat(imageLabels).containsOnly(entry("foo", "bar"));
	}

	@Import({ContainerRegistryAutoConfiguration.class, ApplicationConfigurationMetadataResolverAutoConfiguration.class})
	@AutoConfigureWebClient
	static class TestApplication {
		@Bean
		@Primary
		ContainerRegistryProperties containerRegistryProperties() {
			ContainerRegistryProperties properties = new ContainerRegistryProperties();
			ContainerRegistryConfiguration registryConfiguration = new ContainerRegistryConfiguration();
			registryConfiguration.setRegistryHost(String.format("localhost:%s", serverPort));
			registryConfiguration.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
			registryConfiguration.setUser("admin");
			registryConfiguration.setSecret("Harbor12345");
			registryConfiguration.setDisableSslVerification(true);
			registryConfiguration.setExtra(Collections.singletonMap(
				DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY,
				"http://localhost:" + serverPort + "/service/token"));
			properties.setRegistryConfigurations(Collections.singletonMap("goharbor", registryConfiguration));

			return properties;
		}
	}
}
