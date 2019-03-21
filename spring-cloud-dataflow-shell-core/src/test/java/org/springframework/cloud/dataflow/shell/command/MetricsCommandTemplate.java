/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import java.util.List;

import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.table.Table;

/**
 * Helper methods for dealing with metrics related commands in the shell.
 *
 * @author Eric Bottard
 */
public class MetricsCommandTemplate {

	private final JLineShellComponent shell;

	public MetricsCommandTemplate(JLineShellComponent shell) {
		this.shell = shell;
	}

	public Table listCounters() {
		return (Table) shell.executeCommand(CounterCommands.LIST_COUNTERS).getResult();
	}

	public String resetCounter(String name) {
		return (String) shell.executeCommand(String.format("%s %s", CounterCommands.DELETE_COUNTER, name)).getResult();
	}

	public String displayCounter(String name) {
		return (String) shell.executeCommand(String.format("%s %s", CounterCommands.DISPLAY_COUNTER, name)).getResult();
	}

	public Table listFieldValueCounters() {
		return (Table) shell.executeCommand(FieldValueCounterCommands.LIST_COUNTERS).getResult();
	}

	@SuppressWarnings("unchecked")
	public Table displayFieldValueCounter(String name) {
		List<Object> result = (List<Object>) shell.executeCommand(String.format("%s %s", FieldValueCounterCommands.DISPLAY_COUNTER, name)).getResult();
		return (Table) result.get(1);
	}

	public String resetFieldValueCounter(String name) {
		return (String) shell.executeCommand(String.format("%s %s", FieldValueCounterCommands.RESET_COUNTER, name)).getResult();
	}

	public Table listAggregateCounters() {
		return (Table) shell.executeCommand(AggregateCounterCommands.LIST_AGGR_COUNTERS).getResult();
	}

	public Table displayAggregateCounter(String name) {
		return displayAggregateCounter(name, null, null);
	}

	public Table displayAggregateCounter(String name, String from, String to) {
		return (Table) shell.executeCommand(String.format("%s %s %s %s",
			AggregateCounterCommands.DISPLAY_AGGR_COUNTER,
			name,
			from != null ? "--from '" + from +"'": "",
			to != null ? "--to '" + to +"'": "")
		).getResult();
	}

	public String resetAggregateCounter(String name) {
		return (String) shell.executeCommand(String.format("%s %s", AggregateCounterCommands.DELETE_AGGR_COUNTER, name)).getResult();
	}
}
