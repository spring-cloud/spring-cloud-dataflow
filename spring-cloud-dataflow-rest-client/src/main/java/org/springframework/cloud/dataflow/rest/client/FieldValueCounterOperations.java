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

package org.springframework.cloud.dataflow.rest.client;

import org.springframework.analytics.rest.domain.FieldValueCounterResource;
import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available when dealing with Field Value Counters.
 *
 * @author Eric Bottard
 */
public interface FieldValueCounterOperations {

	/**
	 * Retrieve information about the given named counter.
	 *
	 * @param name the name of the field value counter
	 * @return the field value counter
	 */
	FieldValueCounterResource retrieve(String name);

	/**
	 * Retrieve basic information (i.e. names) for existing counters.
	 *
	 * @return a pages list of field value counters
	 */
	PagedResources<MetricResource> list();

	/**
	 * Delete the counter with given name.
	 *
	 * @param name the name of the field value counter
	 */
	void reset(String name);
}
