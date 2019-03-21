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

package org.springframework.cloud.dataflow.completion;

import static org.springframework.cloud.dataflow.completion.CompletionProposal.expanding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.core.io.Resource;

/**
 * Adds missing application configuration properties at the end of a well formed task definition.
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 */
class AddAppOptionsTaskExpansionStrategy implements TaskExpansionStrategy {

	private final AppRegistry appRegistry;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	public AddAppOptionsTaskExpansionStrategy(AppRegistry appRegistry, ApplicationConfigurationMetadataResolver metadataResolver) {
		this.appRegistry = appRegistry;
		this.metadataResolver = metadataResolver;
	}

	@Override
	public boolean addProposals(String text, TaskDefinition taskDefinition, int detailLevel,
	                            List<CompletionProposal> collector) {
		String appName = taskDefinition.getRegisteredAppName();
		AppRegistration appRegistration = this.appRegistry.find(appName, ApplicationType.task);
		if (appRegistration == null) {
			// Not a valid app, do nothing
			return false;
		}
		Set<String> alreadyPresentOptions = new HashSet<>(taskDefinition.getProperties().keySet());
		Resource jarFile = appRegistration.getResource();
		CompletionProposal.Factory proposals = expanding(text);

		// For whitelisted properties, use their simple name
		for (ConfigurationMetadataProperty property : metadataResolver.listProperties(jarFile)) {
			if (!alreadyPresentOptions.contains(property.getName())) {
				collector.add(proposals.withSeparateTokens("--" + property.getName() + "=", property.getShortDescription()));
			}
		}

		// For other properties (including WL'ed in full form), use their id
		if (detailLevel > 1) {
			for (ConfigurationMetadataProperty property : metadataResolver.listProperties(jarFile, true)) {
				if (!alreadyPresentOptions.contains(property.getId())) {
					collector.add(proposals.withSeparateTokens("--" + property.getId() + "=", property.getShortDescription()));
				}
			}

		}

		return false;
	}

}
