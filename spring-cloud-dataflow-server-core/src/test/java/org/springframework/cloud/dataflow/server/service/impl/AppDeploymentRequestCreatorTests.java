/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.cloud.dataflow.configuration.metadata.BootApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.DefaultStreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 * @author Corneil du Plessis
 */
@ExtendWith(SpringExtension.class)
class AppDeploymentRequestCreatorTests {



	private AppDeploymentRequestCreator appDeploymentRequestCreator;

	@BeforeEach
	void setupMock() {
		this.appDeploymentRequestCreator = new AppDeploymentRequestCreator(mock(AppRegistryService.class),
				mock(CommonApplicationProperties.class),
				new BootApplicationConfigurationMetadataResolver(mock(ContainerImageMetadataResolver.class)),
				new DefaultStreamDefinitionService());
	}

	@Test
	void requalifyShortVisibleProperty() {
		StreamAppDefinition appDefinition = new StreamAppDefinition.Builder().setRegisteredAppName("my-app")
				.setApplicationType(ApplicationType.app)
				.setProperty("timezone", "GMT+2").build("streamname");

		Resource app = new ClassPathResource("/apps/included-source");
		AppDefinition modified = this.appDeploymentRequestCreator.mergeAndExpandAppProperties(appDefinition, app,
				new HashMap<>());

		assertThat(modified.getProperties()).containsEntry("date.timezone", "GMT+2");
		assertThat(modified.getProperties()).doesNotContainKey("timezone");
	}

	@Test
	void sameNamePropertiesOKAsLongAsNotUsedAsShorthand() {
		StreamAppDefinition appDefinition = new StreamAppDefinition.Builder().setRegisteredAppName("my-app")
				.setApplicationType(ApplicationType.app)
				.setProperty("time.format", "hh")
				.setProperty("date.format", "yy")
				.build("streamname");

		Resource app = new ClassPathResource("/apps/included-source");
		AppDefinition modified = this.appDeploymentRequestCreator.mergeAndExpandAppProperties(appDefinition, app, Collections.emptyMap());

		assertThat(modified.getProperties()).containsEntry("date.format", "yy");
		assertThat(modified.getProperties()).containsEntry("time.format", "hh");
	}

	@Test
	void sameNamePropertiesKOWhenShorthand() {
		StreamAppDefinition appDefinition = new StreamAppDefinition.Builder().setRegisteredAppName("my-app")
				.setApplicationType(ApplicationType.app)
				.setProperty("format", "hh").build("streamname");

		Resource app = new ClassPathResource("/apps/included-source");

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			this.appDeploymentRequestCreator.mergeAndExpandAppProperties(appDefinition, app, new HashMap<>());
		}).withMessageContaining("Ambiguous short form property 'format'")
				.withMessageContaining("date.format")
				.withMessageContaining("time.format");
	}

	@Test
	void shorthandsAcceptRelaxedVariations() {
		StreamAppDefinition appDefinition = new StreamAppDefinition.Builder().setRegisteredAppName("my-app")
				.setApplicationType(ApplicationType.app)
				.setProperty("someLongProperty", "yy") // Use camelCase here
				.build("streamname");

		Resource app = new ClassPathResource("/apps/included-source");
		AppDefinition modified = this.appDeploymentRequestCreator.mergeAndExpandAppProperties(appDefinition, app,
				new HashMap<>());

		assertThat(modified.getProperties()).containsEntry("date.some-long-property", "yy");

	}
}
