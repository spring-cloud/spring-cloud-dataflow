/*
 * Copyright 2009-2023 the original author or authors.
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
package org.springframework.cloud.dataflow.server.batch;

import java.sql.Types;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.jsr.JsrJobParametersConverter;
import org.springframework.batch.core.jsr.launch.JsrJobOperator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.repository.AggregateJobQueryDao;
import org.springframework.cloud.dataflow.server.repository.JdbcAggregateJobQueryDao;
import org.springframework.cloud.dataflow.server.service.JobServiceContainer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A factory for a {@link JobService} that makes the configuration of its various
 * ingredients as convenient as possible.
 *
 * @author Dave Syer
 * @author Corneil du Plessis
 *
 */
public class SimpleJobServiceFactoryBean implements FactoryBean<JobService>, InitializingBean, EnvironmentAware {

	private static final Logger logger = LoggerFactory.getLogger(SimpleJobServiceFactoryBean.class);

	private DataSource dataSource;

	private JdbcOperations jdbcTemplate;

	private String databaseType;

	private String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;
	private String taskTablePrefix = "TASK_";

	private DataFieldMaxValueIncrementerFactory incrementerFactory;

	private int maxVarCharLength = AbstractJdbcBatchMetadataDao.DEFAULT_EXIT_MESSAGE_LENGTH;

	private LobHandler lobHandler;

	private JobRepository jobRepository;

	private JobLauncher jobLauncher;

	private JobExplorer jobExplorer;

	private ExecutionContextSerializer serializer;

	private PlatformTransactionManager transactionManager;

	private JobServiceContainer jobServiceContainer;

	private SchemaService schemaService;

	private SchemaVersionTarget schemaVersionTarget;

	private Environment environment;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Set the schemaVersionTarget to be used by the created SimpleJobService.
	 * @param schemaVersionTarget the schemaVersionTarget to be associated with this service.
	 */
	public void setAppBootSchemaVersionTarget(SchemaVersionTarget schemaVersionTarget) {
		this.schemaVersionTarget = schemaVersionTarget;
	}

	/**
	 * A special handler for large objects. The default is usually fine, except for some
	 * (usually older) versions of Oracle. The default is determined from the data base type.
	 *
	 * @param lobHandler the {@link LobHandler} to set
	 *
	 * @see LobHandler
	 */
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	/**
	 * Public setter for the length of long string columns in database. Do not set this if you
	 * haven't modified the schema. Note this value will be used for the exit message in both
	 * {@link JdbcJobExecutionDao} and {@link JdbcStepExecutionDao} and also the short version
	 * of the execution context in {@link JdbcExecutionContextDao} . For databases with
	 * multi-byte character sets this number can be smaller (by up to a factor of 2 for 2-byte
	 * characters) than the declaration of the column length in the DDL for the tables.
	 *
	 * @param maxVarCharLength the exitMessageLength to set
	 */
	public void setMaxVarCharLength(int maxVarCharLength) {
		this.maxVarCharLength = maxVarCharLength;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Public setter for the {@link DataSource}.
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Sets the database type.
	 * @param dbType as specified by {@link DefaultDataFieldMaxValueIncrementerFactory}
	 */
	public void setDatabaseType(String dbType) {
		this.databaseType = dbType;
	}

	/**
	 * Sets the table prefix for all the batch meta-data tables.
	 * @param tablePrefix Prefix for batch meta-data tables
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public void setTaskTablePrefix(String taskTablePrefix) {
		this.taskTablePrefix = taskTablePrefix;
	}

	/**
	 * Sets the {@link JobServiceContainer} for the service.
	 * @param jobServiceContainer the JobServiceContainer for this service.
	 */
	public void setJobServiceContainer(JobServiceContainer jobServiceContainer) {
		this.jobServiceContainer = jobServiceContainer;
	}

	/**
	 * Sets the {@link SchemaService} for this factory bean.
	 * @param schemaService the schemaService for this factory bean.
	 */
	public void setSchemaService(SchemaService schemaService) {
		this.schemaService = schemaService;
	}

	/**
	 * A factory for incrementers (used to build primary keys for meta data). Defaults to
	 * {@link DefaultDataFieldMaxValueIncrementerFactory}.
	 * @param incrementerFactory the incrementer factory to set
	 */
	public void setIncrementerFactory(DataFieldMaxValueIncrementerFactory incrementerFactory) {
		this.incrementerFactory = incrementerFactory;
	}

	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	/**
	 * The repository used to store and update jobs and step executions.
	 *
	 * @param jobRepository the {@link JobRepository} to set
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * The launcher used to run jobs.
	 * @param jobLauncher a {@link JobLauncher}
	 */
	public void setJobLauncher(JobLauncher jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	/**
	 * A custom implementation of the {@link ExecutionContextSerializer}.
	 *
	 * @param serializer the serializer to set
	 * @see ExecutionContextSerializer
	 */
	public void setSerializer(ExecutionContextSerializer serializer) {
		this.serializer = serializer;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(dataSource, "DataSource must not be null.");
		Assert.notNull(jobRepository, "JobRepository must not be null.");
		Assert.notNull(jobLauncher, "JobLauncher must not be null.");
		Assert.notNull(jobExplorer, "JobExplorer must not be null.");

		jdbcTemplate = new JdbcTemplate(dataSource);

		if (incrementerFactory == null) {
			incrementerFactory = new MultiSchemaIncrementerFactory(dataSource);
		}

		if (databaseType == null) {
			databaseType = DatabaseType.fromMetaData(dataSource).name();
			logger.info("No database type set, using meta data indicating: " + databaseType);
		}

		if (lobHandler == null) {
			lobHandler = new DefaultLobHandler();
		}

		if (serializer == null) {
			this.serializer = new Jackson2ExecutionContextStringSerializer();
		}

		Assert.isTrue(incrementerFactory.isSupportedIncrementerType(databaseType), "'" + databaseType
				+ "' is an unsupported database type.  The supported database types are "
				+ StringUtils.arrayToCommaDelimitedString(incrementerFactory.getSupportedIncrementerTypes()));

	}

	protected SearchableJobInstanceDao createJobInstanceDao() throws Exception {
		JdbcSearchableJobInstanceDao dao = new JdbcSearchableJobInstanceDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setJobIncrementer(incrementerFactory.getIncrementer(databaseType, tablePrefix + "JOB_SEQ"));
		dao.setTablePrefix(tablePrefix);
		dao.afterPropertiesSet();
		return dao;
	}

	protected SearchableJobExecutionDao createJobExecutionDao() throws Exception {
		BatchVersion batchVersion = BatchVersion.from(this.schemaVersionTarget);
		JdbcSearchableJobExecutionDao dao = new JdbcSearchableJobExecutionDao(batchVersion);
		dao.setDataSource(dataSource);
		dao.setJobExecutionIncrementer(incrementerFactory.getIncrementer(databaseType, tablePrefix
				+ "JOB_EXECUTION_SEQ"));
		dao.setTablePrefix(tablePrefix);
		dao.setTaskTablePrefix(taskTablePrefix);
		dao.setClobTypeToUse(determineClobTypeToUse(this.databaseType));
		dao.setExitMessageLength(maxVarCharLength);
		dao.afterPropertiesSet();
		return dao;
	}

	protected SearchableStepExecutionDao createStepExecutionDao() throws Exception {
		JdbcSearchableStepExecutionDao dao = new JdbcSearchableStepExecutionDao();
		dao.setDataSource(dataSource);
		dao.setStepExecutionIncrementer(incrementerFactory.getIncrementer(databaseType, tablePrefix
				+ "STEP_EXECUTION_SEQ"));
		dao.setTablePrefix(tablePrefix);
		dao.setClobTypeToUse(determineClobTypeToUse(this.databaseType));
		dao.setExitMessageLength(maxVarCharLength);
		dao.afterPropertiesSet();
		return dao;
	}

	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		JdbcExecutionContextDao dao = new JdbcExecutionContextDao();
		dao.setJdbcTemplate(jdbcTemplate);
		dao.setTablePrefix(tablePrefix);
		dao.setClobTypeToUse(determineClobTypeToUse(this.databaseType));
		if (lobHandler != null) {
			dao.setLobHandler(lobHandler);
		}
		dao.setSerializer(serializer);
		dao.afterPropertiesSet();
		// Assume the same length.
		dao.setShortContextLength(maxVarCharLength);
		return dao;
	}

	private int determineClobTypeToUse(String databaseType) {
		if (DatabaseType.SYBASE == DatabaseType.valueOf(databaseType.toUpperCase())) {
			return Types.LONGVARCHAR;
		}
		else {
			return Types.CLOB;
		}
	}

	protected AggregateJobQueryDao createAggregateJobQueryDao() throws Exception {
		return new JdbcAggregateJobQueryDao(this.dataSource, this.schemaService, this.jobServiceContainer, this.environment);
	}

	/**
	 * Create a {@link SimpleJobService} from the configuration provided.
	 *
	 * @see FactoryBean#getObject()
	 */
	@Override
	public JobService getObject() throws Exception {
		JsrJobParametersConverter jobParametersConverter = new JsrJobParametersConverter(dataSource);
		jobParametersConverter.afterPropertiesSet();
		JsrJobOperator jsrJobOperator = new JsrJobOperator(jobExplorer, jobRepository, jobParametersConverter,
				transactionManager);
		jsrJobOperator.afterPropertiesSet();
		return new SimpleJobService(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(),
			jobRepository, createExecutionContextDao(), jsrJobOperator, createAggregateJobQueryDao(), schemaVersionTarget);
	}

	/**
	 * Tells the containing bean factory what kind of object is the product of
	 * {@link #getObject()}.
	 *
	 * @return SimpleJobService
	 * @see FactoryBean#getObjectType()
	 */
	@Override
	public Class<? extends JobService> getObjectType() {
		return SimpleJobService.class;
	}

	/**
	 * Allows optimisation in the containing bean factory.
	 *
	 * @return true
	 * @see FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}

}
