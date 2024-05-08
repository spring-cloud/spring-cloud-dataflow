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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.table.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Glenn Renfro
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public class JobCommandTests extends AbstractShellIntegrationTest {

	private final static String BASE_JOB_NAME = "myJob";

	private final static String JOB_NAME_ORIG = BASE_JOB_NAME + "_ORIG";

	private final static String JOB_NAME_FOO = BASE_JOB_NAME + "_FOO";

	private final static String JOB_NAME_FOOBAR = BASE_JOB_NAME + "_FOOBAR";

	private static final Logger logger = LoggerFactory.getLogger(JobCommandTests.class);

	private static TaskExecutionDaoContainer daoContainer;

	private static JobRepositoryContainer jobRepositoryContainer;

	private static TaskBatchDaoContainer taskBatchDaoContainer;

	private static AggregateExecutionSupport aggregateExecutionSupport;

	private static final List<JobInstance> jobInstances = new ArrayList<>();

	private static final List<Long> taskExecutionIds = new ArrayList<>(3);

	private static TaskDefinitionReader taskDefinitionReader;

	@BeforeAll
	public static void setUp() throws Exception {
		Thread.sleep(2000);
		taskDefinitionReader = applicationContext.getBean(TaskDefinitionReader.class);
		aggregateExecutionSupport = applicationContext.getBean(AggregateExecutionSupport.class);
		taskBatchDaoContainer = applicationContext.getBean(TaskBatchDaoContainer.class);
		jobRepositoryContainer = applicationContext.getBean(JobRepositoryContainer.class);
		taskBatchDaoContainer = applicationContext.getBean(TaskBatchDaoContainer.class);
		daoContainer = applicationContext.getBean(TaskExecutionDaoContainer.class);

		taskExecutionIds.add(createSampleJob(JOB_NAME_ORIG, 1));
		taskExecutionIds.add(createSampleJob(JOB_NAME_FOO, 1));
		taskExecutionIds.add(createSampleJob(JOB_NAME_FOOBAR, 2));
	}

	@AfterAll
	public static void tearDown() {
		if (applicationContext == null) {
			logger.warn("Application context was null (probably due to setup failure) - not performing tearDown");
			return;
		}
		JdbcTemplate template = new JdbcTemplate(applicationContext.getBean(DataSource.class));
		template.afterPropertiesSet();
		final String TASK_EXECUTION_FORMAT = "DELETE FROM TASK_EXECUTION WHERE TASK_EXECUTION_ID = %d";
		final String TASK_BATCH_FORMAT = "DELETE FROM TASK_TASK_BATCH WHERE TASK_EXECUTION_ID = %d";

		for (Long id : taskExecutionIds) {
			template.execute(String.format(TASK_BATCH_FORMAT, id));
			template.execute(String.format(TASK_EXECUTION_FORMAT, id));
		}
	}

	private static long createSampleJob(String jobName, int jobExecutionCount) {
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(jobName, taskDefinitionReader);
		JobRepository jobRepository = jobRepositoryContainer.get(schemaVersionTarget.getName());
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());
		jobInstances.add(instance);
		TaskExecutionDao dao = daoContainer.get(schemaVersionTarget.getName());
		TaskExecution taskExecution = dao.createTaskExecution(jobName, new Date(), new ArrayList<>(), null);
		Map<String, JobParameter> jobParameterMap = new HashMap<>();
		jobParameterMap.put("foo", new JobParameter("FOO", true));
		jobParameterMap.put("bar", new JobParameter("BAR", false));
		JobParameters jobParameters = new JobParameters(jobParameterMap);
		JobExecution jobExecution;
		TaskBatchDao taskBatchDao = taskBatchDaoContainer.get(schemaVersionTarget.getName());
		for (int i = 0; i < jobExecutionCount; i++) {
			jobExecution = jobRepository.createJobExecution(instance, jobParameters, null);
			taskBatchDao.saveRelationship(taskExecution, jobExecution);
			StepExecution stepExecution = new StepExecution("foobar", jobExecution);
			jobRepository.add(stepExecution);
		}
		return taskExecution.getExecutionId();
	}

	@Test
	public void testJobExecutionList() {
		logger.info("Retrieve Job Execution List Test");
		Table table = getTable(job().jobExecutionList());
		verifyColumnNumber(table, 7);
		checkCell(table, 0, 0, "ID ");
		checkCell(table, 0, 1, "Task ID");
		checkCell(table, 0, 2, "Job Name ");
		checkCell(table, 0, 3, "Start Time ");
		checkCell(table, 0, 4, "Step Execution Count ");
		checkCell(table, 0, 5, "Definition Status ");
		checkCell(table, 0, 6, "Schema Target");

 	 	}

	@Test
	public void testJobExecutionListByName() {
		logger.info("Retrieve Job Execution List By Name Test");
		Table table = getTable(job().jobExecutionListByName(JOB_NAME_FOOBAR));
		verifyColumnNumber(table, 7);
		checkCell(table, 0, 0, "ID ");
		checkCell(table, 0, 1, "Task ID");
		checkCell(table, 0, 2, "Job Name ");
		checkCell(table, 0, 3, "Start Time ");
		checkCell(table, 0, 4, "Step Execution Count ");
		checkCell(table, 0, 5, "Definition Status ");
		checkCell(table, 0, 6, "Schema Target");
	}

	@Test
	public void testViewExecution() {
		logger.info("Retrieve Job Execution Detail by Id");

		Table table = getTable(job().executionDisplay(getFirstJobExecutionIdFromTable()));
		verifyColumnNumber(table, 2);
		assertEquals(19,
				table.getModel().getRowCount(),
				"Number of expected rows returned from the table is incorrect");
		int rowNumber = 0;
		checkCell(table, rowNumber++, 0, "Key ");
		checkCell(table, rowNumber++, 0, "Job Execution Id ");
		checkCell(table, rowNumber++, 0, "Task Execution Id ");
		checkCell(table, rowNumber++, 0, "Task Instance Id ");
		checkCell(table, rowNumber++, 0, "Job Name ");
		checkCell(table, rowNumber++, 0, "Create Time ");
		checkCell(table, rowNumber++, 0, "Start Time ");
		checkCell(table, rowNumber++, 0, "End Time ");
		checkCell(table, rowNumber++, 0, "Running ");
		checkCell(table, rowNumber++, 0, "Stopping ");
		checkCell(table, rowNumber++, 0, "Step Execution Count ");
		checkCell(table, rowNumber++, 0, "Execution Status ");
		checkCell(table, rowNumber++, 0, "Exit Status ");
		checkCell(table, rowNumber++, 0, "Exit Message ");
		checkCell(table, rowNumber++, 0, "Definition Status ");
		checkCell(table, rowNumber++, 0, "Schema Target ");
		checkCell(table, rowNumber++, 0, "Job Parameters ");
		int paramRowOne = rowNumber++;
		int paramRowTwo = rowNumber++;
		boolean jobParamsPresent = (table.getModel().getValue(paramRowOne, 0).equals("foo(STRING) ")
			&& table.getModel().getValue(paramRowTwo, 0).equals("-bar(STRING) "))
			|| (table.getModel().getValue(paramRowOne, 0).equals("-bar(STRING) ")
			&& table.getModel().getValue(paramRowTwo, 0).equals("foo(STRING) "));
		assertTrue(jobParamsPresent, "the table did not contain the correct job parameters ");
	}

	@Test
	public void testViewInstance() {
		logger.info("Retrieve Job Instance Detail by Id");

		Table table = getTable(job().instanceDisplay(jobInstances.get(0).getInstanceId()));
		verifyColumnNumber(table, 6);
		checkCell(table, 0, 0, "Name ");
		checkCell(table, 0, 1, "Execution ID ");
		checkCell(table, 0, 2, "Step Execution Count ");
		checkCell(table, 0, 3, "Status ");
		checkCell(table, 0, 4, "Schema Target ");
		checkCell(table, 0, 5, "Job Parameters ");
		boolean isValidCell = table.getModel().getValue(1, 5).equals("foo=FOO,-bar=BAR")
			|| table.getModel().getValue(1, 5).equals("-bar=BAR,foo=FOO");
		assertTrue(isValidCell, "Job Parameters does match expected.");
	}

	@Test
	public void testJobStepExecutionList() {
		logger.info("Retrieve Job Step Execution List Test");

		Table table = getTable(job().jobStepExecutionList(getFirstJobExecutionIdFromTable()));
		verifyColumnNumber(table, 6);
		checkCell(table, 0, 0, "ID ");
		checkCell(table, 0, 1, "Step Name ");
		checkCell(table, 0, 2, "Job Exec Id ");
		checkCell(table, 0, 3, "Start Time ");
		checkCell(table, 0, 4, "End Time ");
		checkCell(table, 0, 5, "Status ");
	}

	@Test
	public void testJobStepExecutionProgress() {
		logger.info("Retrieve Job Step Execution Progress Test");

		long jobExecutionId = getFirstJobExecutionIdFromTable();
		long stepExecutionId = getFirstStepExecutionIdFromJobExecution(jobExecutionId);

		Table table = getTable(job().jobStepExecutionProgress(stepExecutionId, jobExecutionId));
		verifyColumnNumber(table, 4);
		checkCell(table, 0, 0, "ID ");
		checkCell(table, 0, 1, "Step Name ");
		checkCell(table, 0, 2, "Complete ");
		checkCell(table, 0, 3, "Duration ");

	}

	@Test
	public void testStepExecutionView() {
		logger.info("Retrieve Job Execution Detail by Id");

		long jobExecutionId = getFirstJobExecutionIdFromTable();
		long stepExecutionId = getFirstStepExecutionIdFromJobExecution(jobExecutionId);

		Table table = getTable(job().jobStepExecutionDisplay(stepExecutionId, jobExecutionId));

		verifyColumnNumber(table, 2);
		checkCell(table, 0, 0, "Key ");
		checkCell(table, 1, 0, "Step Execution Id ");
		checkCell(table, 2, 0, "Job Execution Id ");
		checkCell(table, 3, 0, "Step Name ");
		checkCell(table, 4, 0, "Start Time ");
		checkCell(table, 5, 0, "End Time ");
		checkCell(table, 6, 0, "Duration ");
		checkCell(table, 7, 0, "Status ");
		checkCell(table, 8, 0, "Last Updated ");
		checkCell(table, 9, 0, "Read Count ");
		checkCell(table, 10, 0, "Write Count ");
		checkCell(table, 11, 0, "Filter Count ");
		checkCell(table, 12, 0, "Read Skip Count ");
		checkCell(table, 13, 0, "Write Skip Count ");
		checkCell(table, 14, 0, "Process Skip Count ");
		checkCell(table, 15, 0, "Read Skip Count ");
		checkCell(table, 16, 0, "Commit Count ");
		checkCell(table, 17, 0, "Rollback Count ");
		checkCell(table, 18, 0, "Exit Status ");
		checkCell(table, 19, 0, "Exit Description ");

	}

	private void checkCell(Table table, int row, int column, Object expectedValue) {
		assertEquals(expectedValue,
				table.getModel().getValue(row, column),
				String.format("Cell %d,%d's value should be %s", row, column, expectedValue));
	}

	private Table getTable(Object result) {
		assertThat(result).isInstanceOf(Table.class);
		return (Table) result;
	}

	private void verifyColumnNumber(Table table, int columnCount) {
		assertEquals(columnCount, table.getModel().getColumnCount(), "Number of columns returned was not expected");
	}

	private long getFirstJobExecutionIdFromTable() {
		Table result = getTable(job().jobExecutionList());
		return (long) result.getModel().getValue(1, 0);
	}

	private long getFirstStepExecutionIdFromJobExecution(long jobExecutionId) {
		Table result = getTable(job().jobStepExecutionList(jobExecutionId));
		return (long) result.getModel().getValue(1, 0);
	}
}
