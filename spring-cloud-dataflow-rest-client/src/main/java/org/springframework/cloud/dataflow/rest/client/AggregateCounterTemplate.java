package org.springframework.cloud.dataflow.rest.client;

import org.joda.time.DateTime;
import org.springframework.cloud.dataflow.rest.resource.AggregateCounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

/**
 * @author Alex Boyko
 */
public class AggregateCounterTemplate implements AggregateCounterOperations {

    public static final String AGGREGATE_COUNTERS_COLLECTION_RELATION = "aggregate-counters";

    public static final String AGGREGATE_COUNTERS_RELATION = "aggregate-counters/counter";

    private final RestTemplate fRestTemplate;

    private final ResourceSupport fLinks;

    public AggregateCounterTemplate(RestTemplate restTemplate, ResourceSupport resources) {
        fRestTemplate = restTemplate;
        fLinks = resources;
    }

    @Override
    public AggregateCounterResource retrieve(String name, Date from, Date to, Resolution resolution) {
        Assert.notNull(resolution, "Resolution must not be null");

        MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
        values.add("resolution", resolution.toString());
        if (from != null) {
            values.add("from", new DateTime(from.getTime()));
        }
        if (to != null) {
            values.add("to", new DateTime(to.getTime()));
        }

        return fRestTemplate.getForObject(fLinks.getLink(AGGREGATE_COUNTERS_RELATION).expand(name).getHref(), AggregateCounterResource.class, values);
    }

    @Override
    public PagedResources<MetricResource> list() {
        return fRestTemplate.getForObject(fLinks.getLink(AGGREGATE_COUNTERS_COLLECTION_RELATION).getHref(), MetricResource.Page.class);
    }

    @Override
    public void delete(String name) {
        fRestTemplate.delete(fLinks.getLink(AGGREGATE_COUNTERS_RELATION).expand(name).getHref());
    }
}
