/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataProperties;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImageParser;
import org.springframework.cloud.dataflow.configuration.metadata.container.DefaultContainerImageMetadataResolver;
import org.springframework.cloud.dataflow.configuration.metadata.container.RegistryConfiguration;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.AwsEcrAuthorizer;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.BasicAuthRegistryAuthorizer;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.DockerHubRegistryAuthorizer;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.RegistryAuthorizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Automatically exposes an {@link ApplicationConfigurationMetadataResolver} if none is already registered.
 *
 * @author Eric Bottard
 * @author Christian Tzolov
 */
@Configuration
public class ApplicationConfigurationMetadataResolverAutoConfiguration {

	@Bean
	public RegistryAuthorizer dockerHubRegistryAuthorizer() {
		return new DockerHubRegistryAuthorizer();
	}

	@Bean
	public RegistryAuthorizer artifactoryRegistryAuthorizer() {
		return new BasicAuthRegistryAuthorizer();
	}

	@Bean
	public RegistryAuthorizer awsRegistryAuthorizer() {
		return new AwsEcrAuthorizer();
	}

	@Bean
	public ContainerImageParser containerImageParser(ContainerImageMetadataProperties properties) {
		return new ContainerImageParser(properties.getDefaultRegistryHost(),
				properties.getDefaultRepositoryTag(), properties.getOfficialRepositoryNamespace());
	}

	@Bean
	@Validated
	public ContainerImageMetadataProperties containerImageMetadataProperties() {
		ContainerImageMetadataProperties properties = new ContainerImageMetadataProperties();

		// Add the DockerHub registry configuration by default.
		RegistryConfiguration dockerHubAuthConfig = new RegistryConfiguration();
		dockerHubAuthConfig.setRegistryHost(ContainerImageMetadataProperties.DOCKER_HUB_HOST);
		dockerHubAuthConfig.setAuthorizationType(RegistryConfiguration.AuthorizationType.dockerhub);
		properties.getRegistryConfigurations().add(dockerHubAuthConfig);

		return properties;
	}

	@Bean
	@ConditionalOnMissingBean(ContainerImageMetadataResolver.class)
	public DefaultContainerImageMetadataResolver containerImageMetadataResolver(ContainerImageParser imageNameParser,
			List<RegistryAuthorizer> registryAuthorizers, ContainerImageMetadataProperties properties) {
		return new DefaultContainerImageMetadataResolver(imageNameParser, registryAuthorizers, properties);
	}

	@Bean
	@ConditionalOnMissingBean(ApplicationConfigurationMetadataResolver.class)
	public ApplicationConfigurationMetadataResolver metadataResolver(
			DefaultContainerImageMetadataResolver containerImageMetadataResolver) {
		return new BootApplicationConfigurationMetadataResolver(containerImageMetadataResolver);
	}
}
