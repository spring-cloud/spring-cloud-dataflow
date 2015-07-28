/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.core.parser;


import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.data.core.ModuleDefinition;
import org.springframework.cloud.data.core.dsl.ArgumentNode;
import org.springframework.cloud.data.core.dsl.ModuleNode;
import org.springframework.cloud.data.core.dsl.SinkChannelNode;
import org.springframework.cloud.data.core.dsl.SourceChannelNode;
import org.springframework.cloud.data.core.dsl.StreamDslParser;
import org.springframework.cloud.data.core.dsl.StreamNode;

/**
 * Parser to convert a DSL string for a stream into a list of
 * {@link ModuleDefinition} objects that comprise the given stream.
 *
 * @author Andy Clement
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Eric Bottard
 * @since 1.0
 */
public class StreamDefinitionParser {

	/**
	 * Default name for input channel.
	 */
	// TODO: evaluate where this belongs
	public static final String INPUT_CHANNEL = "input";

	/**
	 * Default name for output channel.
	 */
	// TODO: evaluate where this belongs
	public static final String OUTPUT_CHANNEL = "output";

	/**
	 * Parse the provided stream DSL stream and return a list
	 * of {@link ModuleDefinition module definitions} that define
	 * the stream.
	 *
	 * @param name  stream name
	 * @param dsl   stream DSL text
	 * @return list of module definitions for the stream
	 */
	public List<ModuleDefinition> parse(String name, String dsl) {
		StreamDslParser parser = new StreamDslParser();
		return buildModuleDefinitions(name, parser.parse(name, dsl));
	}

	/**
	 * Build a list of ModuleDefinitions out of a parsed StreamNode.
	 *
	 * @param name the name of the definition unit
	 * @param stream the AST construct representing the definition
	 */
	private List<ModuleDefinition> buildModuleDefinitions(String name, StreamNode stream) {
		Deque<ModuleDefinition.Builder> builders = new LinkedList<>();

		List<ModuleNode> moduleNodes = stream.getModuleNodes();
		for (int m = moduleNodes.size() - 1; m >= 0; m--) {
			ModuleNode moduleNode = moduleNodes.get(m);
			ModuleDefinition.Builder builder =
					new ModuleDefinition.Builder()
							.setGroup(name)
							.setName(moduleNode.getName())
							.setLabel(moduleNode.getLabelName());
			if (moduleNode.hasArguments()) {
				ArgumentNode[] arguments = moduleNode.getArguments();
				for (ArgumentNode argument : arguments) {
					builder.setParameter(argument.getName(), argument.getValue());
				}
			}

			if (m > 0) {
				builder.addBinding(INPUT_CHANNEL, String.format("%s.%d", name, m - 1));
			}
			if (m < moduleNodes.size() - 1) {
				builder.addBinding(OUTPUT_CHANNEL, String.format("%s.%d", name, m));
			}

			builders.add(builder);
		}

		SourceChannelNode sourceChannel = stream.getSourceChannelNode();
		if (sourceChannel != null) {
			builders.getLast().addBinding(INPUT_CHANNEL, sourceChannel.getChannelName());
		}

		SinkChannelNode sinkChannel = stream.getSinkChannelNode();
		if (sinkChannel != null) {
			builders.getFirst().addBinding(OUTPUT_CHANNEL, sinkChannel.getChannelName());
		}

		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>(builders.size());
		for (ModuleDefinition.Builder builder : builders) {
			result.add(builder.build());
		}
		return result;
	}

}
