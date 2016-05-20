package org.springframework.cloud.dataflow.rest.resource;

import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by aboyko on 2016-04-05.
 */
public class AggregateCounterResource extends MetricResource {

    /**
     * Counter's values
     */
    private SortedMap<Date, Long> values = new TreeMap<>();

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
