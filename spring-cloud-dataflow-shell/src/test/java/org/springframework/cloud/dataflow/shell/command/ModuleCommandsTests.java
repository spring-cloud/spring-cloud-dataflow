/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.ModuleOperations;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.table.TableModel;

/**
 * Unit tests for ModuleCommands.
 *
 * @author Eric Bottard
 */
public class ModuleCommandsTests {

	ModuleCommands moduleCommands = new ModuleCommands();

	@Mock
	private DataFlowOperations dataFlowOperations;

	@Mock
	private ModuleOperations moduleOperations;

	private DataFlowShell dataFlowShell = new DataFlowShell();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(dataFlowOperations.moduleOperations()).thenReturn(moduleOperations);
		dataFlowShell.setDataFlowOperations(dataFlowOperations);
		moduleCommands.setDataFlowShell(dataFlowShell);
	}

	@Test
	public void testList() {

		String[][] modules = new String[][] {
				{"http", "source"},
				{"filter", "processor"},
				{"transform", "processor"},
				{"file", "source"},
				{"log", "sink"},
				{"moving-average", "processor"}
		};

		Collection<ModuleRegistrationResource> data = new ArrayList<>();
		for (String[] module : modules) {
			data.add(new ModuleRegistrationResource(module[0], module[1], null));
		}
		PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(data.size(), 1, data.size(), 1);
		PagedResources<ModuleRegistrationResource> result = new PagedResources<>(data, metadata);
		when(moduleOperations.list()).thenReturn(result);

		Object[][] expected = new String[][] {
				{"source", "processor", "sink", "task"},
				{"http", "filter", "log", null},
				{"file", "transform", null, null},
				{null, "moving-average", null, null},
		};
		TableModel model = moduleCommands.list().getModel();
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(model.getValue(row, col), Matchers.is(expected[row][col]));
			}
		}
 	}

}
