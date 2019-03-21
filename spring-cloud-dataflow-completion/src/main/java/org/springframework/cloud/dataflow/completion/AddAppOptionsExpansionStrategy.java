/*
 * Copyright 2015-2018 the original author or authors.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;

/**
 * Adds missing application configuration properties at the end of a well formed stream
 * definition.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
class AddAppOptionsExpansionStrategy implements ExpansionStrategy {

	private final ProposalsCollectorSupportUtils collectorSupport;

	public AddAppOptionsExpansionStrategy(AppRegistryCommon appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		this.collectorSupport = new ProposalsCollectorSupportUtils(appRegistry, metadataResolver);
	}

	@Override
	public boolean addProposals(String text, StreamDefinition streamDefinition, int detailLevel,
			List<CompletionProposal> collector) {
		StreamAppDefinition lastApp = streamDefinition.getDeploymentOrderIterator().next();
		AppRegistration appRegistration = this.collectorSupport.findAppRegistration(lastApp.getName(),
				CompletionUtils.determinePotentialTypes(lastApp,streamDefinition.getAppDefinitions().size() > 1));

		if (appRegistration != null) {
			Set<String> alreadyPresentOptions = new HashSet<>(lastApp.getProperties().keySet());
			this.collectorSupport.addPropertiesProposals(text, "", appRegistration, alreadyPresentOptions, collector, detailLevel);
		}
		return false;
	}
}
