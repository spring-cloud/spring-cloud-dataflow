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
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.core.dsl.Token;
import org.springframework.cloud.dataflow.core.dsl.TokenKind;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.core.io.Resource;


/**
 * Provides completions for the case where the user has started to type
 * an app configuration property name but it is not typed in full yet.
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 */
public class UnfinishedConfigurationPropertyNameTaskRecoveryStrategy
		extends StacktraceFingerprintingTaskRecoveryStrategy<CheckPointedParseException> {

	private final AppRegistry appRegistry;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	UnfinishedConfigurationPropertyNameTaskRecoveryStrategy(AppRegistry appRegistry,
	                                                    ApplicationConfigurationMetadataResolver metadataResolver) {
		super(CheckPointedParseException.class, "file --foo", "file --foo.");
		this.appRegistry = appRegistry;
		this.metadataResolver = metadataResolver;
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception,
	                         int detailLevel, List<CompletionProposal> collector) {

		String safe = exception.getExpressionStringUntilCheckpoint();

		List<Token> tokens = exception.getTokens();
		int tokenPointer = tokens.size() - 1;
		while (!tokens.get(tokenPointer - 1).isKind(TokenKind.DOUBLE_MINUS)) {
			tokenPointer--;
		}
		StringBuilder builder = null;
		for (builder = new StringBuilder(); tokenPointer < tokens.size(); tokenPointer++) {
			Token t = tokens.get(tokenPointer);
			if (t.isIdentifier()) {
				builder.append(t.stringValue());
			}
			else {
				builder.append(t.getKind().getTokenChars());
			}
		}
		String buffer = builder.toString();

		TaskDefinition taskDefinition = new TaskDefinition("__dummy", safe);

		String lastAppName = taskDefinition.getRegisteredAppName();
		AppRegistration appRegistration = appRegistry.find(lastAppName, ApplicationType.task);
		if (appRegistration == null) {
			// Not a valid app name, do nothing
			return;
		}
		Set<String> alreadyPresentOptions = new HashSet<>(taskDefinition.getProperties().keySet());

		Resource jarFile = appRegistration.getResource();

		CompletionProposal.Factory proposals = expanding(safe);

		// For whitelisted properties, use their simple name
		for (ConfigurationMetadataProperty property : metadataResolver.listProperties(jarFile)) {
			String name = property.getName();
			if (!alreadyPresentOptions.contains(name) && name.startsWith(buffer)) {
				collector.add(proposals.withSeparateTokens("--" + name
						+ "=", property.getShortDescription()));
			}
		}

		// For other props, use their full id
		if (detailLevel > 1) {
			for (ConfigurationMetadataProperty property : metadataResolver.listProperties(jarFile, true)) {
				String id = property.getId();
				if (!alreadyPresentOptions.contains(id) && id.startsWith(buffer)) {
					collector.add(proposals.withSeparateTokens("--" + id
							+ "=", property.getShortDescription()));
				}
			}
		}
	}
}
