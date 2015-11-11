/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.completion;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolver;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolverAutoConfiguration;
import org.springframework.cloud.stream.module.resolver.ModuleResolver;
import org.springframework.cloud.stream.module.resolver.ModuleResolverConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Include this Configuration class to expose a fully configured {@link StreamCompletionProvider}.
 *
 * @author Eric Bottard
 */
@Configuration
@Import({ModuleResolverConfiguration.class, ModuleConfigurationMetadataResolverAutoConfiguration.class})
public class CompletionConfiguration {

	@Autowired
	private ArtifactRegistry artifactRegistry;

	@Autowired
	private ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver;

	@Autowired
	private ModuleResolver moduleResolver;


	@Bean
	public StreamCompletionProvider streamCompletionProvider() {
		return new StreamCompletionProvider();
	}

	@Bean
	public ExpansionStrategy addModuleOptionsExpansionStrategy() {
		return new AddModuleOptionsExpansionStrategy(artifactRegistry, moduleConfigurationMetadataResolver, moduleResolver);
	}

	@Bean
	public RecoveryStrategy emptyStartYieldsModulesRecoveryStrategy() {
		return new EmptyStartYieldsSourceModulesRecoveryStrategy(artifactRegistry);
	}

	@Bean
	public RecoveryStrategy expandOneDashToTwoDashesRecoveryStrategy() {
		return new ExpandOneDashToTwoDashesRecoveryStrategy();
	}

	@Bean
	public RecoveryStrategy configurationPropertyNameAfterDashDashRecoveryStrategy() {
		return new ConfigurationPropertyNameAfterDashDashRecoveryStrategy(artifactRegistry,
				moduleResolver, moduleConfigurationMetadataResolver);
	}

	@Bean
	public RecoveryStrategy unfinishedConfigurationPropertyNameRecoveryStrategy() {
		return new UnfinishedConfigurationPropertyNameRecoveryStrategy(artifactRegistry,
				moduleResolver, moduleConfigurationMetadataResolver);
	}

	@Bean
	public RecoveryStrategy modulesAfterPipeRecoveryStrategy() {
		return new ModulesAfterPipeRecoveryStrategy(artifactRegistry);
	}

	@Bean
	public ExpansionStrategy unfinishedModuleNameExpansionStrategy() {
		return new UnfinishedModuleNameExpansionStrategy(artifactRegistry);
	}

	@Bean
	public RecoveryStrategy channelNameYieldsModulesRecoveryStrategy() {
		return new ChannelNameYieldsModulesRecoveryStrategy(artifactRegistry);
	}

	@Bean
	public ExpansionStrategy pipeIntoOtherModulesExpansionStrategy() {
		return new PipeIntoOtherModulesExpansionStrategy(artifactRegistry);
	}

	@Bean
	public RecoveryStrategy configurationPropertyValueHintRecoveryStrategy() {
		return new ConfigurationPropertyValueHintRecoveryStrategy(artifactRegistry,
				moduleResolver, moduleConfigurationMetadataResolver);
	}

	@Bean
	public ExpansionStrategy configurationPropertyValueHintExpansionStrategy() {
		return new ConfigurationPropertyValueHintExpansionStrategy(artifactRegistry,
				moduleResolver, moduleConfigurationMetadataResolver);
	}

	@Bean
	public ValueHintProvider defaultValueHintProvider() {
		return new DefaultValueHintProvider();
	}

	@Bean
	public ValueHintProvider enumValueHintProvider() {
		return new EnumValueHintProvider();
	}

	@Bean
	public ValueHintProvider booleanValueHintProvider() {
		return new BooleanValueHintProvider();
	}
}
