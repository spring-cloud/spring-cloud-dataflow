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
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.cloud.data.core.dsl.ArgumentNode;
import org.springframework.cloud.data.core.dsl.ModuleNode;
import org.springframework.cloud.data.core.dsl.SinkChannelNode;
import org.springframework.cloud.data.core.dsl.SourceChannelNode;
import org.springframework.cloud.data.core.dsl.StreamDslParser;
import org.springframework.cloud.data.core.dsl.StreamNode;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Representation of a defined stream. A stream consists of an
 * ordered list of modules used to process data. Each module
 * may contain configuration options provided at the time of
 * stream creation.
 * <p>
 * This stream definition does not include any deployment
 * or runtime configuration for a stream.
 *
 * @see ModuleDefinition
 *
 * @author Patrick Peralta
 * @author Andy Clement
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class StreamDefinition {

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
	 * Name of stream.
	 */
	private final String name;

	/**
	 * DSL definition for stream.
	 */
	private final String dslText;

	/**
	 * Ordered list of {@link ModuleDefinition}s comprising this stream.
	 * The source is the first entry and the sink is the last entry.
	 */
	private final LinkedList<ModuleDefinition> modules;

	/**
	 * Construct a {@code StreamDefinition}.
	 *
	 * @param name     name of stream
	 * @param dslText  DSL definition for stream
	 */
	public StreamDefinition(String name, String dslText) {
		Assert.hasText(name, "name is required");
		Assert.hasText(dslText, "dslText is required");
		this.name = name;
		this.dslText = dslText;
		this.modules = new LinkedList<>();
		StreamNode streamNode = new StreamDslParser(name, dslText).parse();
		for (ModuleDefinition module : buildModuleDefinitions(name, streamNode)) {
			this.modules.addFirst(module);
		}
	}

	/**
	 * Return the name of this stream.
	 *
	 * @return stream name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the DSL definition for this stream.
	 *
	 * @return stream definition DSL
	 */
	public String getDslText() {
		return dslText;
	}

	/**
	 * Return the ordered list of modules for this stream as a {@link List}.
	 * This allows for retrieval of modules in the stream by index.
	 * Modules are maintained in stream flow order (source is first, sink is last).
	 *
	 * @return list of module descriptors for this stream definition
	 */
	public List<ModuleDefinition> getModuleDefinitions() {
		return Collections.unmodifiableList(modules);
	}

	/**
	 * Return an iterator that indicates the order of module deployments for this
	 * stream. The modules are returned in reverse order; i.e. the sink is returned
	 * first followed by the processors in reverse order followed by the
	 * source.
	 *
	 * @return iterator that iterates over the modules in deployment order
	 */
	public Iterator<ModuleDefinition> getDeploymentOrderIterator() {
		return new ReadOnlyIterator<>(modules.descendingIterator());
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("name", this.name)
				.append("definition", this.dslText)
				.toString();
	}


	/**
	 * Build a list of ModuleDefinitions out of a parsed StreamNode.
	 *
	 * @param name the name of the definition unit
	 * @param stream the AST construct representing the definition
	 */
	private static List<ModuleDefinition> buildModuleDefinitions(String name, StreamNode stream) {
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

	/**
	 * Iterator that prevents mutation of its backing data structure.
	 *
	 * @param <T> the type of elements returned by this iterator
	 */
	private static class ReadOnlyIterator<T> implements Iterator<T> {
		private final Iterator<T> wrapped;

		public ReadOnlyIterator(Iterator<T> wrapped) {
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
