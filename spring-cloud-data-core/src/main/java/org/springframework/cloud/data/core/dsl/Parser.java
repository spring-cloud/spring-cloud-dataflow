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

package org.springframework.cloud.data.core.dsl;

import static org.springframework.cloud.data.core.dsl.XDDSLMessages.NAMED_CHANNELS_UNSUPPORTED_HERE;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.data.core.ModuleDefinition;

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
public class Parser {
	public static final String INPUT_CHANNEL = "input";

	public static final String OUTPUT_CHANNEL = "output";

	public List<ModuleDefinition> parse(String name, String dsl, ParsingContext context) {
		StreamConfigParser parser = new StreamConfigParser();
		StreamNode stream = parser.parse(name, dsl);
		return buildModuleDefinitions(name, dsl, context, stream, null);
	}

	/**
	 * todo: fix javadoc
	 * Build a list of ModuleDefinitions out of a parsed StreamNode. If an {@code errors}
	 * list is passed then the method will not exit on the first exception that occurs;
	 * instead it will record the problems in the accumulator and attempt to continue processing.
	 *
	 * @param name the name of the definition unit
	 * @param dsl the raw DSL text of the definition
	 * @param context the context in which parsing happens
	 * @param stream the AST construct representing the definition
	 * @param errors accumulates exceptions that occur during validation
	 */
	private List<ModuleDefinition> buildModuleDefinitions(String name, String dsl,
			ParsingContext context, StreamNode stream, List<Exception> errors) {
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
			builders.add(builder);
		}

		SourceChannelNode sourceChannel = stream.getSourceChannelNode();
		if (sourceChannel != null) {
			if (context.supportsNamedChannels()) {
				builders.getLast().addBinding(INPUT_CHANNEL, sourceChannel.getChannelName());
			}
			else {
				throw new StreamDefinitionException(dsl, sourceChannel.getStartPos(),
						NAMED_CHANNELS_UNSUPPORTED_HERE);
			}
		}

		SinkChannelNode sinkChannel = stream.getSinkChannelNode();
		if (sinkChannel != null) {
			if (context.supportsNamedChannels()) {
				builders.getFirst().addBinding(OUTPUT_CHANNEL, sinkChannel.getChannelName());
			}
			else {
				throw new StreamDefinitionException(dsl, sinkChannel.getChannelNode().getStartPos(),
						NAMED_CHANNELS_UNSUPPORTED_HERE);
			}
		}

		List<ModuleDefinition> result = new ArrayList<ModuleDefinition>(builders.size());
		for (ModuleDefinition.Builder builder : builders) {
			result.add(builder.build());
		}
		return result;
	}

}
