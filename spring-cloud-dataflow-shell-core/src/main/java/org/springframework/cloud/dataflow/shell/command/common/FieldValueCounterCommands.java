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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.analytics.rest.domain.FieldValueCounterResource;
import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.FieldValueCounterOperations;
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
import org.springframework.shell.table.Formatter;
import org.springframework.shell.table.SimpleHorizontalAligner;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.stereotype.Component;

/**
 * Commands for interacting with Field Value Counter analytics.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@Component
public class FieldValueCounterCommands extends AbstractMetricsCommands implements CommandMarker {

	public static final String DISPLAY_COUNTER = "field-value-counter display";

	public static final String LIST_COUNTERS = "field-value-counter list";

	public static final String RESET_COUNTER = "field-value-counter reset";

	@Autowired
	private DataFlowShell dataFlowShell;

	protected FieldValueCounterCommands() {
		super("Field Value Counter");
	}

	@CliAvailabilityIndicator({ LIST_COUNTERS, DISPLAY_COUNTER })
	public boolean availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.FIELD_VALUE_COUNTER);
	}

	@CliAvailabilityIndicator({ RESET_COUNTER })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.FIELD_VALUE_COUNTER);
	}

	@CliCommand(value = DISPLAY_COUNTER, help = "Display the value of a field value counter")
	public List<Object> display(
			@CliOption(key = { "", "name" }, help = "the name of the field value counter to display", mandatory = true,
			optionContext = "existing-field-value-counter disable-string-converter") String name,
			final @CliOption(key = "pattern", help = "the pattern used to format the values (see DecimalFormat)", mandatory = false, unspecifiedDefaultValue = NumberFormatConverter.DEFAULT) NumberFormat pattern) {
		FieldValueCounterResource counter = fvcOperations().retrieve(name);

		LinkedHashMap<String, Object> header = new LinkedHashMap<>();
		header.put("key", "Value");
		header.put("value", "Count");
		TableModel model = new BeanListTableModel<>(counter.getValues().entrySet(), header);

		Table table = DataFlowTables.applyStyle(new TableBuilder(model)).on(CellMatchers.ofType(Double.class))
				.addFormatter(new Formatter() {
					@Override
					public String[] format(Object value) {
						return new String[] { pattern.format(value) };
					}
				}).addAligner(SimpleHorizontalAligner.right).build();
		return Arrays.asList(String.format("Displaying values for field value counter '%s'", name), table);
	}

	@CliCommand(value = LIST_COUNTERS, help = "List all available field value counter names")
	public Table list() {
		PagedResources<MetricResource> list = fvcOperations().list();
		return displayMetrics(list);
	}

	@CliCommand(value = RESET_COUNTER, help = "Reset the field value counter with the given name")
	public String reset(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name of the field value counter to reset",
			optionContext = "existing-field-value-counter disable-string-converter") String name) {
		fvcOperations().reset(name);
		return String.format("Deleted field value counter '%s'", name);
	}

	private FieldValueCounterOperations fvcOperations() {
		return dataFlowShell.getDataFlowOperations().fieldValueCounterOperations();
	}

}
