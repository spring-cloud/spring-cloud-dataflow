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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.ReadablePeriod;

import org.springframework.analytics.metrics.AggregateCounter;
import org.springframework.analytics.metrics.AggregateCounterRepository;
import org.springframework.analytics.metrics.AggregateCounterResolution;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.cloud.dataflow.rest.resource.AggregateCounterResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
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
 * Allows interaction with Aggregate Counters.
 *
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 */
@RestController
@RequestMapping("/metrics/aggregate-counters")
@ExposesResourceFor(AggregateCounterResource.class)
public class AggregateCounterController {

	private final AggregateCounterRepository repository;

	public AggregateCounterController(AggregateCounterRepository repository) {
		this.repository = repository;
	}

	private DeepResourceAssembler deepAssembler = new DeepResourceAssembler();

	private ShallowResourceAssembler shallowAssembler = new ShallowResourceAssembler();

	/**
	 * Retrieve information about a specific aggregate counter.
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	public AggregateCounterResource display(@PathVariable("name") String name) {
		AggregateCounter counter = repository.findOne(name);
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
		AggregateCounter counter = repository.findOne(name);
		if (counter == null) {
			throw new MetricsMvcEndpoint.NoSuchMetricException(name);
		}
		repository.reset(name);
	}

	/**
	 * List Counters that match the given criteria.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	public PagedResources<AggregateCounterResource> list(
			Pageable pageable, PagedResourcesAssembler<String> pagedAssembler,
			@RequestParam(value = "detailed", defaultValue = "false") boolean detailed,
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime to,
			@RequestParam(value = "resolution", defaultValue = "hour") AggregateCounterResolution resolution) {
		List<String> names = new ArrayList<>(repository.list());
		PagedResources<AggregateCounterResource> resources = pagedAssembler.toResource(new PageImpl<>(names), shallowAssembler);
		if (detailed) {
			to = providedOrDefaultToValue(to);
			from = providedOrDefaultFromValue(from, to, resolution);
			Interval interval = new Interval(from, to);
			List<AggregateCounterResource> aggregateCounts = new LinkedList<>();
			for (AggregateCounterResource aggregateCounterResource : resources) {
				AggregateCounter aggregateCount = repository.getCounts(aggregateCounterResource.getName(), interval, resolution);
				aggregateCounts.add(deepAssembler.toResource(aggregateCount));
			}
			return new PagedResources<>(aggregateCounts, resources.getMetadata());
		}
		return resources;
	}

	/**
	 * Retrieve counts for a given time interval, using some precision.
	 *
	 * @param name       the name of the aggregate counter we want to retrieve data from
	 * @param from       the start-time for the interval, default depends on the resolution (e.g. go back 1 day for hourly
	 *                   buckets)
	 * @param to         the end-time for the interval, default "now"
	 * @param resolution the size of buckets to aggregate, <i>e.g.</i> hourly, daily, <i>etc.</i> (default "hour")
	 */
	@ResponseBody
	@RequestMapping(value = "/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public AggregateCounterResource display(
			@PathVariable("name") String name,
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime to,
			@RequestParam(value = "resolution", defaultValue = "hour") AggregateCounterResolution resolution) {
		to = providedOrDefaultToValue(to);
		from = providedOrDefaultFromValue(from, to, resolution);
		AggregateCounter aggregate = repository.getCounts(name, new Interval(from, to), resolution);
		return deepAssembler.toResource(aggregate);
	}

	/**
	 * Return a default value for the interval end if none has been provided.
	 */
	private DateTime providedOrDefaultToValue(DateTime to) {
		if (to == null) {
			to = new DateTime();
		}
		return to;
	}

	/**
	 * Return a default value for the interval start if none has been provided.
	 */
	private DateTime providedOrDefaultFromValue(DateTime from, DateTime to,
			AggregateCounterResolution resolution) {
		if (from != null) {
			return from;
		}
		switch (resolution) {
		case minute:
			return to.minusMinutes(59);
		case hour:
			return to.minusHours(23);
		case day:
			return to.minusDays(6);
		case month:
			return to.minusMonths(11);
		case year:
			return to.minusYears(4);
		default:
			throw new IllegalStateException(
					"Shouldn't happen. Unhandled resolution: " + resolution);
		}
	}

	/**
	 * Knows how to assemble {@link AggregateCounterResource} out of simple String names
	 */
	private static class ShallowResourceAssembler
			extends ResourceAssemblerSupport<String, AggregateCounterResource> {

		private ShallowResourceAssembler() {
			super(AggregateCounterController.class, AggregateCounterResource.class);
		}

		@Override
		public AggregateCounterResource toResource(String name) {
			return createResourceWithId(name, name);
		}

		@Override
		protected AggregateCounterResource instantiateResource(String name) {
			return new AggregateCounterResource(name);
		}
	}

	private static class DeepResourceAssembler
			extends ResourceAssemblerSupport<AggregateCounter, AggregateCounterResource> {

		private DeepResourceAssembler() {
			super(AggregateCounterController.class, AggregateCounterResource.class);
		}

		@Override
		public AggregateCounterResource toResource(AggregateCounter entity) {
			return createResourceWithId(entity.getName(), entity);
		}

		@Override
		protected AggregateCounterResource instantiateResource(AggregateCounter entity) {
			AggregateCounterResource result = new AggregateCounterResource(
					entity.getName());
			ReadablePeriod increment = entity.getResolution().unitPeriod;
			DateTime end = entity.getInterval().getEnd();
			int i = 0;
			for (DateTime when = entity.getInterval().getStart(); !when
					.isAfter(end); when = when.plus(increment)) {
				result.addValue(new Date(when.getMillis()), entity.getCounts()[i++]);
			}
			return result;
		}
	}
}
