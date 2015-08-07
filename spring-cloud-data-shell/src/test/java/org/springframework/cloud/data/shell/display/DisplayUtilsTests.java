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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import org.springframework.cloud.data.shell.display.Table;
import org.springframework.cloud.data.shell.display.TableHeader;
import org.springframework.cloud.data.shell.display.TableRow;
import org.springframework.cloud.data.shell.display.DisplayUtils;
import org.springframework.util.FileCopyUtils;

/**
 * @author Gunnar Hillert
 * @author Stephan Oudmaijer
 */
public class DisplayUtilsTests {

	@Test
	public void testRenderTextTable() {
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Tap Name"))
				.addHeader(2, new TableHeader("Stream Name"))
				.addHeader(3, new TableHeader("Tap Definition"));
		for (int i = 1; i <= 3; i++) {
			final TableRow row = new TableRow();
			row.addValue(1, "tap" + i)
					.addValue(2, "ticktock")
					.addValue(3, "tap@ticktock|log");
			table.getRows().add(row);
		}
		String expectedTableAsString = null;
		final InputStream inputStream = getClass()
				.getClassLoader()
				.getResourceAsStream("testRenderTextTable-expected-output.txt");
		assertNotNull("The inputstream is null.", inputStream);
		try {
			expectedTableAsString = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
		}
		catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		final String tableRenderedAsString = DisplayUtils.renderTextTable(table);
		assertEquals(expectedTableAsString.replaceAll("\r", ""), tableRenderedAsString);
	}

	@Test
	public void testRenderTextTableWithSingleColumn() {
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Gauge name"));
		final TableRow row = new TableRow();
		row.addValue(1, "simplegauge");
		table.getRows().add(row);
		String expectedTableAsString = null;
		final InputStream inputStream = getClass()
				.getClassLoader()
				.getResourceAsStream("testRenderTextTable-single-column-expected-output.txt");
		assertNotNull("The inputstream is null.", inputStream);
		try {
			expectedTableAsString = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
		}
		catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		final String tableRenderedAsString = DisplayUtils.renderTextTable(table);
		assertEquals(expectedTableAsString.replaceAll("\r", ""), tableRenderedAsString);
	}

	@Test
	public void testRenderTextTableWithSingleColumnAndWidthOf4() {
		final Table table = new Table();
		final TableHeader tableHeader = new TableHeader("Gauge name");
		tableHeader.setMaxWidth(4);
		table.addHeader(1, tableHeader);
		final TableRow row = new TableRow();
		row.addValue(1, "simplegauge");
		table.getRows().add(row);
		String expectedTableAsString = null;
		final InputStream inputStream = getClass()
				.getClassLoader()
				.getResourceAsStream("testRenderTextTable-single-column-width4-expected-output.txt");
		assertNotNull("The inputstream is null.", inputStream);
		try {
			expectedTableAsString = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
		}
		catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		final String tableRenderedAsString = DisplayUtils.renderTextTable(table);
		assertEquals(expectedTableAsString.replaceAll("\r", ""), tableRenderedAsString);
	}

	@Test
	public void testRenderParameterInfoDataAsTableWithMaxWidth() {
		final Map<String, String> values = new TreeMap<String, String>();
		values.put("Key1", "Lorem ipsum dolor sit posuere.");
		values.put("My super key 2", "Lorem ipsum");
		String expectedTableAsString = null;
		final InputStream inputStream = getClass()
				.getClassLoader()
				.getResourceAsStream("testRenderParameterInfoDataAsTableWithMaxWidth.txt");
		assertNotNull("The inputstream is null.", inputStream);
		try {
			expectedTableAsString = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
		}
		catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		final String tableRenderedAsString = DisplayUtils.renderParameterInfoDataAsTable(values, false, 20);
		assertEquals(expectedTableAsString.replaceAll("\r", ""), tableRenderedAsString);
	}

	@Test
	public void testRenderTableWithRowShorthand() {
		final Table table = new Table();
		table.addHeader(1, new TableHeader("Property"))
				.addHeader(2, new TableHeader("Value"));
		table.addRow("Job Execution ID", String.valueOf(1))
				.addRow("Job Name", "My Job Name")
				.addRow("Start Time", "12:30")
				.addRow("Step Execution Count", String.valueOf(12))
				.addRow("Status", "COMPLETED");
		assertNotNull(table.toString());
		final InputStream inputStream = getClass()
				.getClassLoader()
				.getResourceAsStream("testRenderTableWithRowShorthand-expected-output.txt");
		assertNotNull("The inputstream is null.", inputStream);
		String expectedTableAsString = null;
		try {
			expectedTableAsString = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
		}
		catch (IOException e) {
			e.printStackTrace();
			fail();
		}
		assertEquals(expectedTableAsString.replaceAll("\r", ""), table.toString());
	}

	@Test
	public void testPadRightWithNullString() {
		assertEquals("     ", DisplayUtils.padRight(null, 5));
	}

	@Test
	public void testPadRightWithEmptyString() {
		assertEquals("     ", DisplayUtils.padRight("", 5));
	}

	@Test
	public void testPadRight() {
		assertEquals("foo  ", DisplayUtils.padRight("foo", 5));
	}
}
