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

package org.springframework.cloud.dataflow.server.service.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.SimpleTestScheduler;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.SchedulerServiceProperties;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.scheduler.CreateScheduleException;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskServiceDependencies.class,
		PropertyPlaceholderAutoConfiguration.class }, properties = {
		"spring.cloud.dataflow.applicationProperties.task.globalkey=globalvalue",
		"spring.cloud.dataflow.applicationProperties.stream.globalstreamkey=nothere",
		"spring.main.allow-bean-definition-overriding=true",
		"spring.cloud.dataflow.task.scheduler-task-launcher-url=https://test.test",
		"spring.cloud.dataflow.features.schedules-enabled=true"})
@EnableConfigurationProperties({ CommonApplicationProperties.class, TaskConfigurationProperties.class,
		DockerValidatorProperties.class, ComposedTaskRunnerConfigurationProperties.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class DefaultSchedulerServiceTests {

	private static final String DATA_FLOW_SCHEDULER_PREFIX = "scheduler.";

	private static final String SCHEDULER_PREFIX = "spring.cloud.scheduler.";

	private static final String BASE_SCHEDULE_NAME = "myTaskSchedule";

	private static final String BASE_DEFINITION_NAME = "myTaskDefinition";

	private static final String CTR_DEFINITION_NAME= "myCtrDefinition";

	@Autowired
	private Scheduler simpleTestScheduler;

	@Autowired
	private SchedulerService schedulerService;

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	private SchedulerServiceProperties schedulerServiceProperties;

	@Autowired
	private AppRegistryService appRegistry;

	@Autowired
	private TaskConfigurationProperties taskConfigurationProperties;

	@Autowired
	private ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	@Autowired
	private CommonApplicationProperties commonApplicationProperties;

	@Autowired
	private ResourceLoader resourceLoader;

	@Autowired
	private  AuditRecordService auditRecordService;

	@Autowired
	private ApplicationConfigurationMetadataResolver metaDataResolver;

	@Autowired
	private Scheduler scheduler;

	private Map<String, String> testProperties;

	private Map<String, String> resolvedProperties;

	List<String> commandLineArgs;

	@Before
	public void setup() throws Exception{
		this.appRegistry.save("demo", ApplicationType.task, "1.0.0.", new URI("file:src/test/resources/apps/foo-task"), new URI("file:src/test/resources/apps/foo-task"));
		this.appRegistry.save("demo2", ApplicationType.task, "1.0.0", new URI("file:src/test/resources/apps/foo-task"), new URI("file:src/test/resources/apps/foo-task"));

		taskDefinitionRepository.save(new TaskDefinition(BASE_DEFINITION_NAME, "demo"));
		taskDefinitionRepository.save(new TaskDefinition(CTR_DEFINITION_NAME, "demo && demo2"));
		initializeSuccessfulRegistry();

		this.testProperties = new HashMap<>();
		this.testProperties.put(DATA_FLOW_SCHEDULER_PREFIX + "AAAA", "* * * * *");
		this.testProperties.put(DATA_FLOW_SCHEDULER_PREFIX + "EXPRESSION", "* * * * *");
		this.resolvedProperties = new HashMap<>();
		this.resolvedProperties.put(SCHEDULER_PREFIX + "AAAA", "* * * * *");
		this.resolvedProperties.put(SCHEDULER_PREFIX + "EXPRESSION", "* * * * *");
		this.commandLineArgs = new ArrayList<>();
	}

	@After
	public void tearDown() {
		((SimpleTestScheduler)simpleTestScheduler).getSchedules().clear();
	}

	@Test
	public void testSchedule(){
		schedulerService.schedule(BASE_SCHEDULE_NAME, BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testScheduleWithLongNameOnKuberenetesPlatform() {
		getMockedKubernetesSchedulerService().schedule(BASE_SCHEDULE_NAME +
				"1234567789012345612345678901234567890123", BASE_DEFINITION_NAME, this.testProperties,
				this.commandLineArgs, null);
	}

	@Test
	public void testScheduleWithCapitalizeNameOnKuberenetesPlatform() {
		SchedulerService testSchedulerService = getMockedKubernetesSchedulerService();
		testSchedulerService.schedule(BASE_SCHEDULE_NAME + "AB", BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		List<ScheduleInfo> scheduleInfos = testSchedulerService.list();
		assertThat(scheduleInfos.size()).isEqualTo(1);
		assertThat(scheduleInfos.get(0).getScheduleName()).isEqualTo("mytaskscheduleab");
	}

	private SchedulerService getMockedKubernetesSchedulerService() {
		Launcher launcher = new Launcher("default", "Kubernetes", Mockito.mock(TaskLauncher.class), scheduler);
		List<Launcher> launchers = new ArrayList<>();
		launchers.add(launcher);
		List<TaskPlatform> taskPlatform = Collections.singletonList(new TaskPlatform("testTaskPlatform", launchers));

		return new DefaultSchedulerService(this.commonApplicationProperties,
				taskPlatform, this.taskDefinitionRepository,
				this.appRegistry, this.resourceLoader,
				this.taskConfigurationProperties, mock(DataSourceProperties.class), null,
				this.metaDataResolver, this.schedulerServiceProperties, this.auditRecordService,
				this.composedTaskRunnerConfigurationProperties);
	}

	public void testScheduleWithLongName(){
		schedulerService.schedule(BASE_SCHEDULE_NAME + "12345677890123456",
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME));
	}

	@Test
	public void testScheduleCTR(){
		schedulerService.schedule(BASE_SCHEDULE_NAME, CTR_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME, CTR_DEFINITION_NAME));
	}

	@Test(expected = CreateScheduleException.class)
	public void testDuplicate(){
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1, BASE_DEFINITION_NAME,
				this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1, BASE_DEFINITION_NAME,
				this.testProperties, this.commandLineArgs);
	}

	@Test
	public void testMultipleSchedules(){
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);

		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 1));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 2));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 3));
	}

	@Test
	public void testRemoveSchedulesForTaskDefinitionName() {
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 4,
				CTR_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		validateSchedulesCount(4);
		schedulerService.unscheduleForTaskDefinition(BASE_DEFINITION_NAME);
		validateSchedulesCount(1);
		schedulerService.unscheduleForTaskDefinition(CTR_DEFINITION_NAME);
		validateSchedulesCount(0);
	}

	@Test
	public void testUnschedule(){
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);

		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 1));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 2));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 3));

		schedulerService.unschedule(BASE_SCHEDULE_NAME + 2);
		validateSchedulesCount(2);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 1));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 3));
	}

	@Test
	public void testEmptyUnschedule(){
		validateSchedulesCount(0);
		schedulerService.unschedule(BASE_SCHEDULE_NAME + 2);
		validateSchedulesCount(0);
	}

	@Test
	public void testList(){
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);

		List<ScheduleInfo> schedules = schedulerService.list();
		assertThat(schedules.size()).isEqualTo(3);
		verifyScheduleExistsInScheduler(schedules.get(0));
		verifyScheduleExistsInScheduler(schedules.get(1));
		verifyScheduleExistsInScheduler(schedules.get(2));
	}

	@Test
	public void testGetSchedule(){
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);

		ScheduleInfo schedule = schedulerService.getSchedule(BASE_SCHEDULE_NAME + 1);
		verifyScheduleExistsInScheduler(schedule);
		schedule = schedulerService.getSchedule(BASE_SCHEDULE_NAME + 2);
		verifyScheduleExistsInScheduler(schedule);
		schedule = schedulerService.getSchedule(BASE_SCHEDULE_NAME + 3);
		verifyScheduleExistsInScheduler(schedule);
	}


	@Test
	public void testListMaxEntry() {
		final int MAX_COUNT = 500;
		schedulerServiceProperties.setMaxSchedulesReturned(MAX_COUNT);
		for (int i = 0; i < MAX_COUNT + 1; i++) {
			schedulerService.schedule(BASE_SCHEDULE_NAME + i,
					BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		}
		List<ScheduleInfo> schedules = schedulerService.list();
		assertThat(schedules.size()).isEqualTo(MAX_COUNT);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testListPaginated() {
		schedulerService.list(PageRequest.of(0, 1));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testListWithParamsPaginated() {
		schedulerService.list(PageRequest.of(0, 1), BASE_DEFINITION_NAME);
	}

	@Test
	public void testListWithParams() {
		taskDefinitionRepository.save(new TaskDefinition(BASE_DEFINITION_NAME + 1, "demo"));
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME + 1, this.testProperties, this.commandLineArgs);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs);

		List<ScheduleInfo> schedules = schedulerService.list(BASE_DEFINITION_NAME + 1);
		assertThat(schedules.size()).isEqualTo(1);
		verifyScheduleExistsInScheduler(schedules.get(0));
	}

	@Test
	public void testEmptyList() {
		taskDefinitionRepository.save(new TaskDefinition(BASE_DEFINITION_NAME + 1, "demo"));
		List<ScheduleInfo> schedules = schedulerService.list(BASE_DEFINITION_NAME + 1, "testTaskPlatform");
		assertThat(schedules.size()).isEqualTo(0);
		schedules = schedulerService.list();
		assertThat(schedules.size()).isEqualTo(0);
	}

	@Test
	public void testScheduleWithCommandLineArguments() throws Exception{
		List<String> commandLineArguments = getCommandLineArguments(Arrays.asList("--myArg1", "--myArg2"));

		assertNotNull("Command line arguments should not be null", commandLineArguments);
		assertEquals("Invalid number of command line arguments", 2, commandLineArguments.size());
		assertEquals("Invalid command line argument", "--myArg1", commandLineArguments.get(0));
		assertEquals("Invalid command line argument", "--myArg2", commandLineArguments.get(1));
	}

	@Test
	public void testScheduleWithoutCommandLineArguments() throws Exception {
		List<String> commandLineArguments = getCommandLineArguments(new ArrayList<>());

		assertNotNull("Command line arguments should not be null", commandLineArguments);
		assertEquals("Invalid number of command line arguments", 0, commandLineArguments.size());
	}

	@Test
	public void testGetDefaultCTR() {
		ScheduleRequest request = getScheduleRequest(new ArrayList<>(), "springcloudtask/composed-task-runner:latest", "1: timestamp && 2: timestamp");
		AppDefinition definition = request.getDefinition();
		assertEquals("Docker Resource [docker:springcloudtask/composed-task-runner:latest]", request.getResource().toString());
	}

	private List<String> getCommandLineArguments(List<String> commandLineArguments) {
		return getScheduleRequest(commandLineArguments,"springcloudtask/timestamp-task:latest", "timestamp").getCommandlineArguments();
	}

	private ScheduleRequest getScheduleRequest(List<String> commandLineArguments, String resourceToReturn, String definition) {
		Scheduler mockScheduler = mock(SimpleTestScheduler.class);
		TaskDefinitionRepository mockTaskDefinitionRepository = mock(TaskDefinitionRepository.class);
		AppRegistryService mockAppRegistryService = mock(AppRegistryService.class);

		Launcher launcher = new Launcher("default", "defaultType", null, mockScheduler);
		List<Launcher> launchers = new ArrayList<>();
		launchers.add(launcher);
		List<TaskPlatform> taskPlatform = Collections.singletonList(new TaskPlatform("testTaskPlatform", launchers));
		SchedulerService mockSchedulerService = new DefaultSchedulerService(mock(CommonApplicationProperties.class),
				taskPlatform, mockTaskDefinitionRepository, mockAppRegistryService, mock(ResourceLoader.class),
				this.taskConfigurationProperties, mock(DataSourceProperties.class), "uri",
				mock(ApplicationConfigurationMetadataResolver.class), mock(SchedulerServiceProperties.class),
				mock(AuditRecordService.class), this.composedTaskRunnerConfigurationProperties);

		TaskDefinition taskDefinition = new TaskDefinition(BASE_DEFINITION_NAME, definition);

		when(mockTaskDefinitionRepository.findById(BASE_DEFINITION_NAME)).thenReturn(Optional.of(taskDefinition));
		when(mockAppRegistryService.getAppResource(any())).thenReturn(new DockerResource(resourceToReturn));
		when(mockAppRegistryService.find(taskDefinition.getRegisteredAppName(), ApplicationType.task))
				.thenReturn(new AppRegistration());
		mockSchedulerService.schedule(BASE_SCHEDULE_NAME, BASE_DEFINITION_NAME, this.testProperties,
				commandLineArguments, null);

		ArgumentCaptor<ScheduleRequest> scheduleRequestArgumentCaptor = ArgumentCaptor.forClass(ScheduleRequest.class);
		verify(mockScheduler).schedule(scheduleRequestArgumentCaptor.capture());
		return scheduleRequestArgumentCaptor.getValue();
	}

	private void verifyScheduleExistsInScheduler(ScheduleInfo scheduleInfo) {
		List<ScheduleInfo> scheduleInfos = schedulerService.list();
		scheduleInfos = scheduleInfos.stream().filter(s -> s.getScheduleName().
				equals(scheduleInfo.getScheduleName())).
				collect(Collectors.toList());

		assertThat(scheduleInfos.size()).isEqualTo(1);
		assertThat(scheduleInfos.get(0).getTaskDefinitionName()).isEqualTo(
				scheduleInfo.getTaskDefinitionName());

		for(String key: scheduleInfo.getScheduleProperties().keySet()) {
			assertThat(scheduleInfos.get(0).getScheduleProperties().
					get(key)).
					isEqualTo(scheduleInfo.getScheduleProperties().get(key));
		}
	}

	private void validateSchedulesCount(int expectedScheduleCount) {
		assertThat(((SimpleTestScheduler)simpleTestScheduler).
				getSchedules().size()).isEqualTo(expectedScheduleCount);
	}

	private ScheduleInfo createScheduleInfo(String scheduleName) {
		return createScheduleInfo(scheduleName, BASE_DEFINITION_NAME);
	}

	private ScheduleInfo createScheduleInfo(String scheduleName, String taskDefinitionName) {
		ScheduleInfo scheduleInfo = new ScheduleInfo();
		scheduleInfo.setScheduleName(scheduleName);
		scheduleInfo.setTaskDefinitionName(taskDefinitionName);
		scheduleInfo.setScheduleProperties(this.resolvedProperties);
		return scheduleInfo;
	}

	private void initializeSuccessfulRegistry() {
		when(this.appRegistry.find(anyString(), any(ApplicationType.class)))
				.thenReturn(new AppRegistration("demo", ApplicationType.task, URI.create("https://helloworld")));
		when(this.appRegistry.getAppResource(any())).thenReturn(mock(Resource.class));
		when(this.appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}

}
