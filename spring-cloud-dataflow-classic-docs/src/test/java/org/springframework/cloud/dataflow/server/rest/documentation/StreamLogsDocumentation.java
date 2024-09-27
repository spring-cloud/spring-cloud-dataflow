/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.rest.documentation;

import java.util.HashMap;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runners.MethodSorters;

import org.springframework.cloud.skipper.domain.LogInfo;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Documentation for the {@code /streams/logs} endpoint.
 *
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class StreamLogsDocumentation extends BaseDocumentation {

	@Test
	public void getLogsByStreamName() throws Exception {
		LogInfo logInfo = new LogInfo();
		Map<String, String> logs = new HashMap<>();
		logs.put("ticktock-log-v1", "Logs-log");
		logs.put("ticktock-time-v1", "Logs-time");
		logInfo.setLogs(logs);
		when(springDataflowServer.getSkipperClient().getLog("ticktock")).thenReturn(logInfo);
		this.mockMvc.perform(
			get("/streams/logs/ticktock"))
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document());
	}

	@Test
	public void getLogsByAppName() throws Exception {
		LogInfo logInfo = new LogInfo();
		Map<String, String> logs = new HashMap<>();
		logs.put("ticktock-log-v1", "Logs-log");
		logInfo.setLogs(logs);
		when(springDataflowServer.getSkipperClient().getLog("ticktock", "ticktock-log-v1")).thenReturn(logInfo);
		this.mockMvc.perform(
				get("/streams/logs/ticktock/ticktock-log-v1"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document());
	}

}
