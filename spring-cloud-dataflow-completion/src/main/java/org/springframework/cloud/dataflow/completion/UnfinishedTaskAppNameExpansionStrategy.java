/*
 * Copyright 2016-2017 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;

/**
 * Provides completions by finding apps whose name starts with a prefix (which was assumed
 * to be a correct app name, but wasn't).
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 */
public class UnfinishedTaskAppNameExpansionStrategy implements TaskExpansionStrategy {

	private final AppRegistryService appRegistry;

	UnfinishedTaskAppNameExpansionStrategy(AppRegistryService appRegistry) {
		this.appRegistry = appRegistry;
	}

	@Override
	public boolean addProposals(String text, TaskDefinition taskDefinition, int detailLevel,
			List<CompletionProposal> collector) {

		Set<String> parameterNames = new HashSet<>(taskDefinition.getProperties().keySet());
		parameterNames.removeAll(CompletionUtils.IMPLICIT_TASK_PARAMETER_NAMES);
		if (!parameterNames.isEmpty() || !text.endsWith(taskDefinition.getRegisteredAppName())) {
			return false;
		}

		// Actually add completions

		String alreadyTyped = taskDefinition.getRegisteredAppName();
		CompletionProposal.Factory proposals = CompletionProposal.expanding(text);

		List<ApplicationType> validTypesAtThisPosition = Arrays.asList(ApplicationType.task);

		for (AppRegistration appRegistration : appRegistry.findAll()) {
			String candidateName = appRegistration.getName();
			if (validTypesAtThisPosition.contains(appRegistration.getType()) && !alreadyTyped.equals(candidateName)
					&& candidateName.startsWith(alreadyTyped)) {
				String expansion = appRegistration.getName();

				collector.add(proposals.withSuffix(expansion.substring(alreadyTyped.length())));
			}
		}
		return false;

	}
}
