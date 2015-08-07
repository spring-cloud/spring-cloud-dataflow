/*
 * Copyright 2009-2015 the original author or authors.
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

package org.springframework.cloud.data.shell.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Provide a basic concept of a table structure containing a map of column headers and a collection of rows. Used to
 * render text-based tables (console output).
 *
 * @see DisplayUtils
 *
 * @author Gunnar Hillert
 * @since 1.0
 */
public class Table {

	private final Map<Integer, TableHeader> headers = new TreeMap<Integer, TableHeader>();

	private volatile List<TableRow> rows = new ArrayList<TableRow>(0);

	public List<TableRow> getRows() {
		return rows;
	}

	public Map<Integer, TableHeader> getHeaders() {
		return headers;
	}

	public Table addHeader(Integer columnIndex, TableHeader tableHeader) {
		this.headers.put(columnIndex, tableHeader);
		return this;
	}

	/**
	 * Add a new empty row to the table.
	 *
	 * @return the newly created row, which can be then be populated
	 */
	public TableRow newRow() {
		TableRow row = new TableRow();
		rows.add(row);
		return row;
	}

	public Table addRow(String... values) {
		final TableRow row = new TableRow();
		int column = 1;
		for (String value : values) {
			row.addValue(column, value);
			column++;
		}
		rows.add(row);
		return this;
	}

	public void calculateColumnWidths() {
		for (Map.Entry<Integer, TableHeader> headerEntry : headers.entrySet()) {
			final Integer headerEntryKey = headerEntry.getKey();
			for (TableRow tableRow : rows) {
				if (tableRow.getValue(headerEntryKey) != null) {
					headerEntry.getValue().updateWidth(tableRow.getValue(headerEntryKey).length());
				}
			}
		}
	}

	@Override
	public String toString() {
		return DisplayUtils.renderTextTable(this);
	}

	@Override
	public int hashCode() {
		calculateColumnWidths();
		final int prime = 31;
		int result = 1;
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + ((rows == null) ? 0 : rows.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Table other = (Table) obj;
		this.calculateColumnWidths();
		other.calculateColumnWidths();
		if (headers == null) {
			if (other.headers != null) {
				return false;
			}
		}
		else if (!headers.equals(other.headers)) {
			return false;
		}
		if (rows == null) {
			if (other.rows != null) {
				return false;
			}
		}
		else if (!rows.equals(other.rows)) {
			return false;
		}
		return true;
	}
}
