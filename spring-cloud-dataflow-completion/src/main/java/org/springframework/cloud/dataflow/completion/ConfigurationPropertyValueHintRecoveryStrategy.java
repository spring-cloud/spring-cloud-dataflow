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

import java.util.LinkedList;
import java.util.List;

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
 * Attempts to fill in possible values after a {@literal --foo=} dangling construct in the
 * DSL.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class ConfigurationPropertyValueHintRecoveryStrategy
		extends StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final ProposalsCollectorSupportUtils collectorSupport;

	@Autowired
	private ValueHintProvider[] valueHintProviders = new ValueHintProvider[0];

	ConfigurationPropertyValueHintRecoveryStrategy(AppRegistryService appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver,
			StreamDefinitionService streamDefinitionService) {
		super(CheckPointedParseException.class, streamDefinitionService,"foo --bar=", "foo | wizz --bar=");
		this.collectorSupport = new ProposalsCollectorSupportUtils(appRegistry, metadataResolver);
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception, int detailLevel,
			List<CompletionProposal> collector) {

		String propertyName = recoverPropertyName(exception);

		AppRegistration lastAppRegistration = lookupLastApp(exception);

		if (lastAppRegistration != null) {
			this.collectorSupport.addValueHintsProposals(dsl, lastAppRegistration, collector, propertyName, valueHintProviders);
		}
	}

	private AppRegistration lookupLastApp(CheckPointedParseException exception) {
		String safe = exception.getExpressionStringUntilCheckpoint();
		StreamDefinition streamDefinition = new StreamDefinition("__dummy", safe);
		LinkedList<StreamAppDefinition> streamAppDefinitions = this.streamDefinitionService.getAppDefinitions(streamDefinition);
		StreamAppDefinition lastApp = StreamDefinitionServiceUtils.getDeploymentOrderIterator(streamAppDefinitions).next();
		return this.collectorSupport.findAppRegistration(lastApp.getName(),
				CompletionUtils.determinePotentialTypes(lastApp, streamAppDefinitions.size() > 1));
	}

	private String recoverPropertyName(CheckPointedParseException exception) {
		List<Token> tokens = exception.getTokens();
		int tokenPointer = tokens.size() - 1;
		while (!tokens.get(tokenPointer - 1).isKind(TokenKind.DOUBLE_MINUS)) {
			tokenPointer--;
		}
		StringBuilder builder;
		final int equalSignPointer = tokens.size() - 1;
		for (builder = new StringBuilder(); tokenPointer < equalSignPointer; tokenPointer++) {
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
}
