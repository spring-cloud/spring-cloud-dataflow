/*
 * Copyright 2020-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.server.service.impl.DefaultSchedulerServiceTestUtil.assertThatCommandLineArgsHaveNonDefaultArgs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.scheduler.CreateScheduleException;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = {TaskServiceDependencies.class,
		DefaultSchedulerServiceMultiplatformTests.MultiplatformTaskConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class}, properties = {
		"spring.cloud.dataflow.applicationProperties.task.globalkey=globalvalue",
		"spring.cloud.dataflow.applicationProperties.stream.globalstreamkey=nothere",
		"spring.main.allow-bean-definition-overriding=true",
		"spring.cloud.dataflow.task.scheduler-task-launcher-url=https://test.test",
		"spring.cloud.dataflow.features.schedules-enabled=true"})
@EnableConfigurationProperties({CommonApplicationProperties.class, TaskConfigurationProperties.class,
		DockerValidatorProperties.class, ComposedTaskRunnerConfigurationProperties.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class DefaultSchedulerServiceMultiplatformTests {

	private static final String DATA_FLOW_SCHEDULER_PREFIX = "scheduler.";

	private static final String DEPLOYER_PREFIX = "spring.cloud.deployer.";

	private static final String BASE_SCHEDULE_NAME = "mytaskschedule";

	private static final String BASE_DEFINITION_NAME = "myTaskDefinition";

	private static final String CTR_DEFINITION_NAME = "myCtrDefinition";

	private static final String KUBERNETES_PLATFORM = "kubernetesPlatform";

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
	private AuditRecordService auditRecordService;

	@Autowired
	private ApplicationConfigurationMetadataResolver metaDataResolver;

	@Autowired
	private Scheduler scheduler;

	@Autowired
	private TaskExecutionInfoService taskExecutionInfoService;

	@Autowired
	private PropertyResolver propertyResolver;

	private Map<String, String> testProperties;

	private Map<String, String> resolvedProperties;

	List<String> commandLineArgs;

	@BeforeEach
	void setup() throws Exception {
		when(this.appRegistry.find(
			eq("demo"), eq(ApplicationType.task), eq("1.0.0"))).thenReturn(new AppRegistration("demo",
			ApplicationType.task, new URI("file:src/test/resources/apps/foo-task")));
		when(this.appRegistry.find(
			eq("demo2"), eq(ApplicationType.task), eq("1.0.0"))).thenReturn(new AppRegistration("demo2",
			ApplicationType.task, new URI("file:src/test/resources/apps/foo-task")));
		taskDefinitionRepository.save(new TaskDefinition(BASE_DEFINITION_NAME, "demo"));
		taskDefinitionRepository.save(new TaskDefinition(CTR_DEFINITION_NAME, "demo && demo2"));
		initializeSuccessfulRegistry();

		this.testProperties = new HashMap<>();
		this.testProperties.put(DATA_FLOW_SCHEDULER_PREFIX + "AAAA", "* * * * *");
		this.testProperties.put(DATA_FLOW_SCHEDULER_PREFIX + "EXPRESSION", "* * * * *");
		this.testProperties.put("version." + BASE_DEFINITION_NAME, "1.0.0");
		this.resolvedProperties = new HashMap<>();
		this.resolvedProperties.put(DEPLOYER_PREFIX + "AAAA", "* * * * *");
		this.resolvedProperties.put(DEPLOYER_PREFIX + "EXPRESSION", "* * * * *");
		this.commandLineArgs = new ArrayList<>();
	}

	@AfterEach
	void tearDown() {
		((SimpleTestScheduler) simpleTestScheduler).getSchedules().clear();
	}

	@Test
	void testSchedule() {
		schedulerService.schedule(BASE_SCHEDULE_NAME, BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME));
	}

	@Test
	void scheduleWithNoVersion() {
		this.testProperties.remove("version." + BASE_DEFINITION_NAME);
		schedulerService.schedule(BASE_SCHEDULE_NAME, BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME));
	}

	@Test
	void scheduleWithLongNameOnKuberenetesPlatform() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			getMockedKubernetesSchedulerService().schedule(BASE_SCHEDULE_NAME +
						"1234567789012345612345678901234567890123", BASE_DEFINITION_NAME, this.testProperties,
				this.commandLineArgs, null);
		});
	}

	@Test
	void scheduleWithCapitalizeNameOnKuberenetesPlatform() {
		SchedulerService testSchedulerService = getMockedKubernetesSchedulerService();
		testSchedulerService.schedule(BASE_SCHEDULE_NAME + "AB", BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		List<ScheduleInfo> scheduleInfos = testSchedulerService.listForPlatform(KUBERNETES_PLATFORM);
		assertThat(scheduleInfos).hasSize(1);
		assertThat(scheduleInfos.get(0).getScheduleName()).isEqualTo("mytaskscheduleab");
	}

	private SchedulerService getMockedKubernetesSchedulerService() {
		Launcher launcher = new Launcher(KUBERNETES_PLATFORM, "Kubernetes", Mockito.mock(TaskLauncher.class), scheduler);
		List<Launcher> launchers = new ArrayList<>();
		launchers.add(launcher);
		List<TaskPlatform> taskPlatform = Collections.singletonList(new TaskPlatform(KUBERNETES_PLATFORM, launchers));

		return new DefaultSchedulerService(
				this.commonApplicationProperties,
				taskPlatform,
				this.taskDefinitionRepository,
				this.appRegistry,
				this.resourceLoader,
				this.taskConfigurationProperties,
				mock(DataSourceProperties.class),
				null,
				this.metaDataResolver,
				this.schedulerServiceProperties,
				this.auditRecordService,
				this.taskExecutionInfoService,
				this.propertyResolver,
				this.composedTaskRunnerConfigurationProperties
		);
	}

	public void testScheduleWithLongName() {
		schedulerService.schedule(BASE_SCHEDULE_NAME + "12345677890123456",
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, null);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME));
	}

	@Test
	void scheduleCTR() {
		schedulerService.schedule(BASE_SCHEDULE_NAME, CTR_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME, CTR_DEFINITION_NAME));
	}

	@Test
	void duplicate() {
		assertThatExceptionOfType(CreateScheduleException.class).isThrownBy(() -> {
			schedulerService.schedule(BASE_SCHEDULE_NAME + 1, BASE_DEFINITION_NAME,
					this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
			schedulerService.schedule(BASE_SCHEDULE_NAME + 1, BASE_DEFINITION_NAME,
					this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		});
	}

	@Test
	void multipleSchedules() {
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);

		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 1));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 2));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 3));
	}

	@Test
	void testGetSchedule() {
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);

		ScheduleInfo schedule = schedulerService.getSchedule(BASE_SCHEDULE_NAME + 1, KUBERNETES_PLATFORM);
		verifyScheduleExistsInScheduler(schedule);
		schedule = schedulerService.getSchedule(BASE_SCHEDULE_NAME + 2, KUBERNETES_PLATFORM);
		verifyScheduleExistsInScheduler(schedule);
		schedule = schedulerService.getSchedule(BASE_SCHEDULE_NAME + 3, KUBERNETES_PLATFORM);
		verifyScheduleExistsInScheduler(schedule);
	}

	@Test
	void removeSchedulesForTaskDefinitionName() {
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 4,
				CTR_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		validateSchedulesCount(4);
		schedulerService.unscheduleForTaskDefinition(BASE_DEFINITION_NAME);
		validateSchedulesCount(1);
		schedulerService.unscheduleForTaskDefinition(CTR_DEFINITION_NAME);
		validateSchedulesCount(0);
	}

	@Test
	void testUnschedule() {
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);

		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 1));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 2));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 3));

		schedulerService.unschedule(BASE_SCHEDULE_NAME + 2, KUBERNETES_PLATFORM);
		validateSchedulesCount(2);
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 1));
		verifyScheduleExistsInScheduler(createScheduleInfo(BASE_SCHEDULE_NAME + 3));
	}

	@Test
	void emptyUnschedule() {
		validateSchedulesCount(0);
		schedulerService.unschedule(BASE_SCHEDULE_NAME + 2, KUBERNETES_PLATFORM);
		validateSchedulesCount(0);
	}

	@Test
	void list() {
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);

		List<ScheduleInfo> schedules = schedulerService.listForPlatform(KUBERNETES_PLATFORM);
		assertThat(schedules).hasSize(3);
		verifyScheduleExistsInScheduler(schedules.get(0));
		verifyScheduleExistsInScheduler(schedules.get(1));
		verifyScheduleExistsInScheduler(schedules.get(2));
	}

	@Test
	void listMaxEntry() {
		final int MAX_COUNT = 500;
		schedulerServiceProperties.setMaxSchedulesReturned(MAX_COUNT);
		for (int i = 0; i < MAX_COUNT + 1; i++) {
			schedulerService.schedule(BASE_SCHEDULE_NAME + i,
					BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		}
		List<ScheduleInfo> schedules = schedulerService.listForPlatform(KUBERNETES_PLATFORM);
		assertThat(schedules).hasSize(MAX_COUNT);
	}

	@Test
	void listPaginated() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			schedulerService.list(PageRequest.of(0, 1), null);
		});
	}

	@Test
	void listWithParamsPaginated() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			schedulerService.list(PageRequest.of(0, 1), BASE_DEFINITION_NAME);
		});
	}

	@Test
	void listWithParams() {
		taskDefinitionRepository.save(new TaskDefinition(BASE_DEFINITION_NAME + 1, "demo"));
		schedulerService.schedule(BASE_SCHEDULE_NAME + 1,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 2,
				BASE_DEFINITION_NAME + 1, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);
		schedulerService.schedule(BASE_SCHEDULE_NAME + 3,
				BASE_DEFINITION_NAME, this.testProperties, this.commandLineArgs, KUBERNETES_PLATFORM);

		List<ScheduleInfo> schedules = schedulerService.list(BASE_DEFINITION_NAME + 1, KUBERNETES_PLATFORM);
		assertThat(schedules).hasSize(1);
		verifyScheduleExistsInScheduler(schedules.get(0));
	}

	@Test
	void emptyList() {
		taskDefinitionRepository.save(new TaskDefinition(BASE_DEFINITION_NAME + 1, "demo"));
		List<ScheduleInfo> schedules = schedulerService.list(BASE_DEFINITION_NAME + 1, "testTaskPlatform");
		assertThat(schedules).isEmpty();
		schedules = schedulerService.listForPlatform(KUBERNETES_PLATFORM);
		assertThat(schedules).isEmpty();
	}

	@Test
	void scheduleWithCommandLineArguments() throws Exception {
		List<String> args = new ArrayList<>();
		args.add("--myArg1");
		args.add("--myArg2");
		args = getCommandLineArguments(args);
		assertThatCommandLineArgsHaveNonDefaultArgs(args, "--app.timestamp", "--myArg1", "--myArg2");
	}

	@Test
	void scheduleWithoutCommandLineArguments() throws URISyntaxException {
		List<String> args = getCommandLineArguments(new ArrayList<>());
		assertThatCommandLineArgsHaveNonDefaultArgs(args, "--app.timestamp");
	}

	private List<String> getCommandLineArguments(List<String> commandLineArguments) throws URISyntaxException {
		Scheduler mockScheduler = mock(SimpleTestScheduler.class);
		TaskDefinitionRepository mockTaskDefinitionRepository = mock(TaskDefinitionRepository.class);
		AppRegistryService mockAppRegistryService = mock(AppRegistryService.class);
		when(mockAppRegistryService.find(
			eq("timestamp"), eq(ApplicationType.task), eq("1.0.0"))).
			thenReturn(new AppRegistration("timestamp", ApplicationType.task,
				new URI("file:src/test/resources/apps/timestamp-task")));


		Launcher launcher = new Launcher("default", "defaultType", null, mockScheduler);
		List<Launcher> launchers = new ArrayList<>();
		launchers.add(launcher);
		List<TaskPlatform> taskPlatform = Collections.singletonList(new TaskPlatform("testTaskPlatform", launchers));
		SchedulerService schedulerService = new DefaultSchedulerService(
				mock(CommonApplicationProperties.class),
				taskPlatform,
				mockTaskDefinitionRepository,
				mockAppRegistryService,
				mock(ResourceLoader.class),
				this.taskConfigurationProperties,
				mock(DataSourceProperties.class),
				"uri",
				mock(ApplicationConfigurationMetadataResolver.class),
				mock(SchedulerServiceProperties.class),
				mock(AuditRecordService.class),
				mock(TaskExecutionInfoService.class),
				mock(PropertyResolver.class),
				this.composedTaskRunnerConfigurationProperties);

		TaskDefinition taskDefinition = new TaskDefinition(BASE_DEFINITION_NAME, "timestamp");

		when(mockTaskDefinitionRepository.findById(BASE_DEFINITION_NAME)).thenReturn(Optional.of(taskDefinition));
		when(mockAppRegistryService.getAppResource(any())).thenReturn(new DockerResource("springcloudtask/timestamp-task:latest"));
		when(mockAppRegistryService.find(taskDefinition.getRegisteredAppName(), ApplicationType.task))
				.thenReturn(new AppRegistration());
		schedulerService.schedule(BASE_SCHEDULE_NAME, BASE_DEFINITION_NAME, this.testProperties,
				commandLineArguments, null);

		ArgumentCaptor<ScheduleRequest> scheduleRequestArgumentCaptor = ArgumentCaptor.forClass(ScheduleRequest.class);
		verify(mockScheduler).schedule(scheduleRequestArgumentCaptor.capture());

		return scheduleRequestArgumentCaptor.getValue().getCommandlineArguments();
	}

	private void verifyScheduleExistsInScheduler(ScheduleInfo scheduleInfo) {
		List<ScheduleInfo> scheduleInfos = schedulerService.listForPlatform(KUBERNETES_PLATFORM);
		scheduleInfos = scheduleInfos.stream().filter(s -> s.getScheduleName().
						equals(scheduleInfo.getScheduleName())).
				collect(Collectors.toList());

		assertThat(scheduleInfos).hasSize(1);
		assertThat(scheduleInfos.get(0).getTaskDefinitionName()).isEqualTo(
				scheduleInfo.getTaskDefinitionName());

		for (String key : scheduleInfo.getScheduleProperties().keySet()) {
			assertThat(scheduleInfos.get(0).getScheduleProperties()).containsEntry(key, scheduleInfo.getScheduleProperties().get(key));
		}
	}

	private void validateSchedulesCount(int expectedScheduleCount) {
		assertThat(((SimpleTestScheduler) simpleTestScheduler).
				getSchedules()).hasSize(expectedScheduleCount);
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

	@Configuration
	static class MultiplatformTaskConfiguration {
		@Bean
		public TaskPlatform taskCFPlatform(Scheduler scheduler) {
			Launcher launcher = new Launcher(KUBERNETES_PLATFORM, "Kubernetes", Mockito.mock(TaskLauncher.class), scheduler);
			List<Launcher> launchers = new ArrayList<>();
			launchers.add(launcher);
			TaskPlatform taskPlatform = new TaskPlatform(KUBERNETES_PLATFORM, launchers);
			return taskPlatform;
		}
	}

}
