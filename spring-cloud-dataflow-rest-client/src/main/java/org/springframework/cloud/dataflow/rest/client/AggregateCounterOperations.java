package org.springframework.cloud.dataflow.rest.client;

import org.springframework.cloud.dataflow.rest.resource.AggregateCounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.hateoas.PagedResources;

import java.util.Date;

/**
 * @author Alex Boyko
 */
public interface AggregateCounterOperations {

    /**
     * Retrieve the information for the given named AggregateCounter
     *
     * @param name the name of the aggregate counter to retrieve information for
     */
    AggregateCounterResource retrieve(String name, Date from, Date to, Resolution resolution);

    /**
     * List the names of the available aggregate counters
     */
    PagedResources<MetricResource> list();

    /**
     * Delete the given named aggregate counter
     *
     * @param name the name of the aggregate counter to delete
     */
    void delete(String name);

    enum Resolution {
        minute, hour, day, month;
    };

}
