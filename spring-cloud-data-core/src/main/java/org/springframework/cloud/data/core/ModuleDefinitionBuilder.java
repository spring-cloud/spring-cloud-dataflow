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

package org.springframework.cloud.data.core;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.data.core.dsl.ArgumentNode;
import org.springframework.cloud.data.core.dsl.ModuleNode;
import org.springframework.cloud.data.core.dsl.SinkChannelNode;
import org.springframework.cloud.data.core.dsl.SourceChannelNode;
import org.springframework.cloud.data.core.dsl.StreamNode;
import org.springframework.util.Assert;

/**
 * Builds a list of {@link ModuleDefinition ModuleDefinitions} out of a parsed {@link StreamNode}.
 *
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Andy Clement
 * @author Eric Bottard
 */
class ModuleDefinitionBuilder {

	/**
	 * Default name for input channel.
	 */
	private static final String INPUT_CHANNEL = "input";

	/**
	 * Default name for output channel.
	 */
	private static final String OUTPUT_CHANNEL = "output";

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
					builder.setParameter(argument.getName(), argument.getValue());
				}
			}
			if (m > 0) {
				builder.addBinding(INPUT_CHANNEL, String.format("%s.%d", streamName, m - 1));
			}
			if (m < moduleNodes.size() - 1) {
				builder.addBinding(OUTPUT_CHANNEL, String.format("%s.%d", streamName, m));
			}
			builders.add(builder);
		}
		SourceChannelNode sourceChannel = streamNode.getSourceChannelNode();
		if (sourceChannel != null) {
			builders.getLast().addBinding(INPUT_CHANNEL, sourceChannel.getChannelName());
		}
		SinkChannelNode sinkChannel = streamNode.getSinkChannelNode();
		if (sinkChannel != null) {
			builders.getFirst().addBinding(OUTPUT_CHANNEL, sinkChannel.getChannelName());
		}
		List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>(builders.size());
		for (ModuleDefinition.Builder builder : builders) {
			moduleDefinitions.add(builder.build());
		}
		return moduleDefinitions;
	}
}
