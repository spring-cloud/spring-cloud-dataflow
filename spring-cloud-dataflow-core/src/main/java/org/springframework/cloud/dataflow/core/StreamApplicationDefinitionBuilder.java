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

package org.springframework.cloud.dataflow.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.dataflow.core.StreamAppDefinition.Builder;
import org.springframework.cloud.dataflow.core.dsl.AppNode;
import org.springframework.cloud.dataflow.core.dsl.ArgumentNode;
import org.springframework.cloud.dataflow.core.dsl.SinkDestinationNode;
import org.springframework.cloud.dataflow.core.dsl.SourceDestinationNode;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Builds a list of {@link StreamAppDefinition StreamAppDefinitions} out of a parsed
 * {@link StreamNode}.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Andy Clement
 * @author Eric Bottard
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
class StreamApplicationDefinitionBuilder {

	private static final String CONSUMER_GROUP_PARAMETER = "group";

	private final String streamName;

	private final StreamNode streamNode;

	/**
	 * Create a StreamAppDefinitionBuilder for the given stream.
	 *
	 * @param streamName the name of the stream
	 * @param streamNode the AST construct representing the stream
	 */
	StreamApplicationDefinitionBuilder(String streamName, StreamNode streamNode) {
		Assert.hasText(streamName, "streamName is required");
		Assert.notNull(streamNode, "streamNode must not be null");
		this.streamName = streamName;
		this.streamNode = streamNode;
	}

	/**
	 * Build a list of {@link StreamAppDefinition}s out of the parsed StreamNode.
	 */
	public List<StreamAppDefinition> build() {
		SourceDestinationNode sourceDestination = streamNode.getSourceDestinationNode();
		SinkDestinationNode sinkDestination = streamNode.getSinkDestinationNode();

		Deque<StreamAppDefinition.Builder> builders = new LinkedList<>();
		List<AppNode> appNodes = streamNode.getAppNodes();
		for (int m = appNodes.size() - 1; m >= 0; m--) {
			AppNode appNode = appNodes.get(m);
			StreamAppDefinition.Builder builder = (Builder) new StreamAppDefinition.Builder()
					.setRegisteredAppName(appNode.getName()).setLabel(appNode.getLabelName());
			if (appNode.hasArguments()) {
				ArgumentNode[] arguments = appNode.getArguments();
				for (ArgumentNode argument : arguments) {
					if (argument.getName().equalsIgnoreCase("inputType")) {
						builder.setProperty(BindingPropertyKeys.INPUT_CONTENT_TYPE, argument.getValue());
					}
					else if (argument.getName().equalsIgnoreCase("outputType")) {
						builder.setProperty(BindingPropertyKeys.OUTPUT_CONTENT_TYPE, argument.getValue());
					}
					else {
						String value = StringUtils.hasText(argument.getValue()) ? argument.getValue() : "\\\"\\\"";
						builder.setProperty(argument.getName(), value);
					}
				}
			}
			if (appNode.isUnboundStreamApp()) {
				builder.setApplicationType(ApplicationType.app);
			}
			else {
				if (m == 0) {
					if (sourceDestination == null) {
						builder.setApplicationType(ApplicationType.source);
					}
					else {
						if (appNodes.size() == 1 && sinkDestination == null) {
							builder.setApplicationType(ApplicationType.sink);
						}
						else {
							builder.setApplicationType(ApplicationType.processor);
						}
					}
				}
				else {
					if (m < appNodes.size() - 1 || sinkDestination != null) {
						builder.setApplicationType(ApplicationType.processor);
					}
					else {
						builder.setApplicationType(ApplicationType.sink);
					}

				}
			}
			// Add binding property keys only if the app is a source, processor or sink.
			if (!appNode.isUnboundStreamApp()) {
				if (m > 0) {
					builder.setProperty(BindingPropertyKeys.INPUT_DESTINATION,
							String.format("%s.%s", streamName, appNodes.get(m - 1).getLabelName()));
					builder.setProperty(BindingPropertyKeys.INPUT_GROUP, streamName);
				}
				if (m < appNodes.size() - 1) {
					builder.setProperty(BindingPropertyKeys.OUTPUT_DESTINATION,
							String.format("%s.%s", streamName, appNode.getLabelName()));
					builder.setProperty(BindingPropertyKeys.OUTPUT_REQUIRED_GROUPS, streamName);
				}
			}
			builders.add(builder);
		}
		if (sourceDestination != null) {
			StreamAppDefinition.Builder sourceAppBuilder = builders.getLast();
			sourceAppBuilder.setProperty(BindingPropertyKeys.INPUT_DESTINATION, sourceDestination.getDestinationName());
			String consumerGroupName = streamName;
			if (sourceDestination.getArguments() != null) {
				ArgumentNode[] argumentNodes = sourceDestination.getArguments();
				for (ArgumentNode argument : argumentNodes) {
					if (argument.getName().equalsIgnoreCase(CONSUMER_GROUP_PARAMETER)) {
						consumerGroupName = argument.getValue();
					}
				}
			}
			sourceAppBuilder.setProperty(BindingPropertyKeys.INPUT_GROUP, consumerGroupName);
		}
		if (sinkDestination != null) {
			builders.getFirst().setProperty(BindingPropertyKeys.OUTPUT_DESTINATION,
					sinkDestination.getDestinationName());
		}
		List<StreamAppDefinition> streamAppDefinitions = new ArrayList<>(builders.size());
		for (StreamAppDefinition.Builder builder : builders) {
			streamAppDefinitions.add(builder.build(streamName));
		}
		return streamAppDefinitions;
	}
}
