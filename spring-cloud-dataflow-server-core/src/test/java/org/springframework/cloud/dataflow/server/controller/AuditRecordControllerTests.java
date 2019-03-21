/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.cloud.dataflow.server.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.impl.AbstractStreamService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the functionality of the {@link AuditRecordController}.
 *
 * @author Gunnar Hillert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class AuditRecordControllerTests {

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private AuditRecordRepository auditRecordRepository;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AppDeployer appDeployer;

	@Before
	public void setupMocks() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		when(appDeployer.deploy(any(AppDeploymentRequest.class))).thenReturn("testID");

		mockMvc.perform(post("/streams/definitions/").param("name", "myStream").param("definition", "time | log")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions/").param("name", "myStream1").param("definition", "time | log")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions/").param("name", "myStream2").param("definition", "time | log")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(delete("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON))
		.andExpect(status().isOk());

	}

	@After
	public void tearDown() {
		streamDefinitionRepository.deleteAll();
		auditRecordRepository.deleteAll();
		assertEquals(0, streamDefinitionRepository.count());
		assertEquals(0, auditRecordRepository.count());
	}

	/**
	 * Verify that the correct number of {@link AuditRecord}s are persisted to the database.
	 *
	 * Keep in mind that {@link AbstractStreamService#deleteStream(String)}
	 * does not only invokes {@link StreamDefinitionRepository#delete(String)} but also
	 * {@link AbstractStreamService#undeployStream(String).
	 */
	@Test
	public void testVerifyNumberOfAuditRecords() throws Exception {
		assertEquals(2, streamDefinitionRepository.count());
		assertEquals(5, auditRecordRepository.count());
	}

	@Test
	public void testRetrieveAllAuditRecords() throws Exception {
		mockMvc.perform(get("/audit-records").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.content.*", hasSize(5)));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithActionUndeploy() throws Exception {
		mockMvc.perform(get("/audit-records?actions=UNDEPLOY").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.content.*", hasSize(1)));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithOperationStream() throws Exception {
		mockMvc.perform(get("/audit-records?operations=STREAM").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.content.*", hasSize(5)));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithOperationTask() throws Exception {
		mockMvc.perform(get("/audit-records?operations=TASK").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.content.*", hasSize(0)));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithOperationTaskAndStream() throws Exception {
		mockMvc.perform(get("/audit-records?operations=TASK,STREAM").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.content.*", hasSize(5)));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithActionDeleteAndUndeploy() throws Exception {
		mockMvc.perform(get("/audit-records?actions=DELETE,UNDEPLOY").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.content.*", hasSize(2)));
	}

	@Test
	public void testRetrieveAuditActionTypes() throws Exception {
		mockMvc.perform(get("/audit-records/audit-action-types").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.*", hasSize(6)))
		.andExpect(jsonPath("$[0].id", is(100)))
		.andExpect(jsonPath("$[0].name", is("Create")))
		.andExpect(jsonPath("$[0].description", is("Create an Entity")))
		.andExpect(jsonPath("$[0].key", is("CREATE")))

		.andExpect(jsonPath("$[1].id", is(200)))
		.andExpect(jsonPath("$[1].name", is("Delete")))
		.andExpect(jsonPath("$[1].description", is("Delete an Entity")))
		.andExpect(jsonPath("$[1].key", is("DELETE")))

		.andExpect(jsonPath("$[2].id", is(300)))
		.andExpect(jsonPath("$[2].name", is("Deploy")))
		.andExpect(jsonPath("$[2].description", is("Deploy an Entity")))
		.andExpect(jsonPath("$[2].key", is("DEPLOY")))

		.andExpect(jsonPath("$[3].id", is(400)))
		.andExpect(jsonPath("$[3].name", is("Rollback")))
		.andExpect(jsonPath("$[3].description", is("Rollback an Entity")))
		.andExpect(jsonPath("$[3].key", is("ROLLBACK")))

		.andExpect(jsonPath("$[4].id", is(500)))
		.andExpect(jsonPath("$[4].name", is("Undeploy")))
		.andExpect(jsonPath("$[4].description", is("Undeploy an Entity")))
		.andExpect(jsonPath("$[4].key", is("UNDEPLOY")))

		.andExpect(jsonPath("$[5].id", is(600)))
		.andExpect(jsonPath("$[5].name", is("Update")))
		.andExpect(jsonPath("$[5].description", is("Update an Entity")))
		.andExpect(jsonPath("$[5].key", is("UPDATE")));
	}

	@Test
	public void testRetrieveAuditOperationTypes() throws Exception {
		mockMvc.perform(get("/audit-records/audit-operation-types").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.*", hasSize(3)))

		/* Commented out until the implementation of gh-2486
		.andExpect(jsonPath("$[0].id", is(100)))
		.andExpect(jsonPath("$[0].name", is("App Registration")))
		.andExpect(jsonPath("$[0].key", is("APP_REGISTRATION")))
		*/

		.andExpect(jsonPath("$[0].id", is(200)))
		.andExpect(jsonPath("$[0].name", is("Schedule")))
		.andExpect(jsonPath("$[0].key", is("SCHEDULE")))

		.andExpect(jsonPath("$[1].id", is(300)))
		.andExpect(jsonPath("$[1].name", is("Stream")))
		.andExpect(jsonPath("$[1].key", is("STREAM")))

		.andExpect(jsonPath("$[2].id", is(400)))
		.andExpect(jsonPath("$[2].name", is("Task")))
		.andExpect(jsonPath("$[2].key", is("TASK")));
	}
}
