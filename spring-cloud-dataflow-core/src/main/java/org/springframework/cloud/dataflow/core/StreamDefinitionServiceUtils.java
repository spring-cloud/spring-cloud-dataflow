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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Utility class serving operations related to the {@link StreamDefinition}s.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
public class StreamDefinitionServiceUtils {


	/**
	 * Redacts sensitive property values in a stream.
	 *
	 * @param streamAppDefinitions the {@link StreamAppDefinition}s of the given stream
	 * @return the list of {@link StreamAppDefinition}s with the sensitive data redacted.
	 */
	public static LinkedList<StreamAppDefinition>  sanitizeStreamAppDefinitions(LinkedList<StreamAppDefinition> streamAppDefinitions) {
		return streamAppDefinitions.stream()
				.map(app -> StreamAppDefinition.Builder
						.from(app)
						.setProperties(new ArgumentSanitizer().sanitizeProperties(app.getProperties()))
						.build(app.getStreamName())
				).collect(Collectors.toCollection(LinkedList::new));
	}

	/**
	 * Return an iterator that indicates the order of application deployments for this
	 * stream. The application definitions are returned in reverse order; i.e. the sink is
	 * returned first followed by the processors in reverse order followed by the source.
	 *
	 * @return iterator that iterates over the application definitions in deployment order
	 */
	public static Iterator<StreamAppDefinition> getDeploymentOrderIterator(LinkedList<StreamAppDefinition> streamAppDefinitions) {
		return new ReadOnlyIterator<>(streamAppDefinitions.descendingIterator());
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
