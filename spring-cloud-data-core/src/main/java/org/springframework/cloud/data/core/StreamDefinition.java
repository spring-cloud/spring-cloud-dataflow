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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
 */
public class StreamDefinition {

	/**
	 * Name of stream.
	 */
	private final String name;

	/**
	 * DSL definition for stream.
	 */
	private final String definition;

	/**
	 * Ordered list of {@link ModuleDefinition}s comprising this stream.
	 * The source is the first entry and the sink is the last entry.
	 */
	private final LinkedList<ModuleDefinition> modules;

	/**
	 * Construct a {@code StreamDefinition}.
	 *
	 * @param name       name of stream
	 * @param definition DSL definition for stream
	 * @param modules    ordered list of modules for stream
	 */
	private StreamDefinition(String name, String definition, List<ModuleDefinition> modules) {
		Assert.hasText(name, "name is required");
		Assert.hasText(definition, "definition is required");
		Assert.notEmpty(modules, "modules are required");
		this.name = name;
		this.definition = definition;
		this.modules = new LinkedList<>(modules);
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
	public String getDefinition() {
		return definition;
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
				.append(this.definition)
				.toString();
	}


	/**
	 * Builder object for {@link StreamDefinition} that supports
	 * fluent style configuration.
	 */
	public static class Builder {

		/**
		 * @see StreamDefinition#name
		 */
		private String name;

		/**
		 * @see StreamDefinition#modules
		 */
		private List<ModuleDefinition> modules = new ArrayList<>();

		/**
		 * @see StreamDefinition#definition
		 */
		private String definition;

		/**
		 * Set the stream name
		 *
		 * @param name name of stream
		 * @return this builder
		 */
		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		/**
		 * Set the DSL definition for the stream.
		 *
		 * @param definition DSL definition
		 * @return this builder
		 */
		public Builder setDefinition(String definition) {
			this.definition = definition;
			return this;
		}

		/**
		 * Add the provided {@link ModuleDefinition module definitions}
		 * in the order provided by the list
		 *
		 * @param modules module definitions for stream
		 * @return this builder
		 */
		public Builder addModuleDefinitions(List<ModuleDefinition> modules) {
			this.modules.addAll(modules);
			return this;
		}

		/**
		 * Append a {@link ModuleDefinition module definition} to the
		 * list of module definitions.
		 *
		 * @param module module definition to add to stream
		 * @return this builder
		 */
		public Builder addModuleDefinition(ModuleDefinition module) {
			this.modules.add(module);
			return this;
		}

		/**
		 * Create a new instance of {@link StreamDefinition}.
		 *
		 * @return new {@code StreamDefinition} instance
		 */
		public StreamDefinition build() {
			return new StreamDefinition(this.name, this.definition, this.modules);
		}
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
