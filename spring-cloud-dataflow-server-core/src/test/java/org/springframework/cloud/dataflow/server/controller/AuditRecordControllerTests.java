/*
 * Copyright 2018-2023 the original author or authors.
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

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * @author Daniel Serleg
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class AuditRecordControllerTests {

	private static final int INITIAL_AUDIT_CREATE_COUNT = 6;

	private static final int FULL_AUDIT_CREATE_COUNT = 7;

	@Autowired
	private StreamDefinitionRepository streamDefinitionRepository;

	@Autowired
	private AuditRecordRepository auditRecordRepository;

	@Autowired
	private AppRegistrationRepository appRegistrationRepository;

	@Autowired
	private AppRegistryService appRegistryService;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private SkipperClient skipperClient;

	private ZonedDateTime startDate;

	private ZonedDateTime betweenDate;

	private ZonedDateTime endDate;

	@BeforeEach
	public void setupMocks() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();

		Info info = new Info();
		info.setStatus(new Status());
		info.getStatus().setStatusCode(StatusCode.DEPLOYED);
		when(skipperClient.status(ArgumentMatchers.anyString())).thenReturn(info);

		when(skipperClient.search(ArgumentMatchers.anyString(), ArgumentMatchers.eq(false)))
				.thenReturn(new ArrayList<PackageMetadata>());

		startDate = ZonedDateTime.now();

		mockMvc.perform(post("/streams/definitions/").param("name", "myStream").param("definition", "time | log")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(post("/streams/definitions/").param("name", "myStream1").param("definition", "time | log")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		// Verify that the 4 app create and 2 stream create audit records have been recorded before setting the between date.
		Awaitility.await().atMost(Duration.ofMillis(30000)).until(() -> auditRecordRepository.count() == INITIAL_AUDIT_CREATE_COUNT);

		betweenDate = ZonedDateTime.now();

		mockMvc.perform(post("/streams/definitions/").param("name", "myStream2").param("definition", "time | log")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		// Verify that the 4 app create and 3 stream create audit records have been recorded before setting the end date.
		Awaitility.await().atMost(Duration.ofMillis(30000)).until(() -> auditRecordRepository.count() == FULL_AUDIT_CREATE_COUNT);

		endDate = ZonedDateTime.now();

		mockMvc.perform(delete("/streams/definitions/myStream").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@AfterEach
	public void tearDown() {
		appRegistrationRepository.deleteAll();
		streamDefinitionRepository.deleteAll();
		auditRecordRepository.deleteAll();
		assertEquals(0, appRegistrationRepository.count());
		assertEquals(0, streamDefinitionRepository.count());
		assertEquals(0, auditRecordRepository.count());
	}

	/**
	 * Verify that the correct number of {@link AuditRecord}s are persisted to the database.
	 *
	 * Keep in mind that {@link StreamService#deleteStream(String)} calls
	 * {@link StreamService#deleteStream(String)} and
	 * {@link StreamService#undeployStream(String)} too.
	 */
	@Test
	public void testVerifyNumberOfAuditRecords() {
		assertEquals(4, appRegistrationRepository.count());
		assertEquals(2, streamDefinitionRepository.count());
		assertEquals(9, auditRecordRepository.count());
	}

	@Test
	public void testRetrieveAllAuditRecords() throws Exception {
		mockMvc.perform(get("/audit-records").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(9)));
	}


	@Test
	public void testRetrieveAllAuditRecordsOrderByCorrelationIdAsc() throws Exception {
		mockMvc.perform(get("/audit-records")
				.param("sort", "correlationId,asc")
				.param("sort", "id,asc")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(9)))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[*].correlationId", contains("filter", "log", "myStream",
						"myStream", "myStream", "myStream1", "myStream2", "time", "timestamp")));
	}

	@Test
	public void testRetrieveAllAuditRecordsOrderByCorrelationIdDesc() throws Exception {
		mockMvc.perform(get("/audit-records")
				.param("sort", "correlationId,desc")
				.param("sort", "id,desc")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(9)))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[*].correlationId", containsInAnyOrder("timestamp", "time", "filter",
						"myStream2", "myStream1", "myStream", "myStream", "myStream", "log")));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithActionUndeploy() throws Exception {
		mockMvc.perform(get("/audit-records?actions=UNDEPLOY").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(1)));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithOperationStream() throws Exception {
		mockMvc.perform(get("/audit-records?operations=STREAM").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(5)));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithOperationTask() throws Exception {
		mockMvc.perform(get("/audit-records?operations=TASK").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded").doesNotExist());
	}

	@Test
	public void testRetrieveAllAuditRecordsWithOperationTaskAndStream() throws Exception {
		mockMvc.perform(get("/audit-records?operations=TASK,STREAM").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(5)));
	}

	@Test
	public void testRetrieveAllAuditRecordsWithActionDeleteAndUndeploy() throws Exception {
		mockMvc.perform(get("/audit-records?actions=DELETE,UNDEPLOY").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(2)));
	}

	@Test
	public void testRetrieveAppRelatedAuditRecords() throws Exception {
		mockMvc.perform(get("/audit-records?operations=APP_REGISTRATION").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(4)));
	}

	@Test
	public void testRetrieveAuditRecordsWithActionCreate() throws Exception {
		mockMvc.perform(get("/audit-records?actions=CREATE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(7)));
	}

	@Test
	public void testRetrieveAuditActionTypes() throws Exception {
		mockMvc.perform(get("/audit-records/audit-action-types").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(7)))
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
				.andExpect(jsonPath("$[5].key", is("UPDATE")))

				.andExpect(jsonPath("$[6].id", is(700)))
				.andExpect(jsonPath("$[6].name", is("SuccessfulLogin")))
				.andExpect(jsonPath("$[6].description", is("Successful login")))
				.andExpect(jsonPath("$[6].key", is("LOGIN_SUCCESS")));
	}

	@Test
	public void testRetrieveAuditOperationTypes() throws Exception {
		mockMvc.perform(get("/audit-records/audit-operation-types").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(5)))

				.andExpect(jsonPath("$[0].id", is(100)))
				.andExpect(jsonPath("$[0].name", is("App Registration")))
				.andExpect(jsonPath("$[0].key", is("APP_REGISTRATION")))

				.andExpect(jsonPath("$[1].id", is(200)))
				.andExpect(jsonPath("$[1].name", is("Schedule")))
				.andExpect(jsonPath("$[1].key", is("SCHEDULE")))

				.andExpect(jsonPath("$[2].id", is(300)))
				.andExpect(jsonPath("$[2].name", is("Stream")))
				.andExpect(jsonPath("$[2].key", is("STREAM")))

				.andExpect(jsonPath("$[3].id", is(400)))
				.andExpect(jsonPath("$[3].name", is("Task")))
				.andExpect(jsonPath("$[3].key", is("TASK")))

				.andExpect(jsonPath("$[4].id", is(500)))
				.andExpect(jsonPath("$[4].name", is("Login")))
				.andExpect(jsonPath("$[4].key", is("LOGIN")));
	}

	@Test
	public void testRetrieveRegisteredAppsAuditData() throws Exception {
		mockMvc.perform(
				get("/audit-records?operations=APP_REGISTRATION&actions=CREATE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(4)))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[*].correlationId",
						containsInAnyOrder("filter", "log", "time", "timestamp")));
	}

	@Test
	public void testRetrieveDeletedAppsAuditData() throws Exception {
		mockMvc.perform(get("/audit-records").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(9)));

		appRegistryService.delete("filter", ApplicationType.processor, "3.2.1");

		mockMvc.perform(
				get("/audit-records?operations=APP_REGISTRATION&actions=DELETE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(1)))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditRecordId", is(14)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].correlationId", is("filter")));
	}

	@Test
	public void testRetrieveAuditRecordsFromNullToGivenDate() throws Exception {
		ZonedDateTime time = betweenDate.withZoneSameInstant(ZoneOffset.UTC);
		String toDate = time.toString();

		mockMvc.perform(get("/audit-records?toDate=" + toDate).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(6)))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[4].auditRecordId", is(9)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[4].correlationId", is("myStream")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[4].auditAction", is("CREATE")))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[5].auditRecordId", is(10)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[5].correlationId", is("myStream1")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[5].auditAction", is("CREATE")));
	}

	@Test
	public void testRetrieveAuditRecordsFromGivenDateToNull() throws Exception {
		ZonedDateTime betweenTime = endDate.withZoneSameInstant(ZoneOffset.UTC);
		String fromDate = betweenTime.toString();

		mockMvc.perform(get("/audit-records?fromDate=" + fromDate).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(greaterThanOrEqualTo(2))))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditRecordId", is(12)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].correlationId", is("myStream")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditAction", is("UNDEPLOY")))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].auditRecordId", is(13)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].correlationId", is("myStream")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].auditAction", is("DELETE")));
	}

	@Test
	public void testRetrieveAuditRecordsBetweenTwoGivenDates() throws Exception {
		ZonedDateTime betweenTime = betweenDate.withZoneSameInstant(ZoneOffset.UTC);
		String fromDate = betweenTime.toString();

		ZonedDateTime endTime = endDate.withZoneSameInstant(ZoneOffset.UTC);
		String toDate = endTime.toString();

		mockMvc.perform(get("/audit-records?fromDate=" + fromDate + "&toDate=" + toDate)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(greaterThanOrEqualTo(1))))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditRecordId", is(11)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].correlationId", is("myStream2")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditAction", is("CREATE")));
	}

	@Test
	public void testRetrieveAuditRecordsBetweenTwoGivenDatesWithFromDateAfterToDate() throws Exception {
		final String toDate = betweenDate.withZoneSameInstant(ZoneOffset.UTC).toString();
		final String fromDate = endDate.withZoneSameInstant(ZoneOffset.UTC).toString();

		mockMvc.perform(get("/audit-records")
				.param("fromDate", fromDate)
				.param("toDate", toDate)
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());
	}

	@Test
	public void testRetrieveAuditRecordsBetweenTwoNullDates() throws Exception {
		mockMvc.perform(get("/audit-records").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(greaterThanOrEqualTo(9))))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[4].auditRecordId", is(9)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[4].correlationId", is("myStream")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[4].auditAction", is("CREATE")));
	}

	@Test
	public void testRetrieveAuditRecordById() throws Exception {
		mockMvc.perform(get("/audit-records/13").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.auditRecordId", is(13)))
				.andExpect(jsonPath("$.correlationId", is("myStream")))
				.andExpect(jsonPath("$.auditAction", is("DELETE")));
	}

	@Test
	public void testRetrieveUpdatedAppsAuditData() throws Exception {
		mockMvc.perform(get("/audit-records?operations=APP_REGISTRATION").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(4)));

		AppRegistration filter = appRegistryService.find("filter", ApplicationType.processor, "3.2.1");
		appRegistryService.save(filter);

		mockMvc.perform(
				get("/audit-records?operations=APP_REGISTRATION&actions=UPDATE").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(1)))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditRecordId", is(14)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].correlationId", is("filter")));
	}

	@Test
	public void testRetrieveStreamAndTaskRecords() throws Exception {
		mockMvc.perform(get("/audit-records?operations=STREAM,TASK").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(5)));
	}

    @Test
	public void testRetrievePagedAuditDataNegative() throws Exception {
		mockMvc.perform(get("/audit-records?page=-5&size=2").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(2)));
	}

	@Test
	public void testRetrievePagedAuditDataInRange() throws Exception {
		mockMvc.perform(get("/audit-records?page=0&size=5").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(5)));
	}


	@Test
	public void testRetrievePagedAuditDataFromPage3() throws Exception {
		mockMvc.perform(get("/audit-records?page=2&size=4").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(1)));
	}

	@Test
	public void testRetrieveDeletedAndUndeployedStreamsAndTasks() throws Exception {
		mockMvc.perform(get("/audit-records?operations=STREAM,TASK&actions=DELETE,UNDEPLOY").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(2)))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditRecordId", is(12)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].correlationId", is("myStream")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditAction", is("UNDEPLOY")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditOperation", is("STREAM")))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].auditRecordId", is(13)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].correlationId", is("myStream")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].auditAction", is("DELETE")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].auditOperation", is("STREAM")));

	}

	@Test
	public void testRetrieveDataByOperationsAndActionsAndDate() throws Exception {
		ZonedDateTime startTime = startDate.withZoneSameInstant(ZoneOffset.UTC);
		String fromDate = startTime.toString();

		ZonedDateTime betweenTime = betweenDate.withZoneSameInstant(ZoneOffset.UTC);
		String toDate = betweenTime.toString();

		mockMvc.perform(get("/audit-records?fromDate=" + fromDate + "&toDate=" + toDate+"&actions=CREATE&operations=STREAM")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(greaterThanOrEqualTo(2))))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditRecordId", is(9)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].correlationId", is("myStream")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditAction", is("CREATE")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[0].auditOperation", is("STREAM")))

				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].auditRecordId", is(10)))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].correlationId", is("myStream1")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].auditAction", is("CREATE")))
				.andExpect(jsonPath("$._embedded.auditRecordResourceList[1].auditOperation", is("STREAM")));
	}

	@Test
	public void testRetrievePagedAuditDataOverlappingRightBound() throws Exception {
		mockMvc.perform(get("/audit-records?page=0&size=20").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*", hasSize(9)));
	}

	@Test
	public void testRetrievePagedAuditDataOutOfRange() throws Exception {
		mockMvc.perform(get("/audit-records?page=55&size=2").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.auditRecordResourceList.*").doesNotExist());
	}
}
