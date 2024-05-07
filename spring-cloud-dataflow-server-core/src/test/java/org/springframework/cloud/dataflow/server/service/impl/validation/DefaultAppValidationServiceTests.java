/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl.validation;

import java.net.URI;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.ValidationService;
import org.springframework.cloud.dataflow.server.service.impl.ComposedTaskRunnerConfigurationProperties;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { TaskServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@EnableConfigurationProperties({ CommonApplicationProperties.class, TaskConfigurationProperties.class,
		DockerValidatorProperties.class, ComposedTaskRunnerConfigurationProperties.class })
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class DefaultAppValidationServiceTests {

	@Autowired
	private AppRegistryService appRegistry;

	@Autowired
	DockerValidatorProperties dockerValidatorProperties;

	@Autowired
	TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	public ValidationService appValidationService;

	@Test
	@DirtiesContext
	public void validateValidTaskTest() {
		initializeSuccessfulRegistry(this.appRegistry);
		assertTrue(appValidationService.validate("AAA", ApplicationType.task));
	}

	@Test
	@DirtiesContext
	public void validateInvalidTaskTest() {
		initializeFailRegistry(appRegistry);
		assertFalse(appValidationService.validate("AAA", ApplicationType.task));
	}

	@Test
	@DirtiesContext
	public void validateInvalidDockerTest() {
		initializeDockerRegistry(appRegistry,"notThere/log-sink-rabbit:1.3.1.RELEASE");
		assertFalse(appValidationService.validate("AAA", ApplicationType.task));
	}

	@Test
	@DirtiesContext
	public void validateDockerTest() {
		org.junit.jupiter.api.Assumptions.assumeTrue(dockerCheck());
		initializeDockerRegistry(appRegistry, "springcloudstream/log-sink-rabbit:latest");
		assertTrue(appValidationService.validate("AAA", ApplicationType.task));
	}

	@Test
	@DirtiesContext
	public void validateDockerMultiPageTest() {
		org.junit.jupiter.api.Assumptions.assumeTrue(dockerCheck());
		initializeDockerRegistry(appRegistry, "springcloudstream/log-sink-rabbit:1.3.1.RELEASE");
		assertTrue(appValidationService.validate("AAA", ApplicationType.task));
	}

	@Test
	@DirtiesContext
	public void validateMissingTagDockerTest() {
		initializeDockerRegistry(appRegistry,"springcloudstream/log-sink-rabbit:1.3.1.NOTHERE");
		assertFalse(appValidationService.validate("AAA", ApplicationType.task));
	}

	private void initializeSuccessfulRegistry(AppRegistryService appRegistry) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task"));
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}

	private void initializeDockerRegistry(AppRegistryService appRegistry, String imageUrl) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create(imageUrl)));
		when(appRegistry.getAppResource(any())).thenReturn(new DockerResource(imageUrl));
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}

	private void initializeFailRegistry(AppRegistryService appRegistry) throws IllegalArgumentException {
		when(appRegistry.find("BBB", ApplicationType.task)).thenThrow(new IllegalArgumentException(
				String.format("Application name '%s' with type '%s' does not exist in the app registry.", "fake",
						ApplicationType.task)));
		when(appRegistry.find("AAA", ApplicationType.task)).thenReturn(mock(AppRegistration.class));
	}

	private static boolean dockerCheck() {
		boolean result = true;
		try {
			CloseableHttpClient httpClient
					= HttpClients.custom()
					.setSSLHostnameVerifier(new NoopHostnameVerifier())
					.build();
			HttpComponentsClientHttpRequestFactory requestFactory
					= new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);
			requestFactory.setConnectTimeout(10000);
			requestFactory.setReadTimeout(10000);

			RestTemplate restTemplate = new RestTemplate(requestFactory);
			System.out.println("Testing access to " + DockerValidatorProperties.DOCKER_REGISTRY_URL
					+ "springcloudstream/log-sink-rabbit/tags");
			restTemplate.getForObject(DockerValidatorProperties.DOCKER_REGISTRY_URL
					+ "/springcloudstream/log-sink-rabbit/tags", String.class);
		}
		catch(Exception ex) {
			System.out.println("dockerCheck() failed. " + ex.getMessage());
			result = false;
		}
		return result;
	}

}
