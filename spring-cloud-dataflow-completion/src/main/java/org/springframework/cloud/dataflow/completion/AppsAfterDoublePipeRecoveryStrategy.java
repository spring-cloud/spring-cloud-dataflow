/*
 * Copyright 2018 the original author or authors.
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

import java.util.List;

import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;

/**
 * Provides completions for the case where the user has entered a comma symbol and an
 * unbound app reference is expected next.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Andy Clement
 */
public class AppsAfterDoublePipeRecoveryStrategy
		extends StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final AppRegistryService appRegistryService;

	AppsAfterDoublePipeRecoveryStrategy(AppRegistryService appRegistryService,
			StreamDefinitionService streamDefinitionService) {
		super(CheckPointedParseException.class, streamDefinitionService,"foo ||", "foo || ");
		this.appRegistryService = appRegistryService;
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception, int detailLevel,
			List<CompletionProposal> collector) {
		StreamDefinition streamDefinition = new StreamDefinition("__dummy",
				exception.getExpressionStringUntilCheckpoint());
		CompletionProposal.Factory proposals = CompletionProposal.expanding(dsl);
		for (AppRegistration appRegistration : appRegistryService.findAll()) {
			if (appRegistration.getType() == ApplicationType.app) {
				String expansion = CompletionUtils.maybeQualifyWithLabel(appRegistration.getName(),
						this.streamDefinitionService.getAppDefinitions(streamDefinition));
				collector.add(proposals.withSeparateTokens(expansion,
						"Continue stream definition with a " + appRegistration.getType()));
			}
		}
	}
}
