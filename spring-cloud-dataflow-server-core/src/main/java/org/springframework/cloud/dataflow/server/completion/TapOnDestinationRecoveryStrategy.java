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

package org.springframework.cloud.dataflow.server.completion;

import java.util.List;

import org.springframework.cloud.dataflow.completion.CompletionProposal;
import org.springframework.cloud.dataflow.completion.RecoveryStrategy;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.dsl.DSLMessage;
import org.springframework.cloud.dataflow.core.dsl.ParseException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.util.StringUtils;

/**
 * Expands constructs that start with {@literal :} to add stream name and app identifiers.
 * Lives in this package as it needs access to a {@link StreamDefinitionRepository}.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
public class TapOnDestinationRecoveryStrategy implements RecoveryStrategy<ParseException> {

	private final StreamDefinitionRepository streamDefinitionRepository;

	private final StreamDefinitionService streamDefinitionService;

	public TapOnDestinationRecoveryStrategy(StreamDefinitionRepository streamDefinitionRepository,
			StreamDefinitionService streamDefinitionService) {
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.streamDefinitionService = streamDefinitionService;
	}

	@Override
	public boolean shouldTrigger(String dslStart, Exception exception) {
		return dslStart.startsWith(":") && !dslStart.contains(" ")
				&& (((ParseException) exception).getMessageCode() == DSLMessage.EXPECTED_STREAM_NAME_AFTER_LABEL_COLON
						|| ((ParseException) exception).getMessageCode() == DSLMessage.EXPECTED_APPNAME);
	}

	@Override
	public void addProposals(String dsl, ParseException exception, int detailLevel,
			List<CompletionProposal> collector) {
		String streamName = dsl.substring(":".length());
		String appName = "";
		if (streamName.contains(".")) {
			String[] splits = streamName.split("\\.");
			streamName = splits[0];
			if (splits.length > 1) {
				appName = splits[1];
			}
		}

		StreamDefinition streamDefinition = null;
		if (StringUtils.hasText(streamName)) {
			// streamDefinition = streamDefinitionRepository.findOne(streamName);
			streamDefinition = streamDefinitionRepository.findById(streamName).orElse(null);
		}
		// User has started to type an app name, or at least the stream name is valid
		if (streamDefinition != null) {
			CompletionProposal.Factory proposals = CompletionProposal.expanding(":" + streamName + ".");
			for (StreamAppDefinition streamAppDefinition : this.streamDefinitionService.getAppDefinitions(streamDefinition)) {
				ApplicationType applicationType = streamAppDefinition.getApplicationType();
				if (streamAppDefinition.getName().startsWith(appName)
						&& !applicationType.equals(ApplicationType.sink)) {
					collector.add(proposals.withSuffix(streamAppDefinition.getName()));
				}
			}
		} // Stream name is not valid (yet). Try to use it as a prefix
		else {
			CompletionProposal.Factory proposals = CompletionProposal.expanding(":");
			for (StreamDefinition stream : streamDefinitionRepository.findAll()) {
				if (stream.getName().startsWith(streamName)) {
					collector.add(proposals.withSuffix(stream.getName()));
				}
			}
		}

	}
}
