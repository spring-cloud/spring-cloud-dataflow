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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * Tests for the {@link TaskSchedulerController}.
 *
 * @author Glenn Renfro
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = TestDependencies.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {"spring.cloud.dataflow.task.scheduler-task-launcher-url=https://test.test"})
class TaskSchedulerControllerTests {

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
	void setupMockMVC() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
	}

	@Test
	void taskSchedulerControllerConstructorMissingService() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			new TaskSchedulerController(null);
		});
	}

	@Test
	void listSchedules() throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		createSampleSchedule("schedule1");
		createSampleSchedule("schedule2");
		mockMvc.perform(get("/tasks/schedules").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.scheduleInfoResourceList[*].scheduleName", containsInAnyOrder("schedule1", "schedule2")))
				.andExpect(jsonPath("$._embedded.scheduleInfoResourceList", hasSize(2)));
	}

	@Test
	void getSchedule() throws Exception {

		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		createSampleSchedule("schedule1");
		createSampleSchedule("schedule2");
		mockMvc.perform(get("/tasks/schedules/schedule1").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(content().json("{scheduleName: \"schedule1\"}"))
				.andExpect(content().json("{taskDefinitionName: \"testDefinition\"}"));
		mockMvc.perform(get("/tasks/schedules/schedule2").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isOk())
				.andExpect(content().json("{scheduleName: \"schedule2\"}"))
				.andExpect(content().json("{taskDefinitionName: \"testDefinition\"}"));
		mockMvc.perform(get("/tasks/schedules/scheduleNotExisting").accept(MediaType.APPLICATION_JSON))
				.andDo(print()).andExpect(status().isNotFound())
				.andExpect(jsonPath("_embedded.errors.[0].message", is("Schedule [scheduleNotExisting] doesn't exist")))
				.andExpect(jsonPath("_embedded.errors.[0].logref", is("NoSuchScheduleException")));
	}

	@Test
	void listSchedulesByTaskDefinitionName() throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null);

		repository.save(new TaskDefinition("foo", "testApp"));
		repository.save(new TaskDefinition("bar", "testApp"));

		createSampleSchedule("foo", "schedule1");
		createSampleSchedule("bar", "schedule2");
		mockMvc.perform(get("/tasks/schedules/instances/bar").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.scheduleInfoResourceList[*].scheduleName", containsInAnyOrder("schedule2")))
				.andExpect(jsonPath("$._embedded.scheduleInfoResourceList", hasSize(1)));
	}

	@Test
	void createSchedule() throws Exception {
		createAndVerifySchedule("mySchedule", "mySchedule");
	}

	@Test
	void createScheduleWithLeadingAndTrailingBlanks() throws Exception {
		createAndVerifySchedule("    mySchedule    ", "mySchedule");
	}

	@Test
	void createScheduleLeadingBlanks() throws Exception {
		createAndVerifySchedule("    mySchedule", "mySchedule");
	}

	@Test
	void createScheduleTrailingBlanks() throws Exception {
		createAndVerifySchedule("mySchedule      ", "mySchedule");
	}

	private void createAndVerifySchedule(String scheduleName, String createdScheduleName) throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		mockMvc.perform(post("/tasks/schedules").param("taskDefinitionName", "testDefinition")
				.param("scheduleName", scheduleName).param("properties", "scheduler.cron.expression=* * * * *")
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		assertThat(simpleTestScheduler.list()).hasSize(1);
		ScheduleInfo scheduleInfo = simpleTestScheduler.list().get(0);
		assertThat(scheduleInfo.getScheduleName()).isEqualTo(createdScheduleName);
		assertThat(scheduleInfo.getScheduleProperties()).hasSize(1);
		assertThat(scheduleInfo.getScheduleProperties()).containsEntry("spring.cloud.deployer.cron.expression", "* * * * *");

		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();

		assertThat(auditRecords).hasSize(6);
		final AuditRecord auditRecord = auditRecords.get(5);

		assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.SCHEDULE);
		assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.CREATE);
		assertThat(auditRecord.getCorrelationId()).isEqualTo("mySchedule");

		JSONAssert.assertEquals("{\"commandlineArguments\":[\"--app.testApp.spring.cloud.task.initialize-enabled=false\"]," +
				"\"taskDefinitionName\":\"testDefinition\"," +
				"\"taskDefinitionProperties\":{\"management.metrics.tags.service\":\"task-application\"," +
				"\"spring.datasource.username\":null,\"spring.datasource.url\":null," +
				"\"spring.datasource.driverClassName\":null," +
				"\"management.metrics.tags.application\":\"${spring.cloud.task.name:unknown}-${spring.cloud.task.executionid:unknown}\"," +
				"\"spring.cloud.task.initialize-enabled\":\"false\"," +
				"\"spring.cloud.task.name\":\"testDefinition\"}," +
				"\"deploymentProperties\":{\"spring.cloud.deployer.cron.expression\":\"* * * * *\"}}", auditRecord.getAuditData(), JSONCompareMode.LENIENT);
	}

	@Test
	void createScheduleWithSensitiveFields() throws Exception {
		String auditData = createScheduleWithArguments("argument1=foo password=secret");
		JSONAssert.assertEquals("{\"commandlineArguments\":[\"argument1=foo\",\"password=******\"," +
						"\"--app.testApp.spring.cloud.task.initialize-enabled=false\"],\"taskDefinitionName\":\"testDefinition\"," +
						"\"taskDefinitionProperties\":{\"prop2.secret\":\"******\",\"spring.datasource.driverClassName\":null," +
						"\"management.metrics.tags.application\":\"${spring.cloud.task.name:unknown}-${spring.cloud.task.executionid:unknown}\"," +
						"\"spring.cloud.task.name\":\"testDefinition\",\"management.metrics.tags.service\":\"task-application\"," +
						"\"prop1\":\"foo\",\"spring.datasource.username\":null,\"spring.datasource.url\":null,\"spring.cloud.task.initialize-enabled\":\"false\"}," +
						"\"deploymentProperties\":{\"spring.cloud.deployer.prop1.secret\":\"******\",\"spring.cloud.deployer.prop2.password\":\"******\",\"spring.cloud.deployer.cron.expression\":\"* * * * *\"}}",
				auditData, JSONCompareMode.LENIENT);
	}

	@Test
	void createScheduleCommaDelimitedArgs() throws Exception {
		String auditData = createScheduleWithArguments("argument1=foo spring.profiles.active=k8s,master argument3=bar");

		JSONAssert.assertEquals("{\"commandlineArguments\":[\"argument1=foo\",\"spring.profiles.active=k8s,master\"," +
						"\"argument3=bar\",\"--app.testApp.spring.cloud.task.initialize-enabled=false\"],\"taskDefinitionName\":\"testDefinition\"," +
						"\"taskDefinitionProperties\":{\"prop2.secret\":\"******\",\"spring.datasource.driverClassName\":null," +
						"\"management.metrics.tags.application\":\"${spring.cloud.task.name:unknown}-${spring.cloud.task.executionid:unknown}\"," +
						"\"spring.cloud.task.name\":\"testDefinition\"," +
						"\"management.metrics.tags.service\":\"task-application\",\"prop1\":\"foo\",\"spring.datasource.username\":null," +
						"\"spring.datasource.url\":null,\"spring.cloud.task.initialize-enabled\":\"false\"}," +
						"\"deploymentProperties\":{\"spring.cloud.deployer.prop1.secret\":\"******\",\"spring.cloud.deployer.prop2.password\":\"******\"," +
						"\"spring.cloud.deployer.cron.expression\":\"* * * * *\"}}",
				auditData, JSONCompareMode.LENIENT);
	}

	private String createScheduleWithArguments(String arguments) throws Exception {
		this.registry.save("testApp", ApplicationType.task, "1.0.0", new URI("file:src/test/resources/apps/foo-task"), null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		mockMvc.perform(post("/tasks/schedules").param("taskDefinitionName", "testDefinition")
				.param("scheduleName", "mySchedule")
				.param("properties",
						"scheduler.cron.expression=* * * * *,app.testApp.prop1=foo,app.testApp.prop2.secret=kenny,deployer.*.prop1.secret=cartman,deployer.*.prop2.password=kyle")
				.param("arguments", arguments)
				.accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isCreated());
		assertThat(simpleTestScheduler.list()).hasSize(1);
		ScheduleInfo scheduleInfo = simpleTestScheduler.list().get(0);
		assertThat(scheduleInfo.getScheduleName()).isEqualTo("mySchedule");
		assertThat(scheduleInfo.getScheduleProperties()).hasSize(3);
		assertThat(scheduleInfo.getScheduleProperties()).containsEntry("spring.cloud.deployer.cron.expression", "* * * * *");

		final List<AuditRecord> auditRecords = auditRecordRepository.findAll();

		assertThat(auditRecords).hasSize(6);
		final AuditRecord auditRecord = auditRecords.get(5);

		assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.SCHEDULE);
		assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.CREATE);
		assertThat(auditRecord.getCorrelationId()).isEqualTo("mySchedule");

		return auditRecord.getAuditData();
	}

	@Test
	void createScheduleBadCron() throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		mockMvc.perform(post("/tasks/schedules").param("taskDefinitionName", "testDefinition")
				.param("scheduleName", "myScheduleBadCron")
				.param("properties",
						"scheduler.cron.expression=" + SimpleTestScheduler.INVALID_CRON_EXPRESSION)
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	void removeSchedulesByTaskName() throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		createSampleSchedule("mySchedule");
		createSampleSchedule("mySchedule2");
		assertThat(simpleTestScheduler.list()).hasSize(2);
		mockMvc.perform(delete("/tasks/schedules/instances/testDefinition").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		assertThat(simpleTestScheduler.list()).isEmpty();
	}


	@Test
	void removeSchedule() throws Exception {
		this.registry.save("testApp", ApplicationType.task,
				"1.0.0", new URI("file:src/test/resources/apps/foo-task"), null);

		repository.save(new TaskDefinition("testDefinition", "testApp"));
		createSampleSchedule("mySchedule");
		assertThat(simpleTestScheduler.list()).hasSize(1);
		mockMvc.perform(delete("/tasks/schedules/" + "mySchedule").accept(MediaType.APPLICATION_JSON)).andDo(print())
				.andExpect(status().isOk());
		assertThat(simpleTestScheduler.list()).isEmpty();

		AuditActionType[] auditActionTypesCreate = { AuditActionType.CREATE };
		final Page<AuditRecord> auditRecordsCreate = auditRecordRepository.findByActionTypeAndOperationTypeAndDate(null,
				auditActionTypesCreate, null, null, PageRequest.of(0, 6));

		AuditActionType[] auditActionTypesDelete = { AuditActionType.DELETE };
		final Page<AuditRecord> auditRecordsDelete = auditRecordRepository.findByActionTypeAndOperationTypeAndDate(null,
				auditActionTypesDelete,
				null, null, PageRequest.of(0, 6));

		assertThat(auditRecordsCreate.getContent()).hasSize(6);
		assertThat(auditRecordsDelete.getContent()).hasSize(1);
		final AuditRecord auditRecord = auditRecordsDelete.getContent().get(0);

		assertThat(auditRecord.getAuditOperation()).isEqualTo(AuditOperationType.SCHEDULE);
		assertThat(auditRecord.getAuditAction()).isEqualTo(AuditActionType.DELETE);
		assertThat(auditRecord.getCorrelationId()).isEqualTo("mySchedule");
		assertThat(auditRecord.getAuditData()).isEqualTo("testDefinition");
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
