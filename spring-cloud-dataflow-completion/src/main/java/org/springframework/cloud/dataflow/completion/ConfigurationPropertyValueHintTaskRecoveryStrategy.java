/*
 * Copyright 2015-2016 the original author or authors.
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
import java.util.List;

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
 * Attempts to fill in possible values after a {@literal --foo=} dangling construct in the DSL.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 */
public class ConfigurationPropertyValueHintTaskRecoveryStrategy extends StacktraceFingerprintingTaskRecoveryStrategy<CheckPointedParseException> {

	private final AppRegistry appRegistry;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	@Autowired
	private ValueHintProvider[] valueHintProviders = new ValueHintProvider[0];

	ConfigurationPropertyValueHintTaskRecoveryStrategy(AppRegistry appRegistry, ApplicationConfigurationMetadataResolver metadataResolver) {
		super(CheckPointedParseException.class, "foo --bar=");
		this.appRegistry = appRegistry;
		this.metadataResolver = metadataResolver;
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception, int detailLevel, List<CompletionProposal> collector) {

		String propertyName = recoverPropertyName(exception);

		AppRegistration appRegistration = lookupLastApp(exception);

		if (appRegistration == null) {
			// Not a valid app name, do nothing
			return;
		}
		Resource appResource = appRegistration.getResource();

		CompletionProposal.Factory proposals = expanding(dsl);

		List<ConfigurationMetadataProperty> whiteList = metadataResolver.listProperties(appResource);

		URLClassLoader classLoader = null;
		try {
			for (ConfigurationMetadataProperty property : metadataResolver.listProperties(appResource, true)) {
				if (CompletionUtils.isMatchingProperty(propertyName, property, whiteList)) {
					if (classLoader == null) {
						classLoader = metadataResolver.createAppClassLoader(appResource);
					}
					for (ValueHintProvider valueHintProvider : valueHintProviders) {
						for (ValueHint valueHint : valueHintProvider.generateValueHints(property, classLoader)) {
							collector.add(proposals.withSuffix(String.valueOf(valueHint.getValue()), valueHint.getShortDescription()));
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
	}

	private AppRegistration lookupLastApp(CheckPointedParseException exception) {
		String safe = exception.getExpressionStringUntilCheckpoint();
		TaskDefinition taskDefinition = new TaskDefinition("__dummy", safe);
		String appName = taskDefinition.getRegisteredAppName();
		AppRegistration appRegistration = this.appRegistry.find(appName, ApplicationType.task);
		return appRegistration;
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
