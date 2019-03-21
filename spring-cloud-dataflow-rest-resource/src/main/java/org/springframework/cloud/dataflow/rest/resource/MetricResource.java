/*
 * Copyright 2015 the original author or authors.
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

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 * Base class for REST representations of named metrics. Can be used when a just a shallow representation
 * of a metric is expected.
 *
 * @author Eric Bottard
 */
public class MetricResource extends ResourceSupport {

	private String name;

	/**
	 * No arg constructor for serialization frameworks.
	 */
	protected MetricResource() {

	}

	public MetricResource(String name) {
		this.name = name;
	}

	/**
	 * Return the name of the metric.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 *
	 * @author Eric Bottard
	 */
	public static class Page extends PagedResources<MetricResource> {

	}

}
