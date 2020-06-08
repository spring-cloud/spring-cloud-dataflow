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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;
import org.springframework.util.StringUtils;

public class DefaultStreamDefinitionService implements StreamDefinitionService {


	private final static List<String> dataFlowAddedProperties = Arrays.asList(
			DataFlowPropertyKeys.STREAM_APP_TYPE,
			DataFlowPropertyKeys.STREAM_APP_LABEL,
			DataFlowPropertyKeys.STREAM_NAME,
			StreamPropertyKeys.METRICS_TRIGGER_INCLUDES,
			StreamPropertyKeys.METRICS_KEY,
			StreamPropertyKeys.METRICS_PROPERTIES,
			BindingPropertyKeys.INPUT_GROUP,
			BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS,
			BindingPropertyKeys.OUTPUT_DESTINATION,
			BindingPropertyKeys.INPUT_DESTINATION);


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

	/**
	 * Return an iterator that indicates the order of application deployments for this
	 * stream. The application definitions are returned in reverse order; i.e. the sink is
	 * returned first followed by the processors in reverse order followed by the source.
	 *
	 * @return iterator that iterates over the application definitions in deployment order
	 */
	public Iterator<StreamAppDefinition> getDeploymentOrderIterator(StreamDefinition streamDefinition) {
		return new ReadOnlyIterator<>(getAppDefinitions(streamDefinition).descendingIterator());
	}


	/**
	 * Redacts sensitive property values in a stream.
	 *
	 * @param streamDefinition the stream definition to sanitize
	 * @return Stream definition text that has sensitive data redacted.
	 */
	public String sanitizeStreamDefinition(StreamDefinition streamDefinition) {
		List<StreamAppDefinition> sanitizedAppDefinitions = getAppDefinitions(streamDefinition).stream()
				.map(app -> StreamAppDefinition.Builder
						.from(app)
						.setProperties(new ArgumentSanitizer().sanitizeProperties(app.getProperties()))
						.build(streamDefinition.getName())
				).collect(Collectors.toList());

		return toDsl(sanitizedAppDefinitions);
	}


	/**
	 * Redacts sensitive property values in a stream.
	 *
	 * @param streamDefinition the stream definition to sanitize
	 * @return Stream definition with the original DSL text that has sensitive data redacted.
	 */
	public String sanitizeOriginalStreamDsl(StreamDefinition streamDefinition) {
		return sanitizeStreamDefinition(new StreamDefinition(streamDefinition.getName(), streamDefinition.getOriginalDslText()));
	}

	/**
	 * Reverse engineers a {@link StreamDefinition} into a semantically equivalent DSL text representation.
	 * @param streamDefinition stream to be converted into DSL
	 * @return the textual DSL representation of the stream
	 */
	public String toDsl(StreamDefinition streamDefinition) {
		return toDsl(getAppDefinitions(streamDefinition));
	}


	/**
	 * Reverse engineers a stream, represented by ordered {@link StreamAppDefinition} list, into a semantically
	 * equivalent DSL text representation.
	 *
	 * @param appDefinitions ordered list of {@link StreamAppDefinition}'s that represent a single stream definition.
	 * @return the textual DSL representation of the stream, that if parsed should produce exactly
	 * the same {@link StreamAppDefinition} list.
	 */
	public String toDsl(List<StreamAppDefinition> appDefinitions) {
		StringBuilder dslBuilder = new StringBuilder();

		int appDefinitionIndex = 0;
		for (StreamAppDefinition appDefinition : appDefinitions) {
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
			if (appDefinitionIndex == (appDefinitions.size() - 1)) {
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


	private String unescape(String text) {
		return StringEscapeUtils.unescapeHtml(text);
	}


	/**
	 * Iterator that prevents mutation of its backing data structure.
	 *
	 * @param <T> the type of elements returned by this iterator
	 */
	private static class ReadOnlyIterator<T> implements Iterator<T> {
		private final Iterator<T> wrapped;

		ReadOnlyIterator(Iterator<T> wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public boolean hasNext() {
			return wrapped.hasNext();
		}

		@Override
		public T next() {
			return wrapped.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
