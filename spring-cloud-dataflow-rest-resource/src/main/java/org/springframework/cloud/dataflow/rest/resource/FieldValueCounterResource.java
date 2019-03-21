/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The REST representation of a Field Value Counter.
 *
 * @author Eric Bottard
 */
public class FieldValueCounterResource extends MetricResource {

	/**
	 * The values for the counter.
	 */
	private Map<String, Double> values;


	/**
	 * No-arg constructor for serialization frameworks.
	 */
	protected FieldValueCounterResource() {

	}

	public FieldValueCounterResource(String name, Map<String, Double> values) {
		super(name);
		setValues(values);
	}

	/**
	 * Return the values for the counter.
	 */
	public Map<String, Double> getValues() {
		return values;
	}

	@JsonProperty
	public void setValues(Map<String, Double> values) {
		this.values = new TreeMap<>(new ByDecreasingValueComparator(values));
		this.values.putAll(values);
	}

	/**
	 * A comparator on map keys that orders them according to their mapped value.
	 *
	 * @author Eric Bottard
	 */
	private static class ByDecreasingValueComparator implements Comparator<String> {

		private final Map<String, Double> values;

		private ByDecreasingValueComparator(Map<String, Double> values) {
			this.values = values;
		}

		@Override
		public int compare(String k1, String k2) {
			int byValue = values.get(k1).compareTo(values.get(k2));
			return byValue == 0 ? k1.compareTo(k2) : -byValue;
		}
	}
}
