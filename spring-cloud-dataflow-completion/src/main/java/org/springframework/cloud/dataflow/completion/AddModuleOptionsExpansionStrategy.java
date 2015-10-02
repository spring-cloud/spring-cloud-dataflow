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

import static org.springframework.cloud.dataflow.completion.CompletionProposal.Factory.expanding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistration;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistry;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolver;
import org.springframework.cloud.stream.module.resolver.ModuleResolver;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Adds missing module configuration properties at the end of a well formed stream definition.
 *
 * @author Eric Bottard
 */
@Component
public class AddModuleOptionsExpansionStrategy implements CompletionExpansionStrategy {


	private final ModuleRegistry moduleRegistry;

	private final ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver;

	private final ModuleResolver moduleResolver;


	/**
	 * Construct a new AddModuleOptionsExpansionStrategy for use in detecting missing module options.
	 *  @param moduleRegistry the registry to check for the existence of the last entered module
	 *        definition.
	 * @param moduleConfigurationMetadataResolver the metadata resolver to use in order to create a list of proposals for
	 * @param moduleResolver
	 */
	@Autowired
	public AddModuleOptionsExpansionStrategy(ModuleRegistry moduleRegistry, ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver, ModuleResolver moduleResolver) {
		this.moduleRegistry = moduleRegistry;
		this.moduleConfigurationMetadataResolver = moduleConfigurationMetadataResolver;
		this.moduleResolver = moduleResolver;
	}

	@Override
	public boolean shouldTrigger(String text, StreamDefinition streamDefinition) {
		return true;
	}

	@Override
	public void addProposals(String text, StreamDefinition streamDefinition, int detailLevel,
			List<CompletionProposal> collector) {
		ModuleDefinition lastModule = streamDefinition.getDeploymentOrderIterator().next();

		String lastModuleName = lastModule.getName();
		ModuleType lastModuleType = CompletionUtils.inferType(lastModule, streamDefinition);

		ModuleRegistration lastModuleRegistration = moduleRegistry.find(lastModuleName, lastModuleType);

		Set<String> alreadyPresentOptions = new HashSet<>(lastModule.getParameters().keySet());

		Resource jarFile = moduleResolver.resolve(CompletionUtils.adapt(lastModuleRegistration.getCoordinates()));

		CompletionProposal.Factory proposals = expanding(text);

		for (ConfigurationMetadataProperty property : moduleConfigurationMetadataResolver.listProperties(jarFile)) {
			if (!alreadyPresentOptions.contains(property.getId())) {
				collector.add(proposals.withSeparateTokens("--" + property.getId() + "=", property.getShortDescription()));
			}
		}

	}

}