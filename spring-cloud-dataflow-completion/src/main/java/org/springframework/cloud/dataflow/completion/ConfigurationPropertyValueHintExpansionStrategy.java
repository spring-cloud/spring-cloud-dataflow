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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.core.dsl.Token;
import org.springframework.cloud.dataflow.core.dsl.TokenKind;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.core.io.Resource;

/**
 * Attempts to fill in possible values after a {@literal --foo=prefix}
 * (syntactically valid) construct in the DSL.
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class ConfigurationPropertyValueHintExpansionStrategy implements ExpansionStrategy {

	private final AppRegistry appRegistry;

	private final ApplicationConfigurationMetadataResolver metadataResolver;

	@Autowired
	private ValueHintProvider[] valueHintProviders = new ValueHintProvider[0];

	ConfigurationPropertyValueHintExpansionStrategy(AppRegistry appRegistry,
	                                                ApplicationConfigurationMetadataResolver metadataResolver) {
		this.appRegistry = appRegistry;
		this.metadataResolver = metadataResolver;
	}

	@Override
	public boolean addProposals(String text, StreamDefinition parseResult,
	                            int detailLevel, List<CompletionProposal> collector) {
		Set<String> propertyNames = new HashSet<>(parseResult.getDeploymentOrderIterator()
				.next().getProperties().keySet());
		propertyNames.removeAll(CompletionUtils.IMPLICIT_PARAMETER_NAMES);
		if (text.endsWith(" ") || propertyNames.isEmpty()) {
			return false;
		}

		String propertyName = recoverPropertyName(text);

		StreamAppDefinition lastApp = parseResult.getDeploymentOrderIterator().next();
		String alreadyTyped = lastApp.getProperties().get(propertyName);

		String lastAppName = lastApp.getName();
		AppRegistration lastAppRegistration = null;
		for (ApplicationType appType : CompletionUtils.determinePotentialTypes(lastApp)) {
			lastAppRegistration = appRegistry.find(lastAppName, appType);
			if (lastAppRegistration != null) {
				break;
			}
		}

		if (lastAppRegistration == null) {
			// Not a valid app name, do nothing
			return false;
		}
		Resource appResource = lastAppRegistration.getResource();

		CompletionProposal.Factory proposals = expanding(text);

		List<ConfigurationMetadataProperty> allProps = metadataResolver.listProperties(appResource, true);
		List<ConfigurationMetadataProperty> whiteListedProps = metadataResolver.listProperties(appResource);

		for (ConfigurationMetadataProperty property : allProps) {
			if (CompletionUtils.isMatchingProperty(propertyName, property, whiteListedProps)) {
				ClassLoader classLoader = null;
				try {
					File file = appResource.getFile();
					Archive jarFileArchive = file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file);
					classLoader = new ClassLoaderExposingJarLauncher(jarFileArchive).createClassLoader();

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
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					if (classLoader instanceof Closeable) {
						try {
							((Closeable) classLoader).close();
						}
						catch (IOException e) {
							// ignore
						}
					}
				}
			}
		}

		return false;
	}

	// This may be the safest way to backtrack to the property name
	// to avoid dealing with escaped space characters, etc.
	private String recoverPropertyName(String text) {
		try {
			new StreamDefinition("__dummy", text + " --");
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
