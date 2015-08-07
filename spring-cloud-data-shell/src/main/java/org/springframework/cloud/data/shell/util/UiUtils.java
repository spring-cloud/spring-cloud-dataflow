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

package org.springframework.cloud.data.shell.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

/**
 * Contains utility methods for rendering data to a formatted console output. E.g. it provides helper methods for
 * rendering ASCII-based data tables.
 * 
 * @author Gunnar Hillert
 * @author Thomas Risberg
 * @since 1.0
 * 
 */
public final class UiUtils {

	public static final String HORIZONTAL_LINE = "-------------------------------------------------------------------------------\n";

	public static final int COLUMN_1 = 1;

	public static final int COLUMN_2 = 2;

	public static final int COLUMN_3 = 3;

	public static final int COLUMN_4 = 4;

	public static final int COLUMN_5 = 5;

	public static final int COLUMN_6 = 6;

	/**
	 * Prevent instantiation.
	 * 
	 */
	private UiUtils() {
		throw new AssertionError();
	}

	/**
	 * Renders a textual representation of the list of provided Map data
	 * 
	 * @param columns List of Maps
	 * @return The rendered table representation as String
	 * 
	 */
	public static String renderMapDataAsTable(List<Map<String, Object>> data, List<String> columns) {

		Table table = new Table();

		int col = 0;
		for (String colName : columns) {
			col++;
			table.getHeaders().put(col, new TableHeader(colName));
			if (col >= 6) {
				break;
			}
		}

		for (Map<String, Object> dataRow : data) {

			TableRow tableRow = new TableRow();

			for (int i = 0; i < col; i++) {
				String value = dataRow.get(columns.get(i)).toString();
				table.getHeaders().get(i + 1).updateWidth(value.length());
				tableRow.addValue(i + 1, value);
			}

			table.getRows().add(tableRow);
		}

		return renderTextTable(table);
	}

	public static String renderParameterInfoDataAsTable(Map<String, String> parameters, boolean withHeader,
			int lastColumnMaxWidth) {
		final Table table = new Table();

		table.getHeaders().put(COLUMN_1, new TableHeader("Parameter"));

		final TableHeader tableHeader2 = new TableHeader("Value (Configured or Default)");
		tableHeader2.setMaxWidth(lastColumnMaxWidth);
		table.getHeaders().put(COLUMN_2, tableHeader2);

		for (Entry<String, String> entry : parameters.entrySet()) {

			final TableRow tableRow = new TableRow();

			table.getHeaders().get(COLUMN_1).updateWidth(entry.getKey().length());
			tableRow.addValue(COLUMN_1, entry.getKey());

			int width = entry.getValue() != null ? entry.getValue().length() : 0;

			table.getHeaders().get(COLUMN_2).updateWidth(width);
			tableRow.addValue(COLUMN_2, entry.getValue());

			table.getRows().add(tableRow);
		}

		return renderTextTable(table, withHeader);
	}

	/**
	 * Renders a textual representation of provided parameter map.
	 * 
	 * @param parameters Map of parameters (key, value)
	 * @return The rendered table representation as String
	 * 
	 */
	public static String renderParameterInfoDataAsTable(Map<String, String> parameters) {
		return renderParameterInfoDataAsTable(parameters, true, -1);
	}

	public static String renderTextTable(Table table) {
		return renderTextTable(table, true);
	}

	/**
	 * Renders a textual representation of the provided {@link Table}
	 * 
	 * @param table Table data {@link Table}
	 * @return The rendered table representation as String
	 */
	public static String renderTextTable(Table table, boolean withHeader) {

		table.calculateColumnWidths();

		final String padding = "  ";
		final String headerBorder = getHeaderBorder(table.getHeaders());
		final StringBuilder textTable = new StringBuilder();

		if (withHeader) {
			final StringBuilder headerline = new StringBuilder();
			for (TableHeader header : table.getHeaders().values()) {

				if (header.getName().length() > header.getWidth()) {
					Iterable<String> chunks = fixedLengthSplit(header.getName(), header.getWidth());
					int length = headerline.length();
					boolean first = true;
					for (String chunk : chunks) {
						final String lineToAppend;
						if (first) {
							lineToAppend = padding + CommonUtils.padRight(chunk, header.getWidth());
						}
						else {
							lineToAppend = StringUtils.leftPad("", length) + padding
									+ CommonUtils.padRight(chunk, header.getWidth());
						}
						first = false;
						headerline.append(lineToAppend);
						headerline.append("\n");
					}
					headerline.deleteCharAt(headerline.lastIndexOf("\n"));
				}
				else {
					String lineToAppend = padding + CommonUtils.padRight(header.getName(), header.getWidth());
					headerline.append(lineToAppend);
				}
			}
			textTable.append(org.springframework.util.StringUtils.trimTrailingWhitespace(headerline.toString()));
			textTable.append("\n");
		}

		textTable.append(headerBorder);

		for (TableRow row : table.getRows()) {
			StringBuilder rowLine = new StringBuilder();
			for (Entry<Integer, TableHeader> entry : table.getHeaders().entrySet()) {
				String value = row.getValue(entry.getKey());
				if (null != value && (value.length() > entry.getValue().getWidth())) {
					Iterable<String> chunks = fixedLengthSplit(value, entry.getValue().getWidth());
					int length = rowLine.length();
					boolean first = true;
					for (String chunk : chunks) {
						final String lineToAppend;
						if (first) {
							lineToAppend = padding + CommonUtils.padRight(chunk, entry.getValue().getWidth());
						}
						else {
							lineToAppend = StringUtils.leftPad("", length) + padding
									+ CommonUtils.padRight(chunk, entry.getValue().getWidth());
						}
						first = false;
						rowLine.append(lineToAppend);
						rowLine.append("\n");
					}
					rowLine.deleteCharAt(rowLine.lastIndexOf("\n"));
				}
				else {
					String lineToAppend = padding + CommonUtils.padRight(value, entry.getValue().getWidth());
					rowLine.append(lineToAppend);
				}
			}
			textTable.append(org.springframework.util.StringUtils.trimTrailingWhitespace(rowLine.toString()));
			textTable.append("\n");
		}

		if (!withHeader) {
			textTable.append(headerBorder);
		}

		return textTable.toString();
	}

	/**
	 * Renders the Table header border, based on the map of provided headers.
	 * 
	 * @param headers Map of headers containing meta information e.g. name+width of header
	 * @return Returns the rendered header border as String
	 */
	public static String getHeaderBorder(Map<Integer, TableHeader> headers) {

		final StringBuilder headerBorder = new StringBuilder();

		for (TableHeader header : headers.values()) {
			headerBorder.append(CommonUtils.padRight("  ", header.getWidth() + 2, '-'));
		}
		headerBorder.append("\n");

		return headerBorder.toString();
	}

	private static Iterable<String> fixedLengthSplit(String s, int length) {
		List<String> chunks = new ArrayList<>();
		int lastIndex = 0;
		while (lastIndex < s.length()) {
			int nextIndex = Math.min(lastIndex + length, s.length());
			chunks.add(s.substring(lastIndex, nextIndex));
			lastIndex = nextIndex;
		}
		return chunks;
	}
}
