/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.analytics.metrics.FieldValueCounter;
import org.springframework.analytics.metrics.FieldValueCounterRepository;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.cloud.dataflow.rest.resource.FieldValueCounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Allows interaction with Field Value Counters.
 *
 * @author Eric Bottard
 */
@RestController
@RequestMapping("/metrics/field-value-counters")
@ExposesResourceFor(FieldValueCounterResource.class)
public class FieldValueCounterController {

	private final FieldValueCounterRepository repository;

	public FieldValueCounterController(FieldValueCounterRepository repository) {
		this.repository = repository;
	}

	private DeepResourceAssembler deepAssembler = new DeepResourceAssembler();

	private ShallowResourceAssembler shallowAssembler = new ShallowResourceAssembler();

	/**
	 * Retrieve information about a specific counter.
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public FieldValueCounterResource display(@PathVariable("name") String name) {
		FieldValueCounter counter = repository.findOne(name);
		if (counter == null) {
			throw new MetricsMvcEndpoint.NoSuchMetricException(name);
		}
		return deepAssembler.toResource(counter);
	}

	/**
	 * Delete (reset) a specific counter.
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	protected void delete(@PathVariable("name") String name) {
		FieldValueCounter counter = repository.findOne(name);
		if (counter == null) {
			throw new MetricsMvcEndpoint.NoSuchMetricException(name);
		}
		repository.reset(name);
	}

	/**
	 * List Counters that match the given criteria.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(
			PagedResourcesAssembler<String> pagedAssembler) {
		List<String> names = new ArrayList<>(repository.list());
		return pagedAssembler.toResource(new PageImpl<>(names), shallowAssembler);
	}

	/**
	 * Knows how to assemble {@link MetricResource} out of simple String names
	 */
	private static class ShallowResourceAssembler extends
			ResourceAssemblerSupport<String, MetricResource> {

		private ShallowResourceAssembler() {
			super(FieldValueCounterController.class, MetricResource.class);
		}

		@Override
		public MetricResource toResource(String name) {
			return createResourceWithId(name, name);
		}

		@Override
		protected MetricResource instantiateResource(String name) {
			return new MetricResource(name);
		}
	}

	/**
	 * Knows how to assemble {@link FieldValueCounterResource} out of {@link FieldValueCounter}.
	 *
	 * @author Eric Bottard
	 */
	private static class DeepResourceAssembler extends
			ResourceAssemblerSupport<FieldValueCounter, FieldValueCounterResource> {

		private DeepResourceAssembler() {
			super(FieldValueCounterController.class, FieldValueCounterResource.class);
		}

		@Override
		public FieldValueCounterResource toResource(FieldValueCounter entity) {
			return createResourceWithId(entity.getName(), entity);
		}

		@Override
		protected FieldValueCounterResource instantiateResource(FieldValueCounter entity) {
			return new FieldValueCounterResource(entity.getName(), entity.getFieldValueCounts());
		}

	}
}
