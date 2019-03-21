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

import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.cloud.dataflow.rest.resource.CounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Allows interaction with Counters.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
@RestController
@RequestMapping("/metrics/counters")
@ExposesResourceFor(CounterResource.class)
public class CounterController {

	public static final String COUNTER_PREFIX = "counter.";

	private final MetricRepository metricRepository;

	private final ResourceAssembler<Metric<Double>, CounterResource> counterResourceAssembler =
			new DeepCounterResourceAssembler();

	protected final ResourceAssembler<Metric<Double>, ? extends MetricResource> shallowResourceAssembler =
			new ShallowMetricResourceAssembler();

	/**
	 * Create a {@link CounterController} that delegates to the provided {@link MetricRepository}.
	 *
	 * @param metricRepository the {@link MetricRepository} used by this controller
	 */
	public CounterController(MetricRepository metricRepository) {
		Assert.notNull(metricRepository, "metricRepository must not be null");
		this.metricRepository = metricRepository;
	}

	/**
	 * List Counters that match the given criteria.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<? extends MetricResource> list(
			Pageable pageable,
			PagedResourcesAssembler<Metric<Double>> pagedAssembler,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed) {
		/* Page */ Iterable<Metric<?>> metrics = metricRepository.findAll(/* pageable */);
		List<Metric<Double>> content = filterCounters(metrics);
		Page<Metric<Double>> page = new PageImpl<>(content);
		ResourceAssembler<Metric<Double>, ? extends MetricResource> assemblerToUse =
				detailed ? counterResourceAssembler : shallowResourceAssembler;
		return pagedAssembler.toResource(page, assemblerToUse);
	}

	/**
	 * Retrieve information about a specific counter.
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public CounterResource display(@PathVariable("name") String name) {
		Metric<Double> c = findCounter(name);
		return counterResourceAssembler.toResource(c);
	}

	/**
	 * Delete (reset) a specific counter.
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	protected void delete(@PathVariable("name") String name) {
		Metric<Double> c = findCounter(name);
		metricRepository.reset(c.getName());
	}

	/**
	 * Find a given counter, taking care of name conversion between the Spring Boot domain and our domain.
	 * @throws MetricsMvcEndpoint.NoSuchMetricException if the counter does not exist
	 */
	private Metric<Double> findCounter(@PathVariable("name") String name) {
		@SuppressWarnings("unchecked")
		Metric<Double> c = (Metric<Double>) metricRepository.findOne(COUNTER_PREFIX + name);
		if (c == null) {
			throw new MetricsMvcEndpoint.NoSuchMetricException(name);
		}
		return c;
	}


	/**
	 * Filter the list of Boot metrics to only return those that are counters.
	 */
	@SuppressWarnings("unchecked")
	private <T extends Number> List<Metric<T>> filterCounters(Iterable<Metric<?>> input) {
		List<Metric<T>> result = new ArrayList<>();
		for (Metric<?> metric : input) {
			if (metric.getName().startsWith(COUNTER_PREFIX)) {
				result.add((Metric<T>) metric);
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
