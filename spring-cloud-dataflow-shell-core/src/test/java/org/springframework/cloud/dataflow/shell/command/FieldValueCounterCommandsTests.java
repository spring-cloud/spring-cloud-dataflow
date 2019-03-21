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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.analytics.metrics.FieldValueCounterRepository;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.shell.table.Table;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.dataflow.shell.command.TableMatcher.hasRowThat;

/**
 * Integration tests for {@link FieldValueCounterCommands}.
 *
 * @author Eric Bottard
 */
public class FieldValueCounterCommandsTests extends AbstractShellIntegrationTest {

	private static FieldValueCounterRepository repository;

	@BeforeClass
	public static void setUpOnce() {
		repository = applicationContext.getBean(FieldValueCounterRepository.class);
	}

	@Before
	@After
	public void cleanSlate() {
		repository.list().forEach(repository::reset);
	}


	@Test
	public void testFVCInteractions() {
		Table table = metrics().listFieldValueCounters();
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getRowCount(), is(1));

		repository.increment("foo", "fieldA", 12.0d);
		repository.increment("foo", "fieldB", 42.0d);
		repository.increment("bar", "fieldA", 12.0d);
		table = metrics().listFieldValueCounters();
		// Test alphabetical order
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getValue(1, 0), is("bar"));
		assertThat(table.getModel().getValue(2, 0), is("foo"));


		Table values = metrics().displayFieldValueCounter("foo");
		assertThat(values, hasRowThat(is("fieldA"), is(12d)));
		assertThat(values, hasRowThat(is("fieldB"), is(42d)));

		String message = metrics().resetFieldValueCounter("foo");
		assertThat(message, is("Deleted field value counter 'foo'"));
		table = metrics().listFieldValueCounters();
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getRowCount(), is(2));
	}
}
