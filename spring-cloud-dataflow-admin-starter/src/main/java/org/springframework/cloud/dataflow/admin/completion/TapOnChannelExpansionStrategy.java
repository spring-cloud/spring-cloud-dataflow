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

package org.springframework.cloud.dataflow.admin.completion;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.admin.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.completion.CompletionProposal;
import org.springframework.cloud.dataflow.completion.RecoveryStrategy;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.DSLMessage;
import org.springframework.cloud.dataflow.core.dsl.ParseException;

/**
 * Expands constructs that start with {@literal tap:stream} to add stream and maybe module identifiers.
 *
 * <p>Lives in this package as it needs access to a {@link StreamDefinitionRepository}.</p>
 *
 * @author Eric Bottard
 */
public class TapOnChannelExpansionStrategy implements RecoveryStrategy<ParseException> {

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Override
	public boolean shouldTrigger(String dslStart, Exception exception) {
		return dslStart.startsWith("tap:stream:") && !dslStart.contains(" ") &&
				((ParseException)exception).getMessageCode() == DSLMessage.EXPECTED_WHITESPACE_AFTER_LABEL_COLON;
	}

	@Override
	public void addProposals(String dsl, ParseException exception, int detailLevel, List<CompletionProposal> collector) {
		String streamName = dsl.substring("tap:stream:".length());
		String moduleName = "";
		if (streamName.contains(".")) {
			String[] splits = streamName.split("\\.");
			streamName = splits[0];
			moduleName = splits[1];
		}

		StreamDefinition streamDefinition = streamDefinitionRepository.findOne(streamName);
		// User has started to type a module name, or at least the stream name is valid
		if (streamDefinition != null) {
			CompletionProposal.Factory proposals = CompletionProposal.expanding("tap:stream:" + streamName + ".");
			for (ModuleDefinition moduleDefinition : streamDefinition.getModuleDefinitions()) {
				if (moduleDefinition.getLabel().startsWith(moduleName)) {
					collector.add(proposals.withSuffix(moduleDefinition.getLabel()));
				}
			}
		} // Stream name is not valid (yet). Try to use it as a prefix
		else {
			CompletionProposal.Factory proposals = CompletionProposal.expanding("tap:stream:");
			for (StreamDefinition stream : streamDefinitionRepository.findAll()) {
				if (stream.getName().startsWith(streamName)) {
					collector.add(proposals.withSuffix(stream.getName()));
				}
			}
		}

	}
}
