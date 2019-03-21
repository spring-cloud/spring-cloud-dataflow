/*
 * Copyright 2016 the original author or authors.
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

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.RuntimeOperations;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.table.TableModel;

/**
 * Unit tests for {@link RuntimeCommands}.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RuntimeCommandsTests {

	private RuntimeCommands runtimeCommands;

	@Mock
	private DataFlowOperations dataFlowOperations;

	@Mock
	private RuntimeOperations runtimeOperations;

	private AppStatusResource appStatusResource1;

	private AppStatusResource appStatusResource2;

	private AppStatusResource appStatusResource3;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(dataFlowOperations.runtimeOperations()).thenReturn(runtimeOperations);
		DataFlowShell dataFlowShell = new DataFlowShell();
		dataFlowShell.setDataFlowOperations(dataFlowOperations);
		this.runtimeCommands = new RuntimeCommands(dataFlowShell);
		appStatusResource1 = new AppStatusResource("1", "deployed");
		Map<String, String> properties = new HashMap<>();
		properties.put("key1", "value1");
		properties.put("key2", "value1");
		AppInstanceStatusResource instanceStatusResource1 = new AppInstanceStatusResource("10", "deployed", properties);
		AppInstanceStatusResource instanceStatusResource2 = new AppInstanceStatusResource("20", "deployed", null);
		List<AppInstanceStatusResource> instanceStatusResources1 = new ArrayList<>();
		instanceStatusResources1.add(instanceStatusResource1);
		instanceStatusResources1.add(instanceStatusResource2);
		PagedResources.PageMetadata metadata1 = new PagedResources.PageMetadata(instanceStatusResources1.size(), 1, instanceStatusResources1.size(), 1);
		PagedResources<AppInstanceStatusResource> resources = new PagedResources<>(instanceStatusResources1, metadata1);
		appStatusResource1.setInstances(resources);
		appStatusResource2 = new AppStatusResource("2", "undeployed");
		AppInstanceStatusResource instanceStatusResource3 = new AppInstanceStatusResource("30", "undeployed", null);
		AppInstanceStatusResource instanceStatusResource4 = new AppInstanceStatusResource("40", "undeployed", null);
		List<AppInstanceStatusResource> instanceStatusResources2 = new ArrayList<>();
		instanceStatusResources1.add(instanceStatusResource3);
		instanceStatusResources1.add(instanceStatusResource4);
		PagedResources.PageMetadata metadata3 = new PagedResources.PageMetadata(instanceStatusResources2.size(), 1, instanceStatusResources2.size(), 1);
		PagedResources<AppInstanceStatusResource> resources2 = new PagedResources<>(instanceStatusResources2, metadata3);
		appStatusResource2.setInstances(resources2);
		appStatusResource3 = new AppStatusResource("3", "failed");
		AppInstanceStatusResource instanceStatusResource5 = new AppInstanceStatusResource("50", "failed", null);
		AppInstanceStatusResource instanceStatusResource6 = new AppInstanceStatusResource("60", "deployed", null);
		List<AppInstanceStatusResource> instanceStatusResources3 = new ArrayList<>();
		instanceStatusResources1.add(instanceStatusResource5);
		instanceStatusResources1.add(instanceStatusResource6);
		PagedResources.PageMetadata metadata4 = new PagedResources.PageMetadata(instanceStatusResources3.size(), 1, instanceStatusResources3.size(), 1);
		PagedResources<AppInstanceStatusResource> resources3 = new PagedResources<>(instanceStatusResources3, metadata4);
		appStatusResource3.setInstances(resources3);
	}

	@Test
	public void testStatusWithSummary() {
		Collection<AppStatusResource> data = new ArrayList<>();
		data.add(appStatusResource1);
		data.add(appStatusResource2);
		data.add(appStatusResource3);
		PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(data.size(), 1, data.size(), 1);
		PagedResources<AppStatusResource> result = new PagedResources<>(data, metadata);
		when(runtimeOperations.status()).thenReturn(result);
		Object[][] expected = new String[][] {
				{"1", "deployed", "2"},
				{"2", "undeployed", "0"},
				{"3", "failed", "0"}
		};
		TableModel model = runtimeCommands.list(true, null).getModel();
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(String.valueOf(model.getValue(row + 1, col)), Matchers.is(expected[row][col]));
			}
		}
	}

	@Test
	public void testStatusWithoutSummary() {
		Collection<AppStatusResource> data = new ArrayList<>();
		data.add(appStatusResource1);
		data.add(appStatusResource2);
		PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(data.size(), 1, data.size(), 1);
		PagedResources<AppStatusResource> result = new PagedResources<>(data, metadata);
		when(runtimeOperations.status()).thenReturn(result);
		Object[][] expected = new String[][] {
				{"1", "deployed", "2"},
				{"10", "deployed"},
				{"20", "deployed"},
				{"2", "undeployed", "0"}
		};
		TableModel model = runtimeCommands.list(false, null).getModel();
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(String.valueOf(model.getValue(row + 1, col)), Matchers.is(expected[row][col]));
			}
		}
	}

	@Test
	public void testStatusByModuleId() {
		when(runtimeOperations.status("1")).thenReturn(appStatusResource1);
		Object[][] expected = new String[][] {
				{"1", "deployed", "2"},
				{"10", "deployed"},
				{"20", "deployed"}
		};
		TableModel model = runtimeCommands.list(false, new String[] {"1"}).getModel();
		assertTrue(model.getRowCount() == 4);
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(String.valueOf(model.getValue(row + 1, col)), Matchers.is(expected[row][col]));
			}
		}
	}

}
