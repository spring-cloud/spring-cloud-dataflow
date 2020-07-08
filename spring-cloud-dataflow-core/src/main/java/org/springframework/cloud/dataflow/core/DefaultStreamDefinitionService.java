/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;
import org.springframework.util.StringUtils;

/**
 * The default implementation of {@link StreamDefinitionService}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public class DefaultStreamDefinitionService implements StreamDefinitionService {

	/**
	 * Use the {@link StreamParser} to retrieve the {@link StreamNode} representation of the stream.
	 * @param streamDefinition the stream definition
	 * @return the StreamNode representation of the stream
	 */
	public StreamNode parse(StreamDefinition streamDefinition) {
		return new StreamParser(streamDefinition.getName(), streamDefinition.getDslText()).parse();
	}

	/**
	 * Return the ordered list of application definitions for this stream as a
	 * {@link List}. This allows for retrieval of application definitions in the stream by
	 * index. Application definitions are maintained in stream flow order (source is
	 * first, sink is last).
	 *
	 * @return list of application definitions for this stream definition
	 */
	public LinkedList<StreamAppDefinition> getAppDefinitions(StreamDefinition streamDefinition) {
		LinkedList<StreamAppDefinition> appDefinitions = new LinkedList<>();
		String streamName = streamDefinition.getName();
		for (StreamAppDefinition appDefinition : new StreamApplicationDefinitionBuilder(streamName, parse(streamDefinition)).build()) {
			appDefinitions.addFirst(appDefinition);
		}
		return appDefinitions;
	}

	public String constructDsl(String originalDslText, LinkedList<StreamAppDefinition> streamAppDefinitions) {
		StringBuilder dslBuilder = new StringBuilder();

		int appDefinitionIndex = 0;
		for (StreamAppDefinition appDefinition : streamAppDefinitions) {
			Map<String, String> props = appDefinition.getProperties();
			String inputDestination = props.get(BindingPropertyKeys.INPUT_DESTINATION);
			String outputDestination = props.get(BindingPropertyKeys.OUTPUT_DESTINATION);
			String inputGroup = props.get(BindingPropertyKeys.INPUT_GROUP);

			// Check for Input Named Destination
			if (appDefinitionIndex == 0 && StringUtils.hasText(inputDestination)) {
				dslBuilder.append(":").append(inputDestination);
				if (inputGroup != null && !inputGroup.equals(appDefinition.getStreamName())) {
					dslBuilder.append(" --group=").append(inputGroup);
				}
				dslBuilder.append(" > ");
			}

			// Add App Definition
			dslBuilder.append(appDefinition.getName());

			if (!appDefinition.getName().equals(appDefinition.getRegisteredAppName())) {
				dslBuilder.append(": ").append(appDefinition.getRegisteredAppName());
			}

			for (String propertyName : props.keySet()) {
				if (!dataFlowAddedProperties.contains(propertyName)) {
					String propertyValue = unescape(props.get(propertyName));
					dslBuilder.append(" --").append(propertyName).append("=").append(
							DefinitionUtils.escapeNewlines(DefinitionUtils.autoQuotes(propertyValue)));
				}
			}

			// Check for Output Named Destination
			if (appDefinitionIndex == (streamAppDefinitions.size() - 1)) {
				if (StringUtils.hasText(outputDestination)) {
					dslBuilder.append(" > ").append(":").append(outputDestination);
				}
			}
			else {
				if (appDefinition.getApplicationType() != ApplicationType.app) {
					dslBuilder.append(" | ");
				} else {
					dslBuilder.append(" || ");
				}
			}

			appDefinitionIndex++;
		}

		// Bridge dsl shortcut optimization
		String dsl = dslBuilder.toString().replace("> bridge >", ">");

		return dsl;
	}

	@Override
	public String redactDsl(StreamDefinition streamDefinition) {
		return this.constructDsl(streamDefinition.getDslText(), StreamDefinitionServiceUtils.sanitizeStreamAppDefinitions(this.getAppDefinitions(streamDefinition)));
	}

	private String unescape(String text) {
		return StringEscapeUtils.unescapeHtml(text);
	}

}
