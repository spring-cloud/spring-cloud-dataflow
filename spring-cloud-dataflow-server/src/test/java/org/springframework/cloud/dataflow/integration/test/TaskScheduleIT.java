/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.integration.test.util.DockerComposeFactoryProperties;
import org.springframework.cloud.dataflow.integration.test.util.RuntimeApplicationHelper;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.dsl.task.Task;
import org.springframework.cloud.dataflow.rest.client.dsl.task.TaskSchedule;
import org.springframework.cloud.dataflow.rest.client.dsl.task.TaskScheduleBuilder;
import org.springframework.cloud.deployer.spi.scheduler.SchedulerPropertyKeys;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Since the Schedule SPI is not supported on local platforms, following ITs are disabled for Docker Compose SCDF installation.
 *
 * For this set the test.docker.compose.disable.extension property (or TEST_DOCKER_COMPOSE_DISABLE_EXTENSION) variable to true.
 * Use the test.docker.compose.dataflowServerUrl property (or TEST_DOCKER_COMPOSE_DATAFLOW_SERVER_URL) variable
 * to configure the DataFLow entry point.
 *
 * For example to run the following test suite against SCDF Kubernetes cluster deployed on GKE:
 * <code>
 *    ./mvnw clean install -pl spring-cloud-dataflow-server -Dtest=foo -DfailIfNoTests=false \
 *        -Dtest.docker.compose.disable.extension=true \
 *        -Dtest.docker.compose.dataflowServerUrl=https://scdf-server.gke.io \
 *        -Pfailsafe
 * </code>
 *
 * @author Christian Tzolov
 */
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(DataFlowITProperties.class)
@EnabledIfSystemProperty(named = DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DISABLE_EXTENSION, matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskScheduleIT {
	private static final Logger logger = LoggerFactory.getLogger(TaskScheduleIT.class);

	private final static boolean dockerComposeDisabled = DockerComposeFactoryProperties.isDockerComposeDisabled();

	private final static String DEFAULT_CRON_EXPRESSION = "56 20 ? * *";

	@Autowired
	private DataFlowITProperties testProperties;

	/**
	 * REST and DSL clients used to interact with the SCDF server and run the tests.
	 */
	private boolean enabledScheduler;
	private String platformInfo;
	private DataFlowTemplate dataFlowOperations;

	@BeforeAll
	public static void beforeAll() {
		logger.info("[{} = {}]", DockerComposeFactoryProperties.TEST_DOCKER_COMPOSE_DISABLE_EXTENSION, dockerComposeDisabled);
	}

	@BeforeEach
	public void before() {
		Assumptions.assumingThat(dockerComposeDisabled, () -> {
			dataFlowOperations = new DataFlowTemplate(URI.create(testProperties.getDataflowServerUrl()));
			enabledScheduler = dataFlowOperations.aboutOperation().get().getFeatureInfo().isSchedulesEnabled();
			Awaitility.setDefaultPollInterval(Duration.ofSeconds(5));
			Awaitility.setDefaultTimeout(Duration.ofMinutes(10));

			RuntimeApplicationHelper runtime = new RuntimeApplicationHelper(dataFlowOperations,
					testProperties.getPlatformName(), testProperties.getKubernetesAppHostSuffix());
			platformInfo = String.format("[platform = %s, type = %s]", runtime.getPlatformName(), runtime.getPlatformType());
		});
	}

	@AfterEach
	public void after() {
		if (dockerComposeDisabled) {
			dataFlowOperations.taskOperations().destroyAll();
		}
	}

	@Test
	@Order(Integer.MIN_VALUE)
	public void testConfigurationInfo() {
		logger.info("[{} = {}]", "test.docker.compose.dataflowServerUrl", testProperties.getDataflowServerUrl());
		logger.info(platformInfo);
		if (!enabledScheduler) {
			logger.info("scheduler is disabled");
		}
	}

	@Test
	public void listTest() {

		Assumptions.assumeTrue(enabledScheduler);

		logger.info("schedule-list-test");

		TaskScheduleBuilder taskScheduleBuilder = TaskSchedule.builder(dataFlowOperations);

		try (Task task1 = Task.builder(dataFlowOperations).name(randomName("task1")).definition("timestamp").build();
			 Task task2 = Task.builder(dataFlowOperations).name(randomName("task2")).definition("timestamp").build();

			 TaskSchedule taskSchedule1 = taskScheduleBuilder.scheduleName(randomName("schedule1")).task(task1).build();
			 TaskSchedule taskSchedule2 = taskScheduleBuilder.scheduleName(randomName("schedule2")).task(task2).build()) {

			taskSchedule1.schedule(DEFAULT_CRON_EXPRESSION, Collections.emptyMap());
			taskSchedule2.schedule(DEFAULT_CRON_EXPRESSION, Collections.emptyMap());

			assertThat(taskScheduleBuilder.list().size()).isEqualTo(2);

			HashSet<String> scheduleSet = new HashSet<>(Arrays.asList(taskSchedule1.getScheduleName(), taskSchedule2.getScheduleName()));

			for (TaskSchedule taskSchedule : taskScheduleBuilder.list()) {
				if (scheduleSet.contains(taskSchedule.getScheduleName())) {
					assertThat(taskSchedule.getScheduleProperties().get(SchedulerPropertyKeys.CRON_EXPRESSION)).isEqualTo(DEFAULT_CRON_EXPRESSION);
				}
				else {
					fail(String.format("%s schedule is missing from result set of list.", taskSchedule.getScheduleName()));
				}
			}
		}
	}

	@Test
	public void filterByTaskTest() {
		Assumptions.assumeTrue(enabledScheduler);

		logger.info("schedule-find-by-task-test");

		TaskScheduleBuilder taskScheduleBuilder = TaskSchedule.builder(dataFlowOperations);

		try (Task task1 = Task.builder(dataFlowOperations).name(randomName("task1")).definition("timestamp").build();
			 Task task2 = Task.builder(dataFlowOperations).name(randomName("task2")).definition("timestamp").build();

			 TaskSchedule taskSchedule1 = taskScheduleBuilder.scheduleName(randomName("schedule1")).task(task1).build();
			 TaskSchedule taskSchedule2 = taskScheduleBuilder.scheduleName(randomName("schedule2")).task(task2).build()) {


			assertThat(taskScheduleBuilder.list().size()).isEqualTo(0);
			assertThat(taskScheduleBuilder.list(task1).size()).isEqualTo(0);
			assertThat(taskScheduleBuilder.list(task2).size()).isEqualTo(0);

			taskSchedule1.schedule(DEFAULT_CRON_EXPRESSION, Collections.emptyMap());
			taskSchedule2.schedule(DEFAULT_CRON_EXPRESSION, Collections.emptyMap());

			assertThat(taskScheduleBuilder.list().size()).isEqualTo(2);
			assertThat(taskScheduleBuilder.list(task1).size()).isEqualTo(1);
			assertThat(taskScheduleBuilder.list(task2).size()).isEqualTo(1);

			assertThat(taskScheduleBuilder.list(task1).get(0).getScheduleName()).isEqualTo(taskSchedule1.getScheduleName());
			assertThat(taskScheduleBuilder.list(task1).get(0).getScheduleProperties().containsKey(SchedulerPropertyKeys.CRON_EXPRESSION)).isTrue();
			assertThat(taskScheduleBuilder.list(task1).get(0).getScheduleProperties().get(SchedulerPropertyKeys.CRON_EXPRESSION)).isEqualTo(DEFAULT_CRON_EXPRESSION);

			assertThat(taskScheduleBuilder.list(task2).get(0).getScheduleName()).isEqualTo(taskSchedule2.getScheduleName());
			assertThat(taskScheduleBuilder.list(task2).get(0).getScheduleProperties().containsKey(SchedulerPropertyKeys.CRON_EXPRESSION)).isTrue();
			assertThat(taskScheduleBuilder.list(task2).get(0).getScheduleProperties().get(SchedulerPropertyKeys.CRON_EXPRESSION)).isEqualTo(DEFAULT_CRON_EXPRESSION);
		}
	}

	@Test
	public void scheduleLifeCycle() {

		Assumptions.assumeTrue(enabledScheduler);

		logger.info("schedule-lifecycle-test");

		try (Task task = Task.builder(dataFlowOperations).name(randomName("task")).definition("timestamp").build();
			 TaskSchedule taskSchedule = TaskSchedule.builder(dataFlowOperations).scheduleName(randomName("schedule")).task(task).build()) {

			assertThat(taskSchedule.isScheduled()).isFalse();

			logger.info("schedule-lifecycle-test: SCHEDULE");
			taskSchedule.schedule(DEFAULT_CRON_EXPRESSION, Collections.emptyMap());

			assertThat(taskSchedule.isScheduled()).isTrue();

			TaskSchedule retrievedSchedule = TaskSchedule.builder(dataFlowOperations).findByScheduleName(taskSchedule.getScheduleName()).get();
			assertThat(retrievedSchedule.getScheduleName()).isEqualTo(taskSchedule.getScheduleName());
			assertThat(retrievedSchedule.getScheduleProperties().containsKey(SchedulerPropertyKeys.CRON_EXPRESSION)).isTrue();
			assertThat(retrievedSchedule.getScheduleProperties().get(SchedulerPropertyKeys.CRON_EXPRESSION)).isEqualTo(DEFAULT_CRON_EXPRESSION);

			logger.info("schedule-lifecycle-test: UNSCHEDULE");
			taskSchedule.unschedule();

			assertThat(taskSchedule.isScheduled()).isFalse();
		}
	}

	private static String randomName(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 10);
	}
}
