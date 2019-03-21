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

import java.util.LinkedHashMap;

import org.springframework.analytics.rest.domain.MetricResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;

/**
 * Base class to factor out similar behavior for all related metrics commands.
 * 
 * @author Eric Bottard
 */
public abstract class AbstractMetricsCommands {

	/**
	 * The Capitalized, singular name of the kind of metrics we're dealing with (e.g. Counter, Field Value Counter)
	 */
	private String kind;

	protected AbstractMetricsCommands(String kind) {
		this.kind = kind;
	}

	/**
	 * Render a table with information about a list of metrics
	 */
	protected Table displayMetrics(PagedResources<MetricResource> list) {
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", String.format("%s name", kind));
		TableModel model = new BeanListTableModel<>(list, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model)).build();
	}

}
