/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.HashMap;

import org.hamcrest.collection.IsMapContaining;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.hamcrest.CoreMatchers.not;

/**
 * Unit tests for StreamDeploymentController.
 *
 * @author Eric Bottard
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamDeploymentControllerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StreamDeploymentController controller;

	@Mock
	private StreamDefinitionRepository streamDefinitionRepository;

	@Mock
	private DeploymentIdRepository deploymentIdRepository;

	@Mock
	private AppRegistry appRegistry;

	@Mock
	private AppDeployer appDeployer;

	private ApplicationConfigurationMetadataResolver metadataResolver = new BootApplicationConfigurationMetadataResolver();

	@Mock
	private CommonApplicationProperties commonApplicationProperties;

	@Before
	public void setup() {
		controller = new StreamDeploymentController(streamDefinitionRepository, deploymentIdRepository, appRegistry,
				appDeployer, metadataResolver, commonApplicationProperties);
	}

	@Test
	public void testRequalifyShortWhiteListedProperty() {
		StreamAppDefinition appDefinition = new StreamAppDefinition.Builder().setRegisteredAppName("my-app")
				.setProperty("timezone", "GMT+2").build("streamname");

		Resource app = new ClassPathResource("/apps/whitelist-source");
		AppDefinition modified = controller.mergeAndExpandAppProperties(appDefinition, app, new HashMap<>());

		Assert.assertThat(modified.getProperties(), IsMapContaining.hasEntry("date.timezone", "GMT+2"));
		Assert.assertThat(modified.getProperties(), not(IsMapContaining.hasKey("timezone")));
	}

	@Test
	public void testSameNamePropertiesOKAsLongAsNotUsedAsShorthand() {
		StreamAppDefinition appDefinition = new StreamAppDefinition.Builder().setRegisteredAppName("my-app")
				.setProperty("time.format", "hh").setProperty("date.format", "yy").build("streamname");

		Resource app = new ClassPathResource("/apps/whitelist-source");
		AppDefinition modified = controller.mergeAndExpandAppProperties(appDefinition, app, new HashMap<>());

		Assert.assertThat(modified.getProperties(), IsMapContaining.hasEntry("date.format", "yy"));
		Assert.assertThat(modified.getProperties(), IsMapContaining.hasEntry("time.format", "hh"));
	}

	@Test
	public void testSameNamePropertiesKOWhenShorthand() {
		StreamAppDefinition appDefinition = new StreamAppDefinition.Builder().setRegisteredAppName("my-app")
				.setProperty("format", "hh").build("streamname");

		Resource app = new ClassPathResource("/apps/whitelist-source");

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Ambiguous short form property 'format'");
		thrown.expectMessage("date.format");
		thrown.expectMessage("time.format");

		controller.mergeAndExpandAppProperties(appDefinition, app, new HashMap<>());

	}

	@Test
	public void testShorthandsAcceptRelaxedVariations() {
		StreamAppDefinition appDefinition = new StreamAppDefinition.Builder().setRegisteredAppName("my-app")
				.setProperty("someLongProperty", "yy") // Use camelCase here
				.build("streamname");

		Resource app = new ClassPathResource("/apps/whitelist-source");
		AppDefinition modified = controller.mergeAndExpandAppProperties(appDefinition, app, new HashMap<>());

		Assert.assertThat(modified.getProperties(), IsMapContaining.hasEntry("date.some-long-property", "yy"));

	}

}
