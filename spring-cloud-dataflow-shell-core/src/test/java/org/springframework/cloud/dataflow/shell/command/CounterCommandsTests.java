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

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.cloud.dataflow.shell.command.common.CounterCommands;
import org.springframework.shell.table.Table;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for {@link CounterCommands}.
 *
 * @author Eric Bottard
 */
public class CounterCommandsTests extends AbstractShellIntegrationTest {

	private static MetricRepository repository;

	@BeforeClass
	public static void setUpOnce() {
		repository = applicationContext.getBean(MetricRepository.class);
	}

	@Before
	@After
	public void cleanSlate() {
		repository.findAll().forEach(m -> repository.reset(m.getName()));
	}

	@Test
	public void testCounterInteractions() {
		Table table = metrics().listCounters();
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getRowCount(), is(1));

		repository.set(new Metric<>("counter.foo", 12.0d));
		repository.set(new Metric<>("counter.bar", 42.0d));
		table = metrics().listCounters();
		// Test alphabetical order
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getValue(1, 0), is("bar"));
		assertThat(table.getModel().getValue(2, 0), is("foo"));

		String value = metrics().displayCounter("foo");
		assertThat(value, is("12"));

		String message = metrics().resetCounter("foo");
		assertThat(message, is("Deleted counter 'foo'"));

		table = metrics().listCounters();
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getRowCount(), is(2));
	}

}
