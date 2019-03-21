/*
 * Copyright 2015-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.client.AppRegistryOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.shell.command.classic.ClassicAppRegistryCommands;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.springframework.cloud.dataflow.shell.command.classic.ClassicStreamCommands}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class ClassicAppRegistryCommandsTests {

	ClassicAppRegistryCommands appRegistryCommands = new ClassicAppRegistryCommands();

	@Mock
	private DataFlowOperations dataFlowOperations;

	@Mock
	private AppRegistryOperations appRegistryOperations;

	private DataFlowShell dataFlowShell = new DataFlowShell();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(dataFlowOperations.appRegistryOperations()).thenReturn(appRegistryOperations);
		dataFlowShell.setDataFlowOperations(dataFlowOperations);
		appRegistryCommands.setDataFlowShell(dataFlowShell);
	}

	@Test
	public void testHintOnEmptyList() {
		Collection<AppRegistrationResource> data = new ArrayList<>();
		PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(data.size(), 1, data.size(), 1);
		PagedResources<AppRegistrationResource> result = new PagedResources<>(data, metadata);
		when(appRegistryOperations.list()).thenReturn(result);

		Object commandResult = appRegistryCommands.list(null);
		assertThat((String) commandResult, CoreMatchers.containsString("app register"));
		assertThat((String) commandResult, CoreMatchers.containsString("app import"));
	}

	@Test
	public void testList() {

		String[][] apps = new String[][] { { "http", "source" }, { "filter", "processor" },
				{ "transform", "processor" }, { "file", "source" }, { "log", "sink" },
				{ "moving-average", "processor" } };

		Collection<AppRegistrationResource> data = new ArrayList<>();
		for (String[] app : apps) {
			data.add(new AppRegistrationResource(app[0], app[1], null));
		}
		PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(data.size(), 1, data.size(), 1);
		PagedResources<AppRegistrationResource> result = new PagedResources<>(data, metadata);
		when(appRegistryOperations.list()).thenReturn(result);

		Object[][] expected = new String[][] { { "source", "processor", "sink", "task" },
				{ "http", "filter", "log", null }, { "file", "transform", null, null },
				{ null, "moving-average", null, null }, };
		TableModel model = ((Table) appRegistryCommands.list(null)).getModel();
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(model.getValue(row, col), Matchers.is(expected[row][col]));
			}
		}
	}

	@Test
	public void testUnknownModule() {
		List<Object> result = appRegistryCommands.info(null,"unknown", ApplicationType.processor);
		assertEquals((String) result.get(0), "Application info is not available for processor:unknown");
	}

	@Test
	public void register() {
		String name = "foo";
		ApplicationType type = ApplicationType.sink;
		String uri = "file:///foo";
		boolean force = false;
		AppRegistrationResource resource = new AppRegistrationResource(name, type.name(), uri);
		when(appRegistryOperations.register(name, type, uri, null, force)).thenReturn(resource);
		String result = appRegistryCommands.register(name, type, uri, null, force);
		assertEquals("Successfully registered application 'sink:foo'", result);
	}

	@Test
	public void importFromLocalResource() {
		String name1 = "foo";
		ApplicationType type1 = ApplicationType.source;
		String uri1 = "file:///foo";
		String name2 = "bar";
		ApplicationType type2 = ApplicationType.sink;
		String uri2 = "file:///bar";
		Properties apps = new Properties();
		apps.setProperty(type1.name() + "." + name1, uri1);
		apps.setProperty(type2.name() + "." + name2, uri2);
		List<AppRegistrationResource> resources = new ArrayList<>();
		resources.add(new AppRegistrationResource(name1, type1.name(), uri1));
		resources.add(new AppRegistrationResource(name2, type2.name(), uri2));
		PagedResources<AppRegistrationResource> pagedResources = new PagedResources<>(resources,
				new PagedResources.PageMetadata(resources.size(), 1, resources.size(), 1));
		when(appRegistryOperations.registerAll(apps, true)).thenReturn(pagedResources);
		String appsFileUri = "classpath:appRegistryCommandsTests-apps.properties";
		String result = appRegistryCommands.importFromResource(appsFileUri, true, true);
		assertEquals("Successfully registered applications: [source.foo, sink.bar]", result);
	}

	@Test
	public void importFromResource() {
		List<AppRegistrationResource> resources = new ArrayList<>();
		resources.add(new AppRegistrationResource("foo", "source", null));
		resources.add(new AppRegistrationResource("bar", "sink", null));
		PagedResources<AppRegistrationResource> pagedResources = new PagedResources<>(resources,
				new PagedResources.PageMetadata(resources.size(), 1, resources.size(), 1));
		String uri = "test://example";
		when(appRegistryOperations.importFromResource(uri, true)).thenReturn(pagedResources);
		String result = appRegistryCommands.importFromResource(uri, false, true);
		assertEquals("Successfully registered 2 applications from 'test://example'", result);
	}
}
