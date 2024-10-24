package org.springframework.cloud.dataflow.integration.test.tasks;

import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.data.CSVLoader;
import org.springframework.cloud.dataflow.integration.test.tags.Performance;
import org.springframework.cloud.dataflow.server.single.DataFlowServerApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DataFlowServerApplication.class,
		properties = { "spring.jpa.hibernate.ddl-auto=none", "spring.datasource.hikari.maximum-pool-size=5" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Performance
abstract class AbstractLargeTaskExecutionDatabaseBase {
	static Logger logger = LoggerFactory.getLogger(AbstractLargeTaskExecutionDatabaseBase.class);
	final MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

	MockMvc mockMvc;

	@Autowired
	WebApplicationContext wac;

	@Autowired
	DataSource dataSource;

	@BeforeEach
	void insertExecutions() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
			.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON).contentType(contentType))
			.build();
		long startTime = System.currentTimeMillis();
		String[] tableNames = new String[] { "BATCH_JOB_INSTANCE", "BATCH_JOB_EXECUTION", "BATCH_JOB_EXECUTION_CONTEXT",
				"BATCH_JOB_EXECUTION_PARAMS", "BATCH_STEP_EXECUTION", "BATCH_STEP_EXECUTION_CONTEXT", "TASK_EXECUTION",
				"TASK_EXECUTION_METADATA", "TASK_EXECUTION_PARAMS", "TASK_TASK_BATCH" };
		Map<String, Map<String, Integer>> columnTypes = new HashMap<>();
		addColumnTableType(columnTypes, "BATCH_JOB_INSTANCE", "VERSION", Types.INTEGER);
		addColumnTableType(columnTypes, "BATCH_JOB_EXECUTION", "VERSION", Types.INTEGER);
		addColumnTableType(columnTypes, "BATCH_STEP_EXECUTION", "VERSION", Types.INTEGER);
		addColumnTableType(columnTypes, "BATCH_JOB_EXECUTION_PARAMS", "DATE_VAL", Types.TIMESTAMP);
		addColumnTableType(columnTypes, "BATCH_JOB_EXECUTION_PARAMS", "LONG_VAL", Types.BIGINT);
		addColumnTableType(columnTypes, "BATCH_JOB_EXECUTION_PARAMS", "DOUBLE_VAL", Types.DOUBLE);
		addColumnTableType(columnTypes, "TASK_EXECUTION", "EXTERNAL_EXECUTION_ID", Types.VARCHAR);
		addColumnTableType(columnTypes, "TASK_EXECUTION", "EXIT_CODE", Types.INTEGER);

		for (String tableName : tableNames) {
			long startLoad = System.currentTimeMillis();
			logger.info("loading:{}", tableName);
			Map<String, Integer> tableColumnTypes = columnTypes.get(tableName);
			CSVLoader.DeriveType deriveType = columnName -> {
				String col = columnName.toUpperCase();
				Integer type = tableColumnTypes != null ? tableColumnTypes.get(col) : null;
				if (type != null) {
					return type;
				}
				if (col.equals("ID") || col.endsWith("_ID")) {
					return Types.BIGINT;
				}
				if (col.endsWith("_COUNT")) {
					return Types.INTEGER;
				}
				if (col.endsWith("_TIME") || col.endsWith("_UPDATED")) {
					return Types.TIMESTAMP;
				}
				return Types.VARCHAR;
			};
			ClassPathResource resource = new ClassPathResource("task-executions/" + tableName + ".csv");
			assertThat(resource.exists())
				.withFailMessage(() -> "classpath:task-executions/" + tableName + ".csv:NOT FOUND")
				.isTrue();
			int loaded = CSVLoader.loadCSV(tableName, dataSource, resource, deriveType);
			long loadTime = System.currentTimeMillis() - startLoad;
			logger.info("loaded:{} into {} in {}ms", loaded, tableName, loadTime);
		}
		long totalTime = System.currentTimeMillis() - startTime;
		logger.info("Total load time={}ms", totalTime);
	}

	private void addColumnTableType(Map<String, Map<String, Integer>> columnTypes, String tableName, String columnName,
			int type) {
		Map<String, Integer> tableColumnTypes = columnTypes.computeIfAbsent(tableName, k -> new HashMap<>());
		tableColumnTypes.put(columnName, type);
	}

	@Test
	void queryWithLargeNumberOfTaskExecutions() throws Exception {
		mockMvc.perform(post("/apps/task/nope/1.0").param("uri", "maven:io.spring:timestamp-task:3.0.0"))
			.andExpect(status().is2xxSuccessful());
		mockMvc.perform(post("/tasks/definitions").param("name", "nope").param("definition", "nope"))
			.andExpect(status().is2xxSuccessful());
		mockMvc.perform(post("/tasks/definitions").param("name", "ts-batch").param("definition", "nope"))
			.andExpect(status().is2xxSuccessful());
		long startTime = System.currentTimeMillis();
		mockMvc
			.perform(get("/tasks/executions").param("size", "20").param("page", "1").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(greaterThanOrEqualTo(20))));
		long totalTime = System.currentTimeMillis() - startTime;
		long startTime2 = System.currentTimeMillis();
		mockMvc
			.perform(
					get("/tasks/executions").param("size", "200").param("page", "2").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(greaterThanOrEqualTo(200))));
		long totalTime2 = System.currentTimeMillis() - startTime2;
		long startTime3 = System.currentTimeMillis();
		mockMvc.perform(
				get("/tasks/thinexecutions").param("size", "20").param("page", "3").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.taskExecutionThinResourceList", hasSize(greaterThanOrEqualTo(20))));
		long totalTime3 = System.currentTimeMillis() - startTime3;
		long startTime4 = System.currentTimeMillis();
		mockMvc.perform(
				get("/tasks/thinexecutions").param("size", "200").param("page", "2").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.taskExecutionThinResourceList", hasSize(greaterThanOrEqualTo(200))));
		long totalTime4 = System.currentTimeMillis() - startTime4;
		long startTime5 = System.currentTimeMillis();
		mockMvc.perform(
				get("/tasks/executions").param("name", "nope").param("page", "0").param("size", "200").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements", is(0)));
		long totalTime5 = System.currentTimeMillis() - startTime5;
		long startTime6 = System.currentTimeMillis();
		mockMvc.perform(
				get("/tasks/thinexecutions").param("name", "nope").param("page", "0").param("size", "200").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements", is(0)));
		long totalTime6 = System.currentTimeMillis() - startTime6;
		long startTime7 = System.currentTimeMillis();
		mockMvc.perform(
				get("/tasks/executions").param("name", "ts-batch").param("page", "0").param("size", "200").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements", is(83)));
		long totalTime7 = System.currentTimeMillis() - startTime7;
		long startTime8 = System.currentTimeMillis();
		mockMvc.perform(
				get("/tasks/thinexecutions").param("name", "ts-batch").param("page", "0").param("size", "200").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page.totalElements", is(83)));
		long totalTime8 = System.currentTimeMillis() - startTime8;
		logger.info("result:totalTime={}ms", totalTime);
		logger.info("result:totalTime2={}ms", totalTime2);
		logger.info("result:totalTime3={}ms", totalTime3);
		logger.info("result:totalTime4={}ms", totalTime4);
		logger.info("result:totalTime5={}ms", totalTime5);
		logger.info("result:totalTime6={}ms", totalTime6);
		logger.info("result:totalTime7={}ms", totalTime7);
		logger.info("result:totalTime8={}ms", totalTime8);
		double ratioExecution = (double) totalTime / (double) totalTime2;
		double ratioThinExecution = (double) totalTime3 / (double) totalTime4;
		double ratioThinToExecution = (double) totalTime2 / (double) totalTime4;
		logger.info("Ratio for tasks/executions:{}", ratioExecution);
		logger.info("Ratio for tasks/thinexecutions:{}", ratioThinExecution);
		logger.info("Ratio for tasks/executions to thinexecutions:{}",
				ratioThinToExecution);
		assertThat(totalTime4).isLessThan(totalTime2);
		assertThat(ratioThinToExecution).isGreaterThan(2.0);
	}

}
