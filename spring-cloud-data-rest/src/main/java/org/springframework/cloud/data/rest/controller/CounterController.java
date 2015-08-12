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

package org.springframework.cloud.data.rest.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.cloud.data.rest.resource.MetricResource;
import org.springframework.cloud.data.rest.resource.CounterResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Allows interaction with Counters.
 *
 * @author Eric Bottard
 */
@RestController
@RequestMapping("/metrics/counters")
@ExposesResourceFor(CounterResource.class)
public class CounterController {

	public static final String COUNTER_PREFIX = "counter.";

	@Autowired
	private MetricRepository metricRepository;

	private final ResourceAssembler<Metric<Double>, CounterResource> counterResourceAssembler =
			new DeepCounterResourceAssembler();

	protected final ResourceAssembler<Metric<Double>, ? extends MetricResource> shallowResourceAssembler =
			new ShallowMetricResourceAssembler();

	/**
	 * List Counters that match the given criteria.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(
			Pageable pageable,
			PagedResourcesAssembler<Metric<Double>> pagedAssembler,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {
		/* Page */
		Iterable metrics = metricRepository.findAll(/* pageable */);
		Page<Metric<Double>> page = new PageImpl<>(filterCounters(metrics));
		ResourceAssembler<Metric<Double>, ? extends MetricResource> assemblerToUse =
				detailed ? counterResourceAssembler : shallowResourceAssembler;
		return pagedAssembler.toResource(page, assemblerToUse);
	}

	/**
	 * Retrieve information about a specific counter.
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public CounterResource display(@PathVariable("name") String name) {
		Metric<Double> c = (Metric<Double>) metricRepository.findOne(COUNTER_PREFIX + name);
		if (c == null) {
			throw new MetricsMvcEndpoint.NoSuchMetricException(name);
		}
		return counterResourceAssembler.toResource(c);
	}

	/**
	 * Delete (reset) a specific counter.
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	protected void delete(@PathVariable("name") String name) {
		Metric<Double> c = (Metric<Double>) metricRepository.findOne(COUNTER_PREFIX + name);
		if (c == null) {
			throw new MetricsMvcEndpoint.NoSuchMetricException(name);
		}
		metricRepository.reset(c.getName());
	}


	private List<Metric<Double>> filterCounters(Iterable<Metric<?>> input) {
		List<Metric<Double>> result = new ArrayList<>();
		for (Metric<?> metric : input) {
			if (metric.getName().startsWith(COUNTER_PREFIX)) {
				result.add((Metric<Double>) metric);
			}
		}
		return result;
	}

	/**
	 * Base class for a ResourceAssembler that builds shallow resources for metrics
	 * (exposing only their names, and hence their "self" rel).
	 *
	 * @author Eric Bottard
	 */
	static class ShallowMetricResourceAssembler extends
			ResourceAssemblerSupport<Metric<Double>, MetricResource> {

		public ShallowMetricResourceAssembler() {
			super(CounterController.class, MetricResource.class);
		}

		@Override
		public MetricResource toResource(Metric<Double> entity) {
			return createResourceWithId(entity.getName().substring(COUNTER_PREFIX.length()), entity);
		}

		@Override
		protected MetricResource instantiateResource(Metric<Double> entity) {
			return new MetricResource(entity.getName().substring(COUNTER_PREFIX.length()));
		}

	}

	/**
	 * Knows how to assemble {@link CounterResource}s out of counter {@link Metric}s.
	 *
	 * @author Eric Bottard
	 */
	static class DeepCounterResourceAssembler extends
			ResourceAssemblerSupport<Metric<Double>, CounterResource> {

		public DeepCounterResourceAssembler() {
			super(CounterController.class, CounterResource.class);
		}

		@Override
		public CounterResource toResource(Metric<Double> entity) {
			return createResourceWithId(entity.getName().substring(COUNTER_PREFIX.length()), entity);
		}

		@Override
		protected CounterResource instantiateResource(Metric<Double> entity) {
			return new CounterResource(entity.getName().substring(COUNTER_PREFIX.length()), entity.getValue().longValue());
		}

	}

}
