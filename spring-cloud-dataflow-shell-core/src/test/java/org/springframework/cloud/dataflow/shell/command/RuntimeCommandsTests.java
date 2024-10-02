/*
 * Copyright 2016-2022 the original author or authors.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.RuntimeOperations;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedModel;
import org.springframework.shell.table.TableModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RuntimeCommands}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 * @author Corneil du Plessis
 */
class RuntimeCommandsTests {

	private RuntimeCommands runtimeCommands;

	@Mock
	private DataFlowOperations dataFlowOperations;

	@Mock
	private RuntimeOperations runtimeOperations;

	private AppStatusResource appStatusResource1;

	private AppStatusResource appStatusResource2;

	private AppStatusResource appStatusResource3;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(dataFlowOperations.runtimeOperations()).thenReturn(runtimeOperations);
		DataFlowShell dataFlowShell = new DataFlowShell();
		dataFlowShell.setDataFlowOperations(dataFlowOperations);
		this.runtimeCommands = new RuntimeCommands(dataFlowShell, new ObjectMapper());
		appStatusResource1 = new AppStatusResource("1", "deployed");
		Map<String, String> properties = new HashMap<>();
		properties.put("key1", "value1");
		properties.put("key2", "value1");
		AppInstanceStatusResource instanceStatusResource1 = new AppInstanceStatusResource("10", "deployed", properties);
		AppInstanceStatusResource instanceStatusResource2 = new AppInstanceStatusResource("20", "deployed", null);
		List<AppInstanceStatusResource> instanceStatusResources1 = new ArrayList<>();
		instanceStatusResources1.add(instanceStatusResource1);
		instanceStatusResources1.add(instanceStatusResource2);
		PagedModel.PageMetadata metadata1 = new PagedModel.PageMetadata(instanceStatusResources1.size(), 1,
				instanceStatusResources1.size(), 1);
		PagedModel<AppInstanceStatusResource> resources = PagedModel.of(instanceStatusResources1, metadata1);
		appStatusResource1.setInstances(resources);
		appStatusResource2 = new AppStatusResource("2", "undeployed");
		AppInstanceStatusResource instanceStatusResource3 = new AppInstanceStatusResource("30", "undeployed", null);
		AppInstanceStatusResource instanceStatusResource4 = new AppInstanceStatusResource("40", "undeployed", null);
		List<AppInstanceStatusResource> instanceStatusResources2 = new ArrayList<>();
		instanceStatusResources1.add(instanceStatusResource3);
		instanceStatusResources1.add(instanceStatusResource4);
		PagedModel.PageMetadata metadata3 = new PagedModel.PageMetadata(instanceStatusResources2.size(), 1,
				instanceStatusResources2.size(), 1);
		PagedModel<AppInstanceStatusResource> resources2 = PagedModel.of(instanceStatusResources2,
				metadata3);
		appStatusResource2.setInstances(resources2);
		appStatusResource3 = new AppStatusResource("3", "failed");
		AppInstanceStatusResource instanceStatusResource5 = new AppInstanceStatusResource("50", "failed", null);
		AppInstanceStatusResource instanceStatusResource6 = new AppInstanceStatusResource("60", "deployed", null);
		List<AppInstanceStatusResource> instanceStatusResources3 = new ArrayList<>();
		instanceStatusResources1.add(instanceStatusResource5);
		instanceStatusResources1.add(instanceStatusResource6);
		PagedModel.PageMetadata metadata4 = new PagedModel.PageMetadata(instanceStatusResources3.size(), 1,
				instanceStatusResources3.size(), 1);
		PagedModel<AppInstanceStatusResource> resources3 = PagedModel.of(instanceStatusResources3,
				metadata4);
		appStatusResource3.setInstances(resources3);
	}

	@Test
	void statusWithSummary() {
		Collection<AppStatusResource> data = new ArrayList<>();
		data.add(appStatusResource1);
		data.add(appStatusResource2);
		data.add(appStatusResource3);
		PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(data.size(), 1, data.size(), 1);
		PagedModel<AppStatusResource> result = PagedModel.of(data, metadata);
		when(runtimeOperations.status()).thenReturn(result);
		Object[][] expected = new String[][] { { "1", "deployed", "2" }, { "2", "undeployed", "0" },
				{ "3", "failed", "0" } };
		TableModel model = runtimeCommands.list(true, null).getModel();
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(String.valueOf(model.getValue(row + 1, col))).isEqualTo(expected[row][col]);
			}
		}
	}

	@Test
	void statusWithoutSummary() {
		Collection<AppStatusResource> data = new ArrayList<>();
		data.add(appStatusResource1);
		data.add(appStatusResource2);
		PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(data.size(), 1, data.size(), 1);
		PagedModel<AppStatusResource> result = PagedModel.of(data, metadata);
		when(runtimeOperations.status()).thenReturn(result);
		Object[][] expected = new String[][] { { "1", "deployed", "2" }, { "10", "deployed" }, { "20", "deployed" },
				{ "2", "undeployed", "0" } };
		TableModel model = runtimeCommands.list(false, null).getModel();
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(String.valueOf(model.getValue(row + 1, col))).isEqualTo(expected[row][col]);
			}
		}
	}

	@Test
	void statusByModuleId() {
		when(runtimeOperations.status("1")).thenReturn(appStatusResource1);
		Object[][] expected = new String[][] { { "1", "deployed", "2" }, { "10", "deployed" }, { "20", "deployed" } };
		TableModel model = runtimeCommands.list(false, new String[] { "1" }).getModel();
		assertThat(model.getRowCount()).isEqualTo(4);
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(String.valueOf(model.getValue(row + 1, col))).isEqualTo(expected[row][col]);
			}
		}
	}

	@Test
	void actuatorGet() {
		String json = "{ \"name\": \"foo\" }";
		when(runtimeOperations.getFromActuator("flipflop3.log-v1", "flipflop3.log-v1-0", "info")).thenReturn(json);
		assertThat(runtimeCommands.getFromActuator("flipflop3.log-v1", "flipflop3.log-v1-0", "info")).isEqualTo(json);
	}

	@Test
	void actuatorPostWithoutData() {
		runtimeCommands.postToActuator("flipflop3.log-v1", "flipflop3.log-v1-0", "info", null);
		verify(runtimeOperations).postToActuator("flipflop3.log-v1", "flipflop3.log-v1-0", "info", Collections.emptyMap());
	}

	@Test
	void actuatorPostWithData() throws Exception {
		SummaryInfo summaryInfo = new SummaryInfo();
		summaryInfo.setName("highLevel");
		summaryInfo.getDetails().add(new DetailInfo("line1 details"));
		summaryInfo.getDetails().add(new DetailInfo("line2 details"));
		ObjectMapper objectMapper = new ObjectMapper();
		String dataJsonStr = objectMapper.writeValueAsString(summaryInfo);

		Map<String, Object> expectedDataMap = new HashMap<>();
		expectedDataMap.put("name", "highLevel");
		expectedDataMap.put("details", Arrays.asList(
				Collections.singletonMap("description", "line1 details"),
				Collections.singletonMap("description", "line2 details")));

		runtimeCommands.postToActuator("flipflop3.log-v1", "flipflop3.log-v1-0", "info", dataJsonStr);

		verify(runtimeOperations).postToActuator("flipflop3.log-v1", "flipflop3.log-v1-0", "info", expectedDataMap);
	}

	@Test
	void actuatorPostWithInvalidData() {
		assertThatThrownBy(() -> runtimeCommands.postToActuator("flipflop3.log-v1", "flipflop3.log-v1-0",
				"info", "{invalidJsonStr}")).isInstanceOf(RuntimeException.class).hasMessageContaining("Unable to parse 'data' into map:");
	}

	static class SummaryInfo {
		private String name;
		private List<DetailInfo> details = new ArrayList<>();

		public SummaryInfo() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<DetailInfo> getDetails() {
			return details;
		}

		public void setDetails(List<DetailInfo> details) {
			this.details = details;
		}
	}

	static class DetailInfo {
		private String description;

		public DetailInfo(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

}
