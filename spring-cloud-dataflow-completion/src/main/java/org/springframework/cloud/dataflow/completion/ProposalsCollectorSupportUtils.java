/*
 * Copyright 2017 the original author or authors.
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

import java.util.List;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.core.dsl.Token;
import org.springframework.cloud.dataflow.core.dsl.TokenKind;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.core.io.Resource;

import static org.springframework.cloud.dataflow.completion.CompletionProposal.expanding;

/**
 * Support class to be used by various strategies to gather {@link CompletionProposal}s
 *
 * @see RecoveryStrategy
 * @see ExpansionStrategy
 *
 * @author Oleg Zhurakousky
 */
class ProposalsCollectorSupportUtils {

	private final AppRegistry appRegistry;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	ProposalsCollectorSupportUtils(AppRegistry appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		this.appRegistry = appRegistry;
		this.metadataResolver = metadataResolver;
	}

	void doAddProposals(String text, String startsWith, AppRegistration appRegistration, Set<String> alreadyPresentOptions, List<CompletionProposal> collector, int detailLevel){
		Resource metadataResource = appRegistration.getMetadataResource();
		CompletionProposal.Factory proposals = expanding(text);

		// For whitelisted properties, use their simple name
		for (ConfigurationMetadataProperty property : metadataResolver.listProperties(metadataResource)) {
			String name = property.getName();
			if (!alreadyPresentOptions.contains(name) && name.startsWith(startsWith)) {
				collector.add(proposals.withSeparateTokens("--" + property.getName() + "=", property.getShortDescription()));
			}
		}
		// For other properties (including WL'ed in full form), use their id
		if (detailLevel > 1) {
			for (ConfigurationMetadataProperty property : metadataResolver.listProperties(metadataResource, true)) {
				String id = property.getId();
				if (!alreadyPresentOptions.contains(id) && id.startsWith(startsWith)) {
					collector.add(proposals.withSeparateTokens("--" + property.getId() + "=", property.getShortDescription()));
				}
			}
		}
	}

	AppRegistration findAppRegistration(String appName, ApplicationType... appTypes){
		AppRegistration lastAppRegistration = null;
		for (ApplicationType appType : appTypes) {
			lastAppRegistration = this.appRegistry.find(appName, appType);
			if (lastAppRegistration != null) {
				return lastAppRegistration;
			}
		}
		return null;
	}

	static String computeStartsWith(CheckPointedParseException exception){
		List<Token> tokens = exception.getTokens();
		int tokenPointer = tokens.size() - 1;
		while (!tokens.get(tokenPointer - 1).isKind(TokenKind.DOUBLE_MINUS)) {
			tokenPointer--;
		}
		StringBuilder startsWithBuffer = null;
		for (startsWithBuffer = new StringBuilder(); tokenPointer < tokens.size(); tokenPointer++) {
			Token t = tokens.get(tokenPointer);
			if (t.isIdentifier()) {
				startsWithBuffer.append(t.stringValue());
			}
			else {
				startsWithBuffer.append(t.getKind().getTokenChars());
			}
		}
		String startsWith = startsWithBuffer.toString();
		return startsWith;
	}
}
