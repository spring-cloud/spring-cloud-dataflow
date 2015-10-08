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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistration;
import org.springframework.cloud.dataflow.module.registry.ModuleRegistry;

/**
 * Provides completions by finding modules whose name starts with a prefix (which was assumed to be a correct module
 * name, but wasn't).
 *
 * @author Eric Bottard
 */
public class UnfinishedModuleNameExpansionStrategy implements ExpansionStrategy {

	private final ModuleRegistry moduleRegistry;

	UnfinishedModuleNameExpansionStrategy(ModuleRegistry moduleRegistry) {
		this.moduleRegistry = moduleRegistry;
	}

	@Override
	public boolean shouldTrigger(String text, StreamDefinition parseResult) {
		ModuleDefinition lastModule = parseResult.getDeploymentOrderIterator().next();
		Set<String> parameterNames = new HashSet<>(lastModule.getParameters().keySet());
		parameterNames.removeAll(CompletionUtils.IMPLICIT_PARAMETER_NAMES);
		return parameterNames.isEmpty() && text.endsWith(lastModule.getName());
	}

	@Override
	public void addProposals(String text, StreamDefinition streamDefinition, int detailLevel, List<CompletionProposal> collector) {
		ModuleDefinition lastModule = streamDefinition.getDeploymentOrderIterator().next();

		String alreadyTyped = lastModule.getName();
		CompletionProposal.Factory proposals = CompletionProposal.expanding(text);

		List<ModuleType> validTypesAtThisPosition = Arrays.asList(CompletionUtils.inferType(lastModule));

		for (ModuleRegistration moduleRegistration : moduleRegistry.findAll()) {
			String candidateName = moduleRegistration.getName();
			if (validTypesAtThisPosition.contains(moduleRegistration.getType()) && !alreadyTyped.equals(candidateName) && candidateName.startsWith(alreadyTyped)) {
				String expansion = CompletionUtils.maybeQualifyWithLabel(moduleRegistration.getName(), streamDefinition);

				collector.add(proposals.withSuffix(expansion.substring(alreadyTyped.length())));
			}
		}

	}
}
