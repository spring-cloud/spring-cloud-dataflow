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

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;

/**
 * Provides code completion on a (maybe ill-formed) stream definition.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class StreamCompletionProvider {

	private final List<RecoveryStrategy<?>> completionRecoveryStrategies;

	private final List<ExpansionStrategy> completionExpansionStrategies;

	private final StreamDefinitionService streamDefinitionService;

	public StreamCompletionProvider(List<RecoveryStrategy<?>> completionRecoveryStrategies,
			List<ExpansionStrategy> completionExpansionStrategies,
			StreamDefinitionService streamDefinitionService) {
		this.completionRecoveryStrategies = new ArrayList<>(completionRecoveryStrategies);
		this.completionExpansionStrategies = new ArrayList<>(completionExpansionStrategies);
		this.streamDefinitionService = streamDefinitionService;
	}

	/*
	 * Attempt to parse the text the user has already typed in. This either succeeds, in
	 * which case we may propose to expand what she has typed, or it fails (most likely
	 * because this is not well formed), in which case we try to recover from the parsing
	 * failure and still add proposals.
	 */
	@SuppressWarnings("unchecked")
	public List<CompletionProposal> complete(String dslStart, int detailLevel) {
		List<CompletionProposal> collector = new ArrayList<>();

		StreamDefinition streamDefinition = null;
		try {
			streamDefinition = new StreamDefinition("__dummy", dslStart);
			this.streamDefinitionService.parse(streamDefinition);
		}
		catch (Exception recoverable) {
			for (RecoveryStrategy strategy : completionRecoveryStrategies) {
				if (strategy.shouldTrigger(dslStart, recoverable)) {
					strategy.addProposals(dslStart, recoverable, detailLevel, collector);
				}
			}

			return collector;
		}

		for (ExpansionStrategy strategy : completionExpansionStrategies) {
			strategy.addProposals(dslStart, streamDefinition, detailLevel, collector);
		}
		return collector;
	}

	public void addCompletionRecoveryStrategy(RecoveryStrategy recoveryStrategy) {
		this.completionRecoveryStrategies.add(recoveryStrategy);
	}
}
