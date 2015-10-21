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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistration;
import org.springframework.cloud.dataflow.artifact.registry.ArtifactRegistry;
import org.springframework.cloud.dataflow.core.ArtifactType;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.core.dsl.Token;
import org.springframework.cloud.dataflow.core.dsl.TokenKind;
import org.springframework.cloud.stream.configuration.metadata.ModuleConfigurationMetadataResolver;
import org.springframework.cloud.stream.module.resolver.ModuleResolver;
import org.springframework.core.io.Resource;

/**
 * Attempts to fill in possible values after a {@literal --foo=prefix}
 * (syntactically valid) construct in the DSL.
 *
 * @author Eric Bottard
 */
public class ConfigurationPropertyValueHintExpansionStrategy implements ExpansionStrategy {

	private final ArtifactRegistry artifactRegistry;

	private final ModuleResolver moduleResolver;

	private final ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver;

	@Autowired
	private ValueHintProvider[] valueHintProviders = new ValueHintProvider[0];

	ConfigurationPropertyValueHintExpansionStrategy(ArtifactRegistry artifactRegistry,
			ModuleResolver moduleResolver,
			ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver) {
		this.artifactRegistry = artifactRegistry;
		this.moduleResolver = moduleResolver;
		this.moduleConfigurationMetadataResolver = moduleConfigurationMetadataResolver;
	}

	@Override
	public boolean addProposals(String text, StreamDefinition parseResult,
			int detailLevel, List<CompletionProposal> collector) {
		Set<String> propertyNames = new HashSet<>(parseResult.getDeploymentOrderIterator()
				.next().getParameters().keySet());
		propertyNames.removeAll(CompletionUtils.IMPLICIT_PARAMETER_NAMES);
		if (text.endsWith(" ") || propertyNames.isEmpty()) {
			return false;
		}

		String propertyName = recoverPropertyName(text);

		ModuleDefinition lastModule = parseResult.getDeploymentOrderIterator().next();
		String alreadyTyped = lastModule.getParameters().get(propertyName);

		String lastModuleName = lastModule.getName();
		ArtifactRegistration lastArtifactRegistration = null;
		for (ArtifactType moduleType : CompletionUtils.determinePotentialTypes(lastModule)) {
			lastArtifactRegistration = artifactRegistry.find(lastModuleName, moduleType);
			if (lastArtifactRegistration != null) {
				break;
			}
		}

		if (lastArtifactRegistration == null) {
			// Not a valid module name, do nothing
			return false;
		}
		Resource moduleResource = moduleResolver.resolve(CompletionUtils
				.fromModuleCoordinates(lastArtifactRegistration.getCoordinates()));

		CompletionProposal.Factory proposals = expanding(text);

		for (ConfigurationMetadataProperty property : moduleConfigurationMetadataResolver
				.listProperties(moduleResource)) {
			if (property.getId().equals(propertyName)) {
				ClassLoader classLoader = null;
				try {

					File file = moduleResource.getFile();
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
