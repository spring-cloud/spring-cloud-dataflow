/*
 * Copyright 2018-2020 the original author or authors.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.configuration.SimpleTestScheduler;
import org.springframework.cloud.dataflow.server.configuration.TestDependencies;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.SchedulerPropertyKeys;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the {@link TaskSchedulerController}.
 *
 * @author Glenn Renfro
 * @author Christian Tzolov
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = { "spring.cloud.dataflow.task.scheduler-task-launcher-url=https://test.test" })
public class TaskSchedulerControllerTests {

	@Autowired
	SchedulerService schedulerService;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private TaskDefinitionRepository repository;

	@Autowired
	private AppRegistryService registry;

	@Autowired
	private SimpleTestScheduler simpleTestScheduler;

	@Autowired
	private AuditRecordRepository auditRecordRepository;

	private MockMvc mockMvc;

	@BeforeEach
	public void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@Test
	public void testTaskSchedulerControllerConstructorMissingService() {
		assertThrows(IllegalArgumentException.class, () -> {
			new TaskSchedulerController(null);
		});
	}

	@Test
	public void testListSchedules() throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null, null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		createSampleSchedule("schedule1");
		createSampleSchedule("schedule2");
		mockMvc.perform(get("/tasks/schedules").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.scheduleInfoResourceList[*].scheduleName", containsInAnyOrder("schedule1", "schedule2")))
				.andExpect(jsonPath("$._embedded.scheduleInfoResourceList", hasSize(2)));
	}

	@Test
	public void testGetSchedule() throws Exception {

		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null, null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		createSampleSchedule("schedule1");
		createSampleSchedule("schedule2");
		mockMvc.perform(get("/tasks/schedules/schedule1").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().json("{scheduleName: \"schedule1\"}"))
				.andExpect(content().json("{taskDefinitionName: \"testDefinition\"}"));
		mockMvc.perform(get("/tasks/schedules/schedule2").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().json("{scheduleName: \"schedule2\"}"))
				.andExpect(content().json("{taskDefinitionName: \"testDefinition\"}"));
		mockMvc.perform(get("/tasks/schedules/scheduleNotExisting").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("_embedded.errors.[0].message", is("Schedule [scheduleNotExisting] doesn't exist")))
				.andExpect(jsonPath("_embedded.errors.[0].logref", is("NoSuchScheduleException")));
	}

	@Test
	public void testListSchedulesByTaskDefinitionName() throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null, null);

		repository.save(new TaskDefinition("foo", "testApp"));
		repository.save(new TaskDefinition("bar", "testApp"));

		createSampleSchedule("foo", "schedule1");
		createSampleSchedule("bar", "schedule2");
		mockMvc.perform(get("/tasks/schedules/instances/bar").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.scheduleInfoResourceList[*].scheduleName", containsInAnyOrder("schedule2")))
				.andExpect(jsonPath("$._embedded.scheduleInfoResourceList", hasSize(1)));
	}

	@Test
	public void testCreateSchedule() throws Exception {
		createAndVerifySchedule("mySchedule", "mySchedule");
	}

	@Test
	public void testCreateScheduleWithLeadingAndTrailingBlanks() throws Exception {
		createAndVerifySchedule("    mySchedule    ", "mySchedule");
	}

	@Test
	public void testCreateScheduleLeadingBlanks() throws Exception {
		createAndVerifySchedule("    mySchedule", "mySchedule");
	}

	@Test
	public void testCreateScheduleTrailingBlanks() throws Exception {
		createAndVerifySchedule("mySchedule      ", "mySchedule");
	}

	private void createAndVerifySchedule(String scheduleName, String createdScheduleName) throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null, null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		mockMvc.perform(post("/tasks/schedules/").param("taskDefinitionName", "testDefinition")
				.param("scheduleName", scheduleName).param("properties", "scheduler.cron.expression=* * * * *")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertEquals(1, simpleTestScheduler.list().size());
		ScheduleInfo scheduleInfo = simpleTestScheduler.list().get(0);
		assertEquals(createdScheduleName, scheduleInfo.getScheduleName());
		assertEquals(1, scheduleInfo.getScheduleProperties().size());
		assertEquals("* * * * *", scheduleInfo.getScheduleProperties().get("spring.cloud.deployer.cron.expression"));

		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();

		assertEquals(6, auditRecords.size());
		final AuditRecord auditRecord = auditRecords.get(5);

		assertEquals(AuditOperationType.SCHEDULE, auditRecord.getAuditOperation());
		assertEquals(AuditActionType.CREATE, auditRecord.getAuditAction());
		assertEquals("mySchedule", auditRecord.getCorrelationId());

		JSONAssert.assertEquals("{\"commandlineArguments\":[\"--app.testApp.spring.cloud.task.initialize-enabled=false\",\"--app.testApp.spring.batch.jdbc.table-prefix=BATCH_\",\"--app.testApp.spring.cloud.task.tablePrefix=TASK_\",\"--app.testApp.spring.cloud.task.schemaTarget=boot2\",\"--app.testApp.spring.cloud.deployer.bootVersion=2\"]," +
				"\"taskDefinitionName\":\"testDefinition\"," +
				"\"taskDefinitionProperties\":{\"management.metrics.tags.service\":\"task-application\"," +
				"\"spring.datasource.username\":null,\"spring.datasource.url\":null," +
				"\"spring.datasource.driverClassName\":null," +
				"\"management.metrics.tags.application\":\"${spring.cloud.task.name:unknown}-${spring.cloud.task.executionid:unknown}\"," +
				"\"spring.cloud.task.initialize-enabled\":\"false\",\"spring.batch.jdbc.table-prefix\":\"BATCH_\",\"spring.cloud.task.schemaTarget\":\"boot2\"," +
				"\"spring.cloud.task.name\":\"testDefinition\",\"spring.cloud.task.tablePrefix\":\"TASK_\",\"spring.cloud.deployer.bootVersion\":\"2\"}," +
				"\"deploymentProperties\":{\"spring.cloud.deployer.cron.expression\":\"* * * * *\"}}", auditRecord.getAuditData(), JSONCompareMode.LENIENT);
	}

	@Test
	public void testCreateScheduleWithSensitiveFields() throws Exception {
		String auditData = createScheduleWithArguments("argument1=foo password=secret");
		JSONAssert.assertEquals("{\"commandlineArguments\":[\"argument1=foo\",\"password=******\"," +
						"\"--app.testApp.spring.cloud.task.initialize-enabled=false\",\"--app.testApp.spring.batch.jdbc.table-prefix=BATCH_\"," +
						"\"--app.testApp.spring.cloud.task.tablePrefix=TASK_\",\"--app.testApp.spring.cloud.task.schemaTarget=boot2\"," +
						"\"--app.testApp.spring.cloud.deployer.bootVersion=2\"],\"taskDefinitionName\":\"testDefinition\"," +
						"\"taskDefinitionProperties\":{\"prop2.secret\":\"******\",\"spring.datasource.driverClassName\":null," +
						"\"management.metrics.tags.application\":\"${spring.cloud.task.name:unknown}-${spring.cloud.task.executionid:unknown}\"," +
						"\"spring.cloud.task.name\":\"testDefinition\",\"spring.cloud.deployer.bootVersion\":\"2\",\"management.metrics.tags.service\":\"task-application\"," +
						"\"prop1\":\"foo\",\"spring.datasource.username\":null,\"spring.datasource.url\":null,\"spring.cloud.task.initialize-enabled\":\"false\"," +
						"\"spring.batch.jdbc.table-prefix\":\"BATCH_\",\"spring.cloud.task.schemaTarget\":\"boot2\",\"spring.cloud.task.tablePrefix\":\"TASK_\"}," +
						"\"deploymentProperties\":{\"spring.cloud.deployer.prop1.secret\":\"******\",\"spring.cloud.deployer.prop2.password\":\"******\",\"spring.cloud.deployer.cron.expression\":\"* * * * *\"}}",
				auditData, JSONCompareMode.LENIENT);
	}

	@Test
	public void testCreateScheduleCommaDelimitedArgs() throws Exception {
		String auditData = createScheduleWithArguments("argument1=foo spring.profiles.active=k8s,master argument3=bar");

		JSONAssert.assertEquals("{\"commandlineArguments\":[\"argument1=foo\",\"spring.profiles.active=k8s,master\"," +
						"\"argument3=bar\",\"--app.testApp.spring.cloud.task.initialize-enabled=false\",\"--app.testApp.spring.batch.jdbc.table-prefix=BATCH_\"," +
						"\"--app.testApp.spring.cloud.task.tablePrefix=TASK_\",\"--app.testApp.spring.cloud.task.schemaTarget=boot2\"," +
						"\"--app.testApp.spring.cloud.deployer.bootVersion=2\"],\"taskDefinitionName\":\"testDefinition\"," +
						"\"taskDefinitionProperties\":{\"prop2.secret\":\"******\",\"spring.datasource.driverClassName\":null," +
						"\"management.metrics.tags.application\":\"${spring.cloud.task.name:unknown}-${spring.cloud.task.executionid:unknown}\"," +
						"\"spring.cloud.task.name\":\"testDefinition\",\"spring.cloud.deployer.bootVersion\":\"2\"," +
						"\"management.metrics.tags.service\":\"task-application\",\"prop1\":\"foo\",\"spring.datasource.username\":null," +
						"\"spring.datasource.url\":null,\"spring.cloud.task.initialize-enabled\":\"false\",\"spring.batch.jdbc.table-prefix\":\"BATCH_\"," +
						"\"spring.cloud.task.schemaTarget\":\"boot2\",\"spring.cloud.task.tablePrefix\":\"TASK_\"}," +
						"\"deploymentProperties\":{\"spring.cloud.deployer.prop1.secret\":\"******\",\"spring.cloud.deployer.prop2.password\":\"******\"," +
						"\"spring.cloud.deployer.cron.expression\":\"* * * * *\"}}",
				auditData, JSONCompareMode.LENIENT);
	}

	private String createScheduleWithArguments(String arguments) throws Exception {
		this.registry.save("testApp", ApplicationType.task, "1.0.0", new URI("file:src/test/resources/apps/foo-task"), null, null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		mockMvc.perform(post("/tasks/schedules/").param("taskDefinitionName", "testDefinition")
				.param("scheduleName", "mySchedule")
				.param("properties",
						"scheduler.cron.expression=* * * * *,app.testApp.prop1=foo,app.testApp.prop2.secret=kenny,deployer.*.prop1.secret=cartman,deployer.*.prop2.password=kyle")
				.param("arguments", arguments)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		assertEquals(1, simpleTestScheduler.list().size());
		ScheduleInfo scheduleInfo = simpleTestScheduler.list().get(0);
		assertEquals("mySchedule", scheduleInfo.getScheduleName());
		assertEquals(3, scheduleInfo.getScheduleProperties().size());
		assertEquals("* * * * *", scheduleInfo.getScheduleProperties().get("spring.cloud.deployer.cron.expression"));

		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();

		assertEquals(6, auditRecords.size());
		final AuditRecord auditRecord = auditRecords.get(5);

		assertEquals(AuditOperationType.SCHEDULE, auditRecord.getAuditOperation());
		assertEquals(AuditActionType.CREATE, auditRecord.getAuditAction());
		assertEquals("mySchedule", auditRecord.getCorrelationId());

		return auditRecord.getAuditData();
	}

	@Test
	public void testCreateScheduleBadCron() throws Exception {
		AppRegistration registration = this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null, null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		mockMvc.perform(post("/tasks/schedules/").param("taskDefinitionName", "testDefinition")
				.param("scheduleName", "myScheduleBadCron")
				.param("properties",
						"scheduler.cron.expression=" + SimpleTestScheduler.INVALID_CRON_EXPRESSION)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	public void testRemoveSchedulesByTaskName() throws Exception {
		AppRegistration registration = this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null, null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		createSampleSchedule("mySchedule");
		createSampleSchedule("mySchedule2");
		assertEquals(2, simpleTestScheduler.list().size());
		mockMvc.perform(delete("/tasks/schedules/instances/testDefinition").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		assertEquals(0, simpleTestScheduler.list().size());
	}


	@Test
	public void testRemoveSchedule() throws Exception {
		AppRegistration registration = this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null, null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		createSampleSchedule("mySchedule");
		assertEquals(1, simpleTestScheduler.list().size());
		mockMvc.perform(delete("/tasks/schedules/" + "mySchedule").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		assertEquals(0, simpleTestScheduler.list().size());

		AuditActionType[] auditActionTypesCreate = { AuditActionType.CREATE };
		final Page<AuditRecord> auditRecordsCreate = auditRecordRepository.findByActionTypeAndOperationTypeAndDate(null,
				auditActionTypesCreate, null, null, PageRequest.of(0, 6));

		AuditActionType[] auditActionTypesDelete = { AuditActionType.DELETE };
		final Page<AuditRecord> auditRecordsDelete = auditRecordRepository.findByActionTypeAndOperationTypeAndDate(null,
				auditActionTypesDelete,
				null, null, PageRequest.of(0, 6));

		assertEquals(6, auditRecordsCreate.getContent().size());
		assertEquals(1, auditRecordsDelete.getContent().size());
		final AuditRecord auditRecord = auditRecordsDelete.getContent().get(0);

		assertEquals(AuditOperationType.SCHEDULE, auditRecord.getAuditOperation());
		assertEquals(AuditActionType.DELETE, auditRecord.getAuditAction());
		assertEquals("mySchedule", auditRecord.getCorrelationId());
		assertEquals("testDefinition", auditRecord.getAuditData());
	}

	private void createSampleSchedule(String scheduleName) {
		createSampleSchedule("testDefinition", scheduleName);
	}

	private void createSampleSchedule(String taskDefinitionName, String scheduleName) {
		Map<String, String> properties = new HashMap<>();
		properties.put("scheduler.testApp." + SchedulerPropertyKeys.CRON_EXPRESSION, "* * * * *");
		schedulerService.schedule(scheduleName, taskDefinitionName, properties, new ArrayList<>(), null);
	}

}
