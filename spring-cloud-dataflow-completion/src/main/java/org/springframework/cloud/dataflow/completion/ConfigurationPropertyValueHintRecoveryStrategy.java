/*
 * Copyright 2015 the original author or authors.
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

import static org.springframework.cloud.dataflow.completion.CompletionProposal.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.core.dsl.Token;
import org.springframework.cloud.dataflow.core.dsl.TokenKind;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistration;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistry;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolver;
import org.springframework.cloud.stream.module.launcher.ModuleJarLauncher;
import org.springframework.cloud.stream.module.resolver.ModuleResolver;
import org.springframework.core.io.Resource;

/**
 * Attempts to fill in possible values after a {@literal --foo=} dangling construct in the DSL.
 *
 * @author Eric Bottard
 */
public class ConfigurationPropertyValueHintRecoveryStrategy extends StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final ModuleRegistry moduleRegistry;

	private final ModuleResolver moduleResolver;

	private final ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver;

	@Autowired
	private ValueHintProvider[] valueHintProviders = new ValueHintProvider[0];

	ConfigurationPropertyValueHintRecoveryStrategy(ModuleRegistry moduleRegistry, ModuleResolver moduleResolver, ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver) {
		super(CheckPointedParseException.class, "foo --bar=", "foo | wizz --bar=");
		this.moduleRegistry = moduleRegistry;
		this.moduleResolver = moduleResolver;
		this.moduleConfigurationMetadataResolver = moduleConfigurationMetadataResolver;
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception, int detailLevel, List<CompletionProposal> collector) {

		String propertyName = recoverPropertyName(exception);

		ModuleRegistration lastModuleRegistration = lookupLastModule(exception);

		if (lastModuleRegistration == null) {
			// Not a valid module name, do nothing
			return;
		}
		Resource moduleResource = moduleResolver.resolve(CompletionUtils.fromModuleCoordinates(lastModuleRegistration.getCoordinates()));

		CompletionProposal.Factory proposals = expanding(dsl);

		for (ConfigurationMetadataProperty property : moduleConfigurationMetadataResolver.listProperties(moduleResource)) {
			if (property.getId().equals(propertyName)) {
				ClassLoader classLoader = null;
				try {

					File moduleFile = moduleResource.getFile();
					Archive jarFileArchive = moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive(moduleFile);
					classLoader = new ClassLoaderExposingJarLauncher(jarFileArchive).createClassLoader();

					for (ValueHintProvider valueHintProvider : valueHintProviders) {
						for (ValueHint valueHint : valueHintProvider.guessValueHints(property, classLoader)) {
							collector.add(proposals.withSuffix(String.valueOf(valueHint.getValue()), valueHint.getShortDescription()));
						}
					}
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				} finally {
					if (classLoader instanceof Closeable) {
						try {
							((Closeable)classLoader).close();
						}
						catch (IOException e) {
							// ignore
						}
					}
				}
			}

		}

	}

	private ModuleRegistration lookupLastModule(CheckPointedParseException exception) {
		String safe = exception.getExpressionStringUntilCheckpoint();
		StreamDefinition streamDefinition = new StreamDefinition("__dummy", safe);
		ModuleDefinition lastModule = streamDefinition.getDeploymentOrderIterator().next();

		String lastModuleName = lastModule.getName();
		ModuleRegistration lastModuleRegistration = null;
		for (ModuleType moduleType : CompletionUtils.determinePotentialTypes(lastModule)) {
			lastModuleRegistration = moduleRegistry.find(lastModuleName, moduleType);
			if (lastModuleRegistration != null) {
				break;
			}
		}
		return lastModuleRegistration;
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
