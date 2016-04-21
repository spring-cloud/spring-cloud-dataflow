package org.springframework.cloud.dataflow.server.controller;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.ReadablePeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.cloud.dataflow.rest.resource.AggregateCounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.cloud.stream.module.metrics.AggregateCounter;
import org.springframework.cloud.stream.module.metrics.AggregateCounterRepository;
import org.springframework.cloud.stream.module.metrics.AggregateCounterResolution;
import org.springframework.cloud.stream.module.metrics.FieldValueCounter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST controller for aggregate-counters
 *
 * @author Eric Bottard
 */
@Controller
@RequestMapping("/metrics/aggregate-counters")
@ExposesResourceFor(AggregateCounterResource.class)
public class AggregateCounterController {

    private final AggregateCounterRepository repository;

    private final DeepAggregateCountResourceAssembler aggregateCountResourceAssembler = new DeepAggregateCountResourceAssembler();

    private final ShallowResourceAssembler shallowAssembler = new ShallowResourceAssembler(AggregateCounterController.class);

    @Autowired
    public AggregateCounterController(AggregateCounterRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists all {@link AggregateCounter}s.
     */
    @ResponseBody
    @RequestMapping(value = "", method = RequestMethod.GET)
    public PagedResources<? extends MetricResource> list(PagedResourcesAssembler<String> pagedAssembler) {
        List<String> names = new ArrayList<>();
        for (String name : repository.list()) {
            names.add(name);
        }
        return pagedAssembler.toResource(new PageImpl<>(names), shallowAssembler);
    }

    /**
     * Delete (reset) a specific counter.
     */
    @RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    protected void delete(@PathVariable("name") String name) {
        DateTime to = providedOrDefaultToValue(null);
        DateTime from = providedOrDefaultFromValue(null, to, AggregateCounterResolution.minute);
        AggregateCounter counter = repository.getCounts(name, new Interval(from, to), AggregateCounterResolution.minute);
        if (counter == null) {
            throw new MetricsMvcEndpoint.NoSuchMetricException(name);
        }
        repository.reset(counter.getName());
    }

    /**
     * Retrieve counts for a given time interval, using some precision.
     *
     * @param name the name of the aggregate counter we want to retrieve data from
     * @param from the start-time for the interval, default depends on the resolution (e.g. go back 1 day for hourly
     *        buckets)
     * @param to the end-time for the interval, default "now"
     * @param resolution the size of buckets to aggregate, <i>e.g.</i> hourly, daily, <i>etc.</i> (default "hour")
     */
    @ResponseBody
    @RequestMapping(value = "/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public AggregateCounterResource display(@PathVariable("name") String name, //
                                           @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime from, //
                                           @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime to, //
                                           @RequestParam(value = "resolution", defaultValue = "hour") AggregateCounterResolution resolution) {

        to = providedOrDefaultToValue(to);
        from = providedOrDefaultFromValue(from, to, resolution);

        AggregateCounter aggregate = repository.getCounts(name, new Interval(from, to), resolution);

        if (aggregate == null) {
            throw new MetricsMvcEndpoint.NoSuchMetricException(name);
        }
        return aggregateCountResourceAssembler.toResource(aggregate);
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
    private DateTime providedOrDefaultFromValue(DateTime from, DateTime to, AggregateCounterResolution resolution) {
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
                throw new IllegalStateException("Shouldn't happen. Unhandled resolution: " + resolution);
        }
    }

    /**
     * Builds shallow resources for metrics (exposing only their names.
     *
     * @author Eric Bottard
     */
    private static class ShallowResourceAssembler extends
            ResourceAssemblerSupport<String, MetricResource> {

        public ShallowResourceAssembler(Class<?> controllerClass) {
            super(controllerClass, MetricResource.class);
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
     * Knows how to construct {@link AggregateCounterResource} out of {@link AggregateCounter}.
     *
     * @author Eric Bottard
     */
    private static class DeepAggregateCountResourceAssembler extends
            ResourceAssemblerSupport<AggregateCounter, AggregateCounterResource> {

        public DeepAggregateCountResourceAssembler() {
            super(AggregateCounterController.class, AggregateCounterResource.class);
        }

        @Override
        public AggregateCounterResource toResource(AggregateCounter entity) {
            return createResourceWithId(entity.getName(), entity);
        }

        @Override
        protected AggregateCounterResource instantiateResource(AggregateCounter entity) {
            AggregateCounterResource result = new AggregateCounterResource(entity.getName());
            ReadablePeriod increment = entity.getResolution().unitPeriod;
            DateTime end = entity.getInterval().getEnd();
            int i = 0;
            for (DateTime when = entity.getInterval().getStart(); !when.isAfter(end); when = when.plus(increment)) {
                result.addValue(new Date(when.getMillis()), entity.getCounts()[i++]);
            }
            return result;
        }
    }

}
