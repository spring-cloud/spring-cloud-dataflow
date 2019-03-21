/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.rest.resource;

import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The REST representation of an Aggregate Count.
 *
 * @author Eric Bottard
 */
@XmlRootElement(name = "aggregate-counts")
public class AggregateCounterResource extends MetricResource {

	@JsonProperty("counts")
	@XmlElement(name = "counts", type = TreeMap.class)
	private SortedMap<Date, Long> values = new TreeMap<Date, Long>();

	/**
	 * No-arg constructor for serialization frameworks.
	 */
	protected AggregateCounterResource() {
	}

	public AggregateCounterResource(String name) {
		super(name);
	}

	/**
	 * Add a data point to the set.
	 */
	public void addValue(Date when, long value) {
		values.put(when, value);
	}

	/**
	 * Returns a date-sorted view of counts.
	 */
	public SortedMap<Date, Long> getValues() {
		return values;
	}

}
