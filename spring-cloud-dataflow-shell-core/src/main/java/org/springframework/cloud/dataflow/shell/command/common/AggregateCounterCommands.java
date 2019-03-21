/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command.common;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import org.joda.time.DateTimeConstants;

import org.springframework.analytics.rest.domain.AggregateCounterResource;
import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.AggregateCounterOperations;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.dataflow.shell.converter.NumberFormatConverter;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.SimpleHorizontalAligner;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.stereotype.Component;

/**
 * Commands for interacting with aggregate counter analytics.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@Component
public class AggregateCounterCommands extends AbstractMetricsCommands implements CommandMarker {

	public static final String DISPLAY_AGGR_COUNTER = "aggregate-counter display";

	public static final String LIST_AGGR_COUNTERS = "aggregate-counter list";

	public static final String DELETE_AGGR_COUNTER = "aggregate-counter delete";

	@Autowired
	private DataFlowShell dataFlowShell;

	protected AggregateCounterCommands() {
		super("AggregateCounter");
	}

	@CliAvailabilityIndicator({ DISPLAY_AGGR_COUNTER, LIST_AGGR_COUNTERS })
	public boolean availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.AGGREGATE_COUNTER);
	}

	@CliAvailabilityIndicator({ DELETE_AGGR_COUNTER })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.AGGREGATE_COUNTER);
	}

	@CliCommand(value = DISPLAY_AGGR_COUNTER, help = "Display aggregate counter values by chosen interval and "
			+ "resolution(minute, hour)")
	public Table display(
			@CliOption(optionContext = "existing-aggregate-counter disable-string-converter", key = { "",
					"name" }, help = "the name of the aggregate counter to display", mandatory = true) String name,
			@CliOption(key = "from", help = "start-time for the interval. format: 'yyyy-MM-dd HH:mm:ss'", mandatory = false) String from,
			@CliOption(key = "to", help = "end-time for the interval. format: 'yyyy-MM-dd HH:mm:ss'. defaults to "
					+ "now", mandatory = false) String to,
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
				fromDate = new Date(
						System.currentTimeMillis() - ((long) lastHours) * DateTimeConstants.MILLIS_PER_HOUR);
				break;
			case 2:
				fromDate = new Date(System.currentTimeMillis() - ((long) lastDays) * DateTimeConstants.MILLIS_PER_DAY);
				break;
			default:
				fromDate = null;
				break;
			}

			Date toDate = (to == null) ? null : dateFormat.parse(to);
			AggregateCounterResource aggResource = aggregateCounterOperations().retrieve(name, fromDate, toDate,
					resolution);
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
		PagedResources<MetricResource> list = aggregateCounterOperations().list(/* TODO */);
		return displayMetrics(list);
	}

	@CliCommand(value = DELETE_AGGR_COUNTER, help = "Delete an aggregate counter")
	public String delete(
			@CliOption(key = { "", "name" }, help = "the name of the aggregate counter to delete", mandatory = true,
			optionContext = "existing-aggregate-counter disable-string-converter") String name) {
		aggregateCounterOperations().reset(name);
		return String.format("Deleted aggregatecounter '%s'", name);
	}

	private AggregateCounterOperations aggregateCounterOperations() {
		return dataFlowShell.getDataFlowOperations().aggregateCounterOperations();
	}

	private Table displayAggrCounter(AggregateCounterResource aggResource, final NumberFormat pattern) {
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("key", "TIME");
		headers.put("value", "COUNT");
		TableModel model = new BeanListTableModel<>(aggResource.getValues().entrySet(), headers);
		Table table = DataFlowTables.applyStyle(new TableBuilder(model)).on(CellMatchers.ofType(Long.class))
				.addFormatter(value -> new String[] { pattern.format(value) }).addAligner(SimpleHorizontalAligner.right).build();
		return table;
	}

}
