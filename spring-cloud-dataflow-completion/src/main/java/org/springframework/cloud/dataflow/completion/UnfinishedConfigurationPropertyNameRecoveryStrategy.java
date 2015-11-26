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

import static org.springframework.cloud.dataflow.completion.CompletionProposal.expanding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
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
 * Provides completions for the case where the user has started to type a
 * module configuration property name but it is not typed in full yet.
 * @author Eric Bottard
 */
public class UnfinishedConfigurationPropertyNameRecoveryStrategy
		extends StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final ArtifactRegistry artifactRegistry;

	private final ModuleResolver moduleResolver;

	private final ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver;

	UnfinishedConfigurationPropertyNameRecoveryStrategy(ArtifactRegistry artifactRegistry,
	                                                    ModuleResolver moduleResolver, ModuleConfigurationMetadataResolver moduleConfigurationMetadataResolver) {
		super(CheckPointedParseException.class, "file --foo", "file | bar --quick", "file --foo.", "file | bar --quick.");
		this.artifactRegistry = artifactRegistry;
		this.moduleResolver = moduleResolver;
		this.moduleConfigurationMetadataResolver = moduleConfigurationMetadataResolver;
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

		StreamDefinition streamDefinition = new StreamDefinition("__dummy", safe);
		ModuleDefinition lastModule = streamDefinition.getDeploymentOrderIterator().next();

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
			return;
		}
		Set<String> alreadyPresentOptions = new HashSet<>(lastModule.getParameters().keySet());

		Resource jarFile = moduleResolver.resolve(CompletionUtils
				.fromModuleCoordinates(lastArtifactRegistration.getCoordinates()));

		CompletionProposal.Factory proposals = expanding(safe);

		Set<String> prefixes = new HashSet<>();
		for (ConfigurationMetadataGroup group : moduleConfigurationMetadataResolver.listPropertyGroups(jarFile)) {
			String groupId = ConfigurationMetadataRepository.ROOT_GROUP.equals(group.getId()) ? "" : group.getId();
			int lastDot = buffer.lastIndexOf('.');
			String bufferWithoutEndingDot = lastDot > 0 ? buffer.substring(0, lastDot) : "";
			if (bufferWithoutEndingDot.equals(groupId)) {
				// User has typed in the group id, complete with property names
				for (ConfigurationMetadataProperty property : group.getProperties().values()) {
					if (!alreadyPresentOptions.contains(property.getId()) && property.getId().startsWith(buffer)) {
						collector.add(proposals.withSeparateTokens("--" + property.getId()
								+ "=", property.getShortDescription()));
					}
				}
			}
			else if (groupId.startsWith(buffer)) {
				// User has typed in a substring of the the group id, complete till the next dot
				int dot = groupId.indexOf('.', buffer.length());
				String prefix = dot > 0 ? groupId.substring(0, dot) : groupId;
				if (!prefixes.contains(prefix)) {
					prefixes.add(prefix);
					collector.add(proposals.withSeparateTokens("--" + prefix + ".", "Properties starting with '" + prefix + ".'"));
				}
			}
		}
	}
}
