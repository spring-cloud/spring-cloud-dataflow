/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import java.text.NumberFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.CounterOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.CounterResource;
import org.springframework.cloud.dataflow.rest.resource.MetricResource;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.dataflow.shell.converter.NumberFormatConverter;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.Table;
import org.springframework.stereotype.Component;

/**
 * Commands for interacting with Counter analytics.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@Component
public class CounterCommands extends AbstractMetricsCommands implements CommandMarker {

	protected CounterCommands() {
		super("Counter");
	}

	private static final String DISPLAY_COUNTER = "counter display";

	private static final String LIST_COUNTERS = "counter list";

	private static final String DELETE_COUNTER = "counter reset";

	@Autowired
	private DataFlowShell dataFlowShell;

	@CliAvailabilityIndicator({ LIST_COUNTERS, DISPLAY_COUNTER, DELETE_COUNTER })
	public boolean available() {
		DataFlowOperations dataFlowOperations = dataFlowShell.getDataFlowOperations();
		return dataFlowOperations != null && dataFlowOperations.counterOperations() != null;
	}

	@CliCommand(value = DISPLAY_COUNTER, help = "Display the value of a counter")
	public String display(
			@CliOption(key = { "", "name" }, help = "the name of the counter to display", mandatory = true
					/*,optionContext = "existing-counter disable-string-converter"*/) String name,
			@CliOption(key = "pattern", help = "the pattern used to format the value (see DecimalFormat)",
					mandatory = false, unspecifiedDefaultValue = NumberFormatConverter.DEFAULT) NumberFormat pattern) {
		CounterResource counter = counterOperations().retrieve(name);

		return pattern.format(counter.getValue());
	}

	@CliCommand(value = LIST_COUNTERS, help = "List all available counter names")
	public Table list() {
		PagedResources<MetricResource> list = counterOperations().list();
		return displayMetrics(list);
	}

	@CliCommand(value = DELETE_COUNTER, help = "Reset the counter with the given name")
	public String reset(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the counter to reset"
					/*, optionContext = "existing-counter disable-string-converter"*/) String name) {
		counterOperations().reset(name);
		return String.format("Deleted counter '%s'", name);
	}

	private CounterOperations counterOperations() {
		return dataFlowShell.getDataFlowOperations().counterOperations();
	}

}
