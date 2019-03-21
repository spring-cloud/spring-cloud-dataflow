/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.Date;

import org.springframework.analytics.rest.domain.AggregateCounterResource;
import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available when dealing with Aggregate Counters.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface AggregateCounterOperations {

	/**
	 * Retrieve the information for the given named AggregateCounter
	 *
	 * @param name the name of the aggregate counter to retrieve information for
	 * @param from the start date
	 * @param to the end date
	 * @param resolution the resolution (minute, hour, day, or month) of the aggregate
	 * counter data
	 * @return the aggregate counter
	 */
	AggregateCounterResource retrieve(String name, Date from, Date to, Resolution resolution);

	/**
	 * Retrieve basic information (i.e. names) for existing counters.
	 *
	 * @return the paged list of metrics
	 */
	PagedResources<MetricResource> list();

	/**
	 * Delete the aggregate counter with given name.
	 *
	 * @param name the name of the aggregate counter
	 */
	void reset(String name);

	public static enum Resolution {
		minute,
		hour,
		day,
		month
	}
}
