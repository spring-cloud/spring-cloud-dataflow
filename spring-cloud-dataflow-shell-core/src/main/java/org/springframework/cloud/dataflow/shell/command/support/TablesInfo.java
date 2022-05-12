/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.command.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.table.Table;

/**
 * Encapsulates output of tables with header and footer infos.
 *
 * @author Janne Valkealahti
 */
public class TablesInfo {

	private final List<Table> tables = new ArrayList<>();
	private final List<String> headers = new ArrayList<>();
	private final List<String> footers = new ArrayList<>();

	public List<Table> getTables() {
		return tables;
	}

	public void addTable(Table table) {
		tables.add(table);
	}

	public List<String> getHeaders() {
		return headers;
	}

	public void addHeader(String header) {
		headers.add(header);
	}

	public List<String> getFooters() {
		return footers;
	}

	public void addFooter(String footer) {
		footers.add(footer);
	}
}
