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

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.analytics.metrics.AggregateCounterRepository;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.shell.table.Table;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.dataflow.shell.command.TableMatcher.hasRowThat;

/**
 * Integration tests for {@link AggregateCounterCommands}.
 *
 * @author Eric Bottard
 */
public class AggregateCounterCommandsTests extends AbstractShellIntegrationTest {

	private static AggregateCounterRepository repository;

	@BeforeClass
	public static void setUpOnce() {
		repository = applicationContext.getBean(AggregateCounterRepository.class);
	}

	@Before
	@After
	public void cleanSlate() {
		repository.list().forEach(repository::reset);
	}


	@Test
	public void testAggregateCounterInteractions() {
		Table table = metrics().listAggregateCounters();
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getRowCount(), is(1));

		repository.increment("foo", 12L, DateTime.parse("2012-04-04T11:07"));
		repository.increment("foo", 42L, DateTime.parse("2012-04-04T10:07"));
		repository.increment("bar", 12L, DateTime.parse("2012-04-04T11:07"));
		table = metrics().listAggregateCounters();
		// Test alphabetical order
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getValue(1, 0), is("bar"));
		assertThat(table.getModel().getValue(2, 0), is("foo"));


		Table values = metrics().displayAggregateCounter("foo", "2012-04-04 05:00:00", "2012-04-04 17:00:00");
		assertThat(values, hasRowThat(is(Instant.parse("2012-04-04T10:00").toDate()), is(42L)));
		assertThat(values, hasRowThat(is(Instant.parse("2012-04-04T11:00").toDate()), is(12L)));

		String message = metrics().resetAggregateCounter("foo");
		assertThat(message, is("Deleted aggregatecounter 'foo'"));
		table = metrics().listAggregateCounters();
		assertThat(table.getModel().getColumnCount(), is(1));
		assertThat(table.getModel().getRowCount(), is(2));
	}
}
