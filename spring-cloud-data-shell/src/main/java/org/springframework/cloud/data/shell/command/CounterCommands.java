/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.data.shell.command;

import java.text.NumberFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.data.rest.client.CounterOperations;
import org.springframework.cloud.data.rest.resource.CounterResource;
import org.springframework.cloud.data.rest.resource.MetricResource;
import org.springframework.cloud.data.shell.config.CloudDataShell;
import org.springframework.cloud.data.shell.converter.NumberFormatConverter;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.table.Table;
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

	private static final String DELETE_COUNTER = "counter delete";

	@Autowired
	private CloudDataShell cloudDataShell;

	@CliAvailabilityIndicator({ LIST_COUNTERS, DISPLAY_COUNTER, DELETE_COUNTER })
	public boolean available() {
		return cloudDataShell.getCloudDataOperations() != null;
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
	public Table list(/* TODO */) {
		PagedResources<MetricResource> list = counterOperations().list(/* TODO */);
		return displayMetrics(list);
	}

	@CliCommand(value = DELETE_COUNTER, help = "Delete the counter with the given name")
	public String delete(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the counter to delete"
					/*, optionContext = "existing-counter disable-string-converter"*/) String name) {
		counterOperations().delete(name);
		return String.format("Deleted counter '%s'", name);
	}

	private CounterOperations counterOperations() {
		return cloudDataShell.getCloudDataOperations().counterOperations();
	}

}
