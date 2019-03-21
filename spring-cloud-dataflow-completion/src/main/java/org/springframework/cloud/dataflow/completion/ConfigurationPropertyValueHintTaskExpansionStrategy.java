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

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.core.dsl.Token;
import org.springframework.cloud.dataflow.core.dsl.TokenKind;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.core.io.Resource;

/**
 * Attempts to fill in possible values after a {@literal --foo=prefix}
 * (syntactically valid) construct in the DSL.
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 */
public class ConfigurationPropertyValueHintTaskExpansionStrategy implements TaskExpansionStrategy {

	private final AppRegistry appRegistry;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	@Autowired
	private ValueHintProvider[] valueHintProviders = new ValueHintProvider[0];

	ConfigurationPropertyValueHintTaskExpansionStrategy(AppRegistry appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		this.appRegistry = appRegistry;
		this.metadataResolver = metadataResolver;
	}

	@Override
	public boolean addProposals(String text, TaskDefinition parseResult,
			int detailLevel, List<CompletionProposal> collector) {
		Set<String> propertyNames = new HashSet<>(parseResult.getProperties().keySet());
		propertyNames.removeAll(CompletionUtils.IMPLICIT_TASK_PARAMETER_NAMES);
		if (text.endsWith(" ") || propertyNames.isEmpty()) {
			return false;
		}

		String propertyName = recoverPropertyName(text);

		String alreadyTyped = parseResult.getProperties().get(propertyName);
		
		String appName = parseResult.getRegisteredAppName();
		AppRegistration appRegistration = appRegistry.find(appName, ApplicationType.task);
		if (appRegistration == null) {
			// Not a valid app name, do nothing
			return false;
		}
		Resource appResource = appRegistration.getResource();

		CompletionProposal.Factory proposals = expanding(text);

		List<ConfigurationMetadataProperty> allProps = metadataResolver.listProperties(appResource, true);
		List<ConfigurationMetadataProperty> whiteListedProps = metadataResolver.listProperties(appResource);

		URLClassLoader classLoader = null;
		try {
			for (ConfigurationMetadataProperty property : allProps) {
				if (CompletionUtils.isMatchingProperty(propertyName, property, whiteListedProps)) {
					if (classLoader == null) {
						classLoader = metadataResolver.createAppClassLoader(appResource);
					}
					for (ValueHintProvider valueHintProvider : valueHintProviders) {
						List<ValueHint> valueHints = valueHintProvider.generateValueHints(property, classLoader);
						if (!valueHints.isEmpty() && valueHintProvider.isExclusive(property)) {
							collector.clear();
						}
						for (ValueHint valueHint : valueHints) {
							String candidate = String.valueOf(valueHint.getValue());
							if (!candidate.equals(alreadyTyped) && candidate.startsWith(alreadyTyped)) {
								collector.add(proposals.withSuffix(candidate.substring(alreadyTyped.length()),
										valueHint.getShortDescription()));
							}
						}
						if (!valueHints.isEmpty() && valueHintProvider.isExclusive(property)) {
							return true;
						}
					}
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			if (classLoader != null) {
				try {
					classLoader.close();
				}
				catch (IOException e) {
					// ignore
				}
			}
		}

		return false;
	}

	// This may be the safest way to backtrack to the property name
	// to avoid dealing with escaped space characters, etc.
	private String recoverPropertyName(String text) {
		try {
			new TaskDefinition("__dummy", text + " --");
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
