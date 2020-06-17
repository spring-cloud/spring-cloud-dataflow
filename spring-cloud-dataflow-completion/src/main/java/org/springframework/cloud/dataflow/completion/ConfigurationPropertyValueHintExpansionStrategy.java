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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDefinitionServiceUtils;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.core.dsl.Token;
import org.springframework.cloud.dataflow.core.dsl.TokenKind;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;

/**
 * Attempts to fill in possible values after a {@literal --foo=prefix} (syntactically
 * valid) construct in the DSL.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ConfigurationPropertyValueHintExpansionStrategy implements ExpansionStrategy {

	private final ProposalsCollectorSupportUtils collectorSupport;

	private final StreamDefinitionService streamDefinitionService;

	@Autowired
	private ValueHintProvider[] valueHintProviders = new ValueHintProvider[0];

	ConfigurationPropertyValueHintExpansionStrategy(AppRegistryService appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver,
			StreamDefinitionService streamDefinitionService) {
		this.collectorSupport = new ProposalsCollectorSupportUtils(appRegistry, metadataResolver);
		this.streamDefinitionService = streamDefinitionService;
	}

	@Override
	public boolean addProposals(String text, StreamDefinition streamDefinition, int detailLevel,
			List<CompletionProposal> collector) {
		LinkedList<StreamAppDefinition> streamAppDefinitions = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		Set<String> propertyNames = new HashSet<>(
				StreamDefinitionServiceUtils.getDeploymentOrderIterator(streamAppDefinitions).next().getProperties().keySet());
		propertyNames.removeAll(CompletionUtils.IMPLICIT_PARAMETER_NAMES);
		if (text.endsWith(" ") || propertyNames.isEmpty()) {
			return false;
		}

		String propertyName = recoverPropertyName(text);

		StreamAppDefinition lastApp = StreamDefinitionServiceUtils.getDeploymentOrderIterator(streamAppDefinitions).next();
		String alreadyTyped = lastApp.getProperties().get(propertyName);

		AppRegistration lastAppRegistration = this.collectorSupport.findAppRegistration(lastApp.getName(),
				CompletionUtils.determinePotentialTypes(lastApp, streamAppDefinitions.size() > 1));
		if (lastAppRegistration != null) {
			return this.collectorSupport.addAlreadyTypedValueHintsProposals(text, lastAppRegistration, collector, propertyName, valueHintProviders, alreadyTyped);
		}
		return false;
	}

	// This may be the safest way to backtrack to the property name
	// to avoid dealing with escaped space characters, etc.
	private String recoverPropertyName(String text) {
		try {
			this.streamDefinitionService.parse(new StreamDefinition("__dummy", text + " --"));
		}
		catch (CheckPointedParseException exception) {
			List<Token> tokens = exception.getTokens();
			int end = tokens.size() - 1 - 2; // -2 for skipping dangling -- and space preceding it
			int tokenPointer = end;
			while (!tokens.get(tokenPointer - 1).isKind(TokenKind.DOUBLE_MINUS)) {
				tokenPointer--;
			}
			StringBuilder builder;
			for (builder = new StringBuilder(); tokenPointer < end; tokenPointer++) {
				Token t = tokens.get(tokenPointer);
				if (t.isIdentifier()) {
					builder.append(t.stringValue());
				}
				else {
					builder.append(t.getKind().getTokenChars());
				}
			}
			return builder.toString();
		}
		throw new AssertionError("Can't be reached");
	}

}
