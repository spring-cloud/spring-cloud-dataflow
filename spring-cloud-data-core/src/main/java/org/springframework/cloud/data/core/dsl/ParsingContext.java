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

import org.springframework.cloud.data.core.ModuleType;
import static org.springframework.cloud.data.core.ModuleType.*;

/**
 * Captures the context in which a parse operation is made.
 *
 * @author Eric Bottard
 */
public enum ParsingContext {

	/**
	 * A full stream definition, which ought to start with a source (or channel) and end with a sink (or channel).
	 */
	stream(true, true, source, processor, sink),

	/**
	 * A composed module, which starts or ends on a processor.
	 */
	// Read these vertically: either [source, processor, processor] or [processor, processor, sink]
	module(true, false, new ModuleType[] { source, processor },
			new ModuleType[] { processor /* ,processor */},
			new ModuleType[] { processor, sink }),

	/**
	 * For the purpose of DSL completion only, a (maybe unfinished) stream definition.
	 */
	partial_stream(false, true, new ModuleType[] { source },
			new ModuleType[] { processor },
			new ModuleType[] { processor, sink }),
	/**
	 * For the purpose of DSL completion only, a (maybe unfinished) composed module definition.
	 */
	partial_module(false, false, new ModuleType[] { source, processor },
			new ModuleType[] { processor },
			new ModuleType[] { processor, sink });

	/**
	 * Represents the position of a module in an XD DSL declaration.
	 *
	 * @author Eric Bottard
	 */
	public enum Position {

		// Do not mess with the order of those!
		start, middle, end;

		/**
		 * Return the position instance for a module appearing at {@code index}
		 * inside a stream made of [0..lastIndex] (inclusive).
		 */
		public static Position of(int index, int lastIndex) {
			if (index < 0) {
				throw new IllegalArgumentException("index can't be negative");
			}
			else if (index == 0) {
				return start;
			}
			else if (index < lastIndex) {
				return middle;
			}
			else if (index == lastIndex) {
				return end;
			}
			else {
				throw new IllegalArgumentException("index can't be greater than lastIndex");
			}
		}
	}

	/**
	 * Return the kinds of modules that may appear at some position in the current parsing context.
	 */
	ModuleType[] allowed(Position position) {
		ModuleType[] result = allowed[position.ordinal()];
		if (result == null) {
			throw new IllegalArgumentException(String.format("A %s can't have a module at position '%s'",
					this.name(), position.name()));
		}
		return result;
	}

	public boolean shouldBindAndValidate() {
		return bindAndValidate;
	}

	public boolean supportsNamedChannels() {
		return supportsNamedChannels;
	}

	private ParsingContext(boolean bindAndValidate, boolean supportsNamedChannels, ModuleType atStart,
			ModuleType atMiddle, ModuleType atEnd) {
		this(bindAndValidate, supportsNamedChannels,
				new ModuleType[] { atStart },
				new ModuleType[] { atMiddle },
				new ModuleType[] { atEnd });
	}

	private ParsingContext(boolean bindAndValidate, boolean supportsNamedChannels, ModuleType[] atStart,
			ModuleType[] atMiddle, ModuleType[] atEnd) {
		this.bindAndValidate = bindAndValidate;
		this.supportsNamedChannels = supportsNamedChannels;
		allowed[0] = atStart;
		allowed[1] = atMiddle;
		allowed[2] = atEnd;
	}

	private final ModuleType[][] allowed = new ModuleType[Position.values().length][];

	/**
	 * Whether to apply binding and validation to module options.
	 *
	 * <p>
	 * Actual deployments will want this, while partials typically don't
	 * contain all required options yet, so we don't want to fail with
	 * a validation exception.
	 */
	private final boolean bindAndValidate;

	private final boolean supportsNamedChannels;

}