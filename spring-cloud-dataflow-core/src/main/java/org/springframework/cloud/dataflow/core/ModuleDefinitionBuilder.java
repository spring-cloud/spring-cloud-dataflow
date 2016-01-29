/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.dataflow.core.dsl.ArgumentNode;
import org.springframework.cloud.dataflow.core.dsl.ModuleNode;
import org.springframework.cloud.dataflow.core.dsl.SinkChannelNode;
import org.springframework.cloud.dataflow.core.dsl.SourceChannelNode;
import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.util.Assert;

/**
 * Builds a list of {@link ModuleDefinition ModuleDefinitions} out of a parsed {@link StreamNode}.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Andy Clement
 * @author Eric Bottard
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
class ModuleDefinitionBuilder {

	public static final String DEFAULT_CONSUMER_GROUP_NAME = "default";

	private final String streamName;

	private final StreamNode streamNode;

	/**
	 * Create a ModuleDefinitionBuilder for the given stream.
	 *
	 * @param streamName the name of the stream
	 * @param streamNode the AST construct representing the stream
	 */
	public ModuleDefinitionBuilder(String streamName, StreamNode streamNode) {
		Assert.hasText(streamName, "streamName is required");
		Assert.notNull(streamNode, "streamNode must not be null");
		this.streamName = streamName;
		this.streamNode = streamNode;
	}

	/**
	 * Build a list of ModuleDefinitions out of the parsed StreamNode.
	 */
	public List<ModuleDefinition> build() {
		Deque<ModuleDefinition.Builder> builders = new LinkedList<>();
		List<ModuleNode> moduleNodes = streamNode.getModuleNodes();
		for (int m = moduleNodes.size() - 1; m >= 0; m--) {
			ModuleNode moduleNode = moduleNodes.get(m);
			ModuleDefinition.Builder builder =
					new ModuleDefinition.Builder()
							.setGroup(streamName)
							.setName(moduleNode.getName())
							.setLabel(moduleNode.getLabelName());
			if (moduleNode.hasArguments()) {
				ArgumentNode[] arguments = moduleNode.getArguments();
				for (ArgumentNode argument : arguments) {
					if (argument.getName().equalsIgnoreCase("inputType")) {
						builder.setParameter(BindingProperties.INPUT_TYPE_KEY, argument.getValue());
					}
					else if (argument.getName().equalsIgnoreCase("outputType")) {
						builder.setParameter(BindingProperties.OUTPUT_TYPE_KEY, argument.getValue());
					}
					else {
						builder.setParameter(argument.getName(), argument.getValue());
					}
				}
			}
			if (m > 0) {
				builder.setParameter(BindingProperties.INPUT_BINDING_KEY,
						String.format("%s.%s", streamName, moduleNodes.get(m - 1).getLabelName()));
				builder.setParameter(BindingProperties.INPUT_GROUP_KEY, DEFAULT_CONSUMER_GROUP_NAME);
				builder.setParameter(BindingProperties.INPUT_DURABLE_SUBSCRIPTION_KEY, "true");
			}
			if (m < moduleNodes.size() - 1) {
				builder.setParameter(BindingProperties.OUTPUT_BINDING_KEY,
						String.format("%s.%s", streamName, moduleNode.getLabelName()));
			}
			builders.add(builder);
		}
		SourceChannelNode sourceChannel = streamNode.getSourceChannelNode();
		if (sourceChannel != null) {
			builders.getLast().setParameter(BindingProperties.INPUT_BINDING_KEY, sourceChannel.getChannelName());
			builders.getLast().setParameter(BindingProperties.INPUT_GROUP_KEY, streamName);
			builders.getLast().setParameter(BindingProperties.INPUT_DURABLE_SUBSCRIPTION_KEY, "true");
		}
		SinkChannelNode sinkChannel = streamNode.getSinkChannelNode();
		if (sinkChannel != null) {
			builders.getFirst().setParameter(BindingProperties.OUTPUT_BINDING_KEY, sinkChannel.getChannelName());
		}
		List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>(builders.size());
		for (ModuleDefinition.Builder builder : builders) {
			moduleDefinitions.add(builder.build());
		}
		return moduleDefinitions;
	}
}
