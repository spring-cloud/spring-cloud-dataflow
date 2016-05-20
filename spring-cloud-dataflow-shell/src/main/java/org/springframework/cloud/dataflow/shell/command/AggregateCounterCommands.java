/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.command;

import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.AggregateCounterOperations;
import org.springframework.cloud.dataflow.rest.resource.AggregateCounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.dataflow.shell.converter.NumberFormatConverter;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.*;
import org.springframework.shell.table.Formatter;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Commands for interacting with Aggregate-Counter analytics.
 *
 * @author Eric Bottard
 */
@Component
public class AggregateCounterCommands extends AbstractMetricsCommands implements CommandMarker {

    protected AggregateCounterCommands() {
        super("Aggregate Counter");
    }

    private static final String DISPLAY_AGGR_COUNTER = "aggregate-counter display";

    private static final String LIST_AGGR_COUNTERS = "aggregate-counter list";

    private static final String DELETE_AGGR_COUNTER = "aggregate-counter delete";

    @Autowired
    private DataFlowShell dataFlowShell;

    @CliAvailabilityIndicator({ DISPLAY_AGGR_COUNTER, LIST_AGGR_COUNTERS, DELETE_AGGR_COUNTER })
    public boolean available() {
        return dataFlowShell.getDataFlowOperations() != null;
    }

    @CliCommand(value = DISPLAY_AGGR_COUNTER, help = "Display aggregate counter values by chosen interval and resolution(minute, hour)")
    public List<Object> display(
            @CliOption(key = { "", "name" }, help = "the name of the aggregate counter to display", mandatory = true, optionContext = "existing-aggregate-counter") String name,
            @CliOption(key = "from", help = "start-time for the interval. format: 'yyyy-MM-dd HH:mm:ss'", mandatory = false) String from,
            @CliOption(key = "to", help = "end-time for the interval. format: 'yyyy-MM-dd HH:mm:ss'. defaults to now", mandatory = false) String to,
            @CliOption(key = "lastHours", help = "set the interval to last 'n' hours", mandatory = false) Integer lastHours,
            @CliOption(key = "lastDays", help = "set the interval to last 'n' days", mandatory = false) Integer lastDays,
            @CliOption(key = "resolution", help = "the size of the bucket to aggregate (minute, hour, day, month)", mandatory = false, unspecifiedDefaultValue = "hour") AggregateCounterOperations.Resolution resolution,
            @CliOption(key = "pattern", help = "the pattern used to format the count values (see DecimalFormat)", mandatory = false, unspecifiedDefaultValue = NumberFormatConverter.DEFAULT) NumberFormat pattern) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date fromDate;
            switch (Assertions.atMostOneOf("from", from, "lastHours", lastHours, "lastDays", lastDays)) {
                case 0:
                    fromDate = dateFormat.parse(from);
                    break;
                case 1:
                    fromDate = new Date(System.currentTimeMillis() - ((long) lastHours)
                            * DateTimeConstants.MILLIS_PER_HOUR);
                    break;
                case 2:
                    fromDate = new Date(System.currentTimeMillis() - ((long) lastDays)
                            * DateTimeConstants.MILLIS_PER_DAY);
                    break;
                default:
                    fromDate = null;
                    break;
            }

            Date toDate = (to == null) ? null : dateFormat.parse(to);
            AggregateCounterResource aggResource = aggrCounterOperations().retrieve(name, fromDate, toDate, resolution);
            return displayAggrCounter(aggResource, pattern);
        }
        catch (ParseException pe) {
            throw new IllegalArgumentException(
                    "Parse exception ocurred while parsing the 'from/to' options. The accepted date format is "
                            + dateFormat.toPattern());
        }
    }

    @CliCommand(value = LIST_AGGR_COUNTERS, help = "List all available aggregate counter names")
    public Table list(/* TODO */) {
        PagedResources<MetricResource> list = aggrCounterOperations().list(/* TODO */);
        return displayMetrics(list);
    }

    @CliCommand(value = DELETE_AGGR_COUNTER, help = "Delete an aggregate counter")
    public String delete(
            @CliOption(key = { "", "name" }, help = "the name of the aggregate counter to delete", mandatory = true, optionContext = "existing-aggregate-counter") String name) {
        aggrCounterOperations().delete(name);
        return String.format("Deleted aggregate counter '%s'", name);
    }

    private AggregateCounterOperations aggrCounterOperations() {
        return dataFlowShell.getDataFlowOperations().aggregateCounterOperations();
    }

    private List<Object> displayAggrCounter(AggregateCounterResource aggResource, final NumberFormat pattern) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<>();
        header.put("key", "Time");
        header.put("value", "Count");
        TableModel model = new BeanListTableModel<>(aggResource.getValues().entrySet(), header);

        Table table = DataFlowTables.applyStyle(new TableBuilder(model))
                .on(CellMatchers.ofType(Double.class))
                .addFormatter(new Formatter() {
                    @Override
                    public String[] format(Object value) {
                        return new String[] {pattern.format(value)};
                    }
                })
                .addAligner(SimpleHorizontalAligner.right)
                .build();
        return Arrays.asList(
                String.format("Displaying values for aggregate counter '%s'", aggResource.getName()),
                table
        );
    }

}
