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

import static org.springframework.cloud.dataflow.completion.CompletionProposal.expanding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.module.registry.ArtifactRegistration;
import org.springframework.cloud.dataflow.module.registry.ArtifactRegistry;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolver;
import org.springframework.cloud.stream.module.resolver.ModuleResolver;
import org.springframework.core.io.Resource;

/**
 * Provides completion proposals when the user has typed the two dashes that
 * precede a module configuration property.
 *
 * @author Eric Bottard
 */
class ConfigurationPropertyNameAfterDashDashRecoveryStrategy
		extends StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final ArtifactRegistry moduleRegistry;

	private final ModuleResolver moduleResolver;

	private final ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver;

	ConfigurationPropertyNameAfterDashDashRecoveryStrategy(ArtifactRegistry moduleRegistry,
			ModuleResolver moduleResolver, ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver) {
		super(CheckPointedParseException.class, "file --", "file | foo --");
		this.moduleRegistry = moduleRegistry;
		this.moduleResolver = moduleResolver;
		this.moduleConfigurationMetadataResolver = moduleConfigurationMetadataResolver;
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception,
			int detailLevel, List<CompletionProposal> collector) {

		String safe = exception.getExpressionStringUntilCheckpoint();
		StreamDefinition streamDefinition = new StreamDefinition("__dummy", safe);
		ModuleDefinition lastModule = streamDefinition.getDeploymentOrderIterator().next();

		String lastModuleName = lastModule.getName();
		ArtifactRegistration lastArtifactRegistration = null;
		for (ArtifactType moduleType : CompletionUtils.determinePotentialTypes(lastModule)) {
			lastArtifactRegistration = moduleRegistry.find(lastModuleName, moduleType);
			if (lastArtifactRegistration != null) {
				break;
			}
		}
		if (lastArtifactRegistration == null) {
			// Not a valid module name, do nothing
			return;
		}
		Set<String> alreadyPresentOptions = new HashSet<>(lastModule.getParameters().keySet());

		Resource jarFile = moduleResolver.resolve(CompletionUtils
				.fromModuleCoordinates(lastArtifactRegistration.getCoordinates()));

		CompletionProposal.Factory proposals = expanding(dsl);

		for (ConfigurationMetadataProperty property : moduleConfigurationMetadataResolver.listProperties(jarFile)) {
			if (!alreadyPresentOptions.contains(property.getId())) {
				collector.add(proposals.withSuffix(property.getId() + "=", property.getShortDescription()));
			}
		}

	}
}
