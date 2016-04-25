/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.core.ArtifactType;
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
 * @author Mark Fisher
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

	@Test
	public void register() {
		String name = "foo";
		ArtifactType type = ArtifactType.sink;
		String uri = "file:///foo";
		boolean force = false;
		ModuleRegistrationResource resource = new ModuleRegistrationResource(name, type.name(), uri);
		when(moduleOperations.register(name, type, uri, force)).thenReturn(resource);
		String result = moduleCommands.register(name, type, uri, force);
		assertEquals("Successfully registered module 'sink:foo'", result);
	}

	@Test
	public void importFromLocalResource() {
		String name1 = "foo";
		ArtifactType type1 = ArtifactType.source;
		String uri1 = "file:///foo";
		String name2 = "bar";
		ArtifactType type2 = ArtifactType.sink;
		String uri2 = "file:///bar";
		Properties apps = new Properties();
		apps.setProperty(type1.name() + "." + name1, uri1);
		apps.setProperty(type2.name() + "." + name2, uri2);
		List<ModuleRegistrationResource> resources = new ArrayList<>();
		resources.add(new ModuleRegistrationResource(name1, type1.name(), uri1));
		resources.add(new ModuleRegistrationResource(name2, type2.name(), uri2));
		PagedResources<ModuleRegistrationResource> pagedResources = new PagedResources<>(resources,
				new PagedResources.PageMetadata(resources.size(), 1, resources.size(), 1));
		when(moduleOperations.registerAll(apps, true)).thenReturn(pagedResources);
		String appsFileUri = "classpath:moduleCommandsTests-apps.properties";
		String result = moduleCommands.importFromResource(appsFileUri, true, true);
		assertEquals("Successfully registered modules: [source.foo, sink.bar]", result);
	}

	@Test
	public void importFromResource() {
		List<ModuleRegistrationResource> resources = new ArrayList<>();
		resources.add(new ModuleRegistrationResource("foo", "source", null));
		resources.add(new ModuleRegistrationResource("bar", "sink", null));
		PagedResources<ModuleRegistrationResource> pagedResources = new PagedResources<>(resources,
				new PagedResources.PageMetadata(resources.size(), 1, resources.size(), 1));
		String uri = "test://example";
		when(moduleOperations.importFromResource(uri, true)).thenReturn(pagedResources);
		String result = moduleCommands.importFromResource(uri, false, true);
		assertEquals("Successfully registered 2 modules from 'test://example'", result);
	}
}
