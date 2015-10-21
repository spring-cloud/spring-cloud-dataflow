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

import static org.springframework.cloud.dataflow.core.ArtifactType.*;

import java.util.List;

import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.module.registry.ArtifactRegistration;
import org.springframework.cloud.dataflow.module.registry.ArtifactRegistry;

/**
 * Proposes module names when the user has typed a named channel redirection.
 *
 * @author Eric Bottard
 */
class ChannelNameYieldsModulesRecoveryStrategy extends
		StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final ArtifactRegistry moduleRegistry;

	public ChannelNameYieldsModulesRecoveryStrategy(ArtifactRegistry moduleRegistry) {
		super(CheckPointedParseException.class, "queue:foo >", "queue:foo > ");
		this.moduleRegistry = moduleRegistry;
	}

	@Override
	public boolean shouldTrigger(String dslStart, Exception exception) {
		if( !super.shouldTrigger(dslStart, exception)) {
			return false;
		}
		// Cast is safe from call to super.
		// Backtracking would return even before the named channel
		return ((CheckPointedParseException)exception).getExpressionStringUntilCheckpoint().trim().isEmpty();
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception,
			int detailLevel, List<CompletionProposal> proposals) {
		CompletionProposal.Factory completionFactory = CompletionProposal.expanding(dsl);
		for (ArtifactRegistration moduleRegistration : moduleRegistry.findAll()) {
			if (moduleRegistration.getType() == processor || moduleRegistration.getType() == sink) {
				proposals.add(completionFactory.withSeparateTokens(moduleRegistration.getName(),
						"Wire named channel into a " + moduleRegistration.getType() + " module"));
			}
		}
	}

}
