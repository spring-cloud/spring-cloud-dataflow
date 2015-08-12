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

import org.springframework.cloud.data.rest.resource.MetricResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;

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
		Table table = new Table();
		table.addHeader(1, new TableHeader(String.format("%s name", kind)));
		for (MetricResource r : list) {
			table.newRow().addValue(1, r.getName());
		}
		return table;
	}

}
