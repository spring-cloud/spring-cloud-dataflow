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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDefinitionServiceUtils;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;

/**
 * Provides completion proposals when the user has typed the two dashes that precede an
 * app configuration property.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
class ConfigurationPropertyNameAfterDashDashRecoveryStrategy
		extends StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final ProposalsCollectorSupportUtils collectorSupport;

	ConfigurationPropertyNameAfterDashDashRecoveryStrategy(AppRegistryService appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver,
			StreamDefinitionService streamDefinitionService) {
		super(CheckPointedParseException.class, streamDefinitionService, "file --", "file | foo --");
		this.collectorSupport = new ProposalsCollectorSupportUtils(appRegistry, metadataResolver);
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception, int detailLevel,
			List<CompletionProposal> collector) {

		String safe = exception.getExpressionStringUntilCheckpoint();
		StreamDefinition streamDefinition = new StreamDefinition("__dummy", safe);
		LinkedList<StreamAppDefinition> streamAppDefinitions = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		StreamAppDefinition lastApp = StreamDefinitionServiceUtils.getDeploymentOrderIterator(streamAppDefinitions).next();

		AppRegistration appRegistration = this.collectorSupport.findAppRegistration(lastApp.getName(),
				CompletionUtils.determinePotentialTypes(lastApp, streamAppDefinitions.size() > 1));

		if (appRegistration != null) {
			Set<String> alreadyPresentOptions = new HashSet<>(lastApp.getProperties().keySet());
			this.collectorSupport.addPropertiesProposals(safe, "", appRegistration, alreadyPresentOptions, collector, detailLevel);
		}
	}
}
