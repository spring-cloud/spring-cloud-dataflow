/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.repository;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.server.repository.support.AppDefinitionMixin;
import org.springframework.cloud.dataflow.server.repository.support.AppDeploymentRequestMixin;
import org.springframework.cloud.dataflow.server.repository.support.Order;
import org.springframework.cloud.dataflow.server.repository.support.PagingQueryProvider;
import org.springframework.cloud.dataflow.server.repository.support.SchemaUtilities;
import org.springframework.cloud.dataflow.server.repository.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.cloud.dataflow.server.service.impl.ResourceDeserializer;
import org.springframework.cloud.dataflow.server.service.impl.ResourceMixin;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * JDBC implementation for the {@code DataflowTaskExecutionMetadataDao}
 *
 * @author Michael Minella
 * @author Corneil du Plessis
 * @see DataflowTaskExecutionMetadataDao
 * @since 2.3
 */
public class JdbcDataflowTaskExecutionMetadataDao implements DataflowTaskExecutionMetadataDao {
	private final static Logger logger = LoggerFactory.getLogger(JdbcDataflowTaskExecutionMetadataDao.class);

	private static final String INSERT_SQL = "INSERT INTO %PREFIX%EXECUTION_METADATA (ID, TASK_EXECUTION_ID, " +
		"TASK_EXECUTION_MANIFEST) VALUES (:id, :taskExecutionId, :taskExecutionManifest)";

	private static final String FIND_MANIFEST_BY_TASK_EXECUTION_ID = "SELECT M.TASK_EXECUTION_MANIFEST AS TASK_EXECUTION_MANIFEST " +
		"FROM %PREFIX%EXECUTION_METADATA M INNER JOIN " +
		"%PREFIX%EXECUTION E ON M.TASK_EXECUTION_ID = E.TASK_EXECUTION_ID " +
		"WHERE E.TASK_EXECUTION_ID = :taskExecutionId";
	private static final String FIND_MANIFEST_BY_TASK_EXECUTION_IDS = "SELECT M.TASK_EXECUTION_MANIFEST AS TASK_EXECUTION_MANIFEST, M.TASK_EXECUTION_ID AS TASK_EXECUTION_ID " +
		"FROM %PREFIX%EXECUTION_METADATA M WHERE M.TASK_EXECUTION_ID in (:taskExecutionIds)";

	private static final String DELETE_MANIFEST_BY_TASK_EXECUTION_IDS = "DELETE FROM %PREFIX%EXECUTION_METADATA WHERE TASK_EXECUTION_ID IN (:taskExecutionIds)";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final DataFieldMaxValueIncrementer incrementer;

	private final ObjectMapper objectMapper;

	private final DataSource dataSource;

	private final String tablePrefix;

	public JdbcDataflowTaskExecutionMetadataDao(
		DataSource dataSource,
		DataFieldMaxValueIncrementer incrementer,
		String prefix
	) {
		this.tablePrefix = prefix;
		this.incrementer = incrementer;

		this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

		this.objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Resource.class,
			new ResourceDeserializer(new AppResourceCommon(new MavenProperties(), new DefaultResourceLoader())));
		this.objectMapper.registerModule(module);
		this.objectMapper.addMixIn(Resource.class, ResourceMixin.class);
		this.objectMapper.addMixIn(AppDefinition.class, AppDefinitionMixin.class);
		this.objectMapper.addMixIn(AppDeploymentRequest.class, AppDeploymentRequestMixin.class);
		this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		this.dataSource = dataSource;
	}

	@Override
	public void save(TaskExecution taskExecution, TaskManifest manifest) {
		try {
			final String manifestJson = this.objectMapper.writeValueAsString(manifest);

			final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("id", incrementer.nextLongValue())
				.addValue("taskExecutionId", taskExecution.getExecutionId())
				.addValue("taskExecutionManifest", manifestJson);

			String sql = SchemaUtilities.getQuery(INSERT_SQL, tablePrefix);
			logger.debug("save:sql={}, parameters={}", sql, queryParameters.getValues());
			this.jdbcTemplate.update(sql, queryParameters);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unable to serialize manifest", e);
		}
	}

	@Override
	public TaskManifest getLatestManifest(String taskName) {
		Map<String, Order> sortKeys = new HashMap<>(1);
		sortKeys.put("E.TASK_EXECUTION_ID", Order.DESCENDING);

		SqlPagingQueryProviderFactoryBean sqlPagingQueryProviderFactoryBean = new SqlPagingQueryProviderFactoryBean();

		sqlPagingQueryProviderFactoryBean.setDataSource(this.dataSource);
		sqlPagingQueryProviderFactoryBean.setSelectClause("TASK_EXECUTION_MANIFEST");
		sqlPagingQueryProviderFactoryBean.setFromClause(SchemaUtilities.getQuery(
			"%PREFIX%EXECUTION_METADATA M INNER JOIN %PREFIX%EXECUTION E ON M.TASK_EXECUTION_ID = E.TASK_EXECUTION_ID",
			tablePrefix));
		sqlPagingQueryProviderFactoryBean.setWhereClause("E.TASK_NAME = :taskName");
		sqlPagingQueryProviderFactoryBean.setSortKeys(sortKeys);

		try {
			PagingQueryProvider queryProvider = sqlPagingQueryProviderFactoryBean.getObject();

			queryProvider.init(this.dataSource);

			final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskName", taskName);

			String sql = queryProvider.getPageQuery(PageRequest.of(0, 1));
			logger.debug("getLatestManifest:sql={},parameters={}", sql, queryParameters.getValues());
			return this.jdbcTemplate.queryForObject(sql, queryParameters, (resultSet, i) -> {
				try {
					return objectMapper.readValue(resultSet.getString("TASK_EXECUTION_MANIFEST"), TaskManifest.class);
				} catch (IOException e) {
					throw new IllegalArgumentException("Unable to deserialize manifest", e);
				}
			});
		} catch (EmptyResultDataAccessException erdae) {
			return null;
		} catch (Exception e) {
			throw new IllegalStateException("Unable to generate query", e);
		}
	}

	@Override
	public TaskManifest findManifestById(Long id) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskExecutionId", id);

		try {
			String sql = SchemaUtilities.getQuery(FIND_MANIFEST_BY_TASK_EXECUTION_ID, tablePrefix);
			logger.debug("findManifestById:sql={}, parameters={}", sql, queryParameters);
			return this.jdbcTemplate.queryForObject(sql, queryParameters, (resultSet, i) -> {
				try {
					return objectMapper.readValue(resultSet.getString("TASK_EXECUTION_MANIFEST"), TaskManifest.class);
				} catch (IOException e) {
					throw new IllegalArgumentException("Unable to deserialize manifest", e);
				}
			});
		} catch (EmptyResultDataAccessException erdae) {
			return null;
		}
	}

	@Override
	public Map<Long, TaskManifest> findManifestByIds(Set<Long> ids) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskExecutionIds", ids);

		try {
			String sql = SchemaUtilities.getQuery(FIND_MANIFEST_BY_TASK_EXECUTION_IDS, tablePrefix);
			logger.debug("findManifestByIds:sql={}, parameters={}", sql, queryParameters);
			final Map<Long, TaskManifest> result = new HashMap<>();
			this.jdbcTemplate.query(sql, queryParameters, rs -> {
				try {
					String executionManifest = rs.getString("TASK_EXECUTION_MANIFEST");
					if(executionManifest != null && !executionManifest.trim().isEmpty()) {
						result.put(rs.getLong("TASK_EXECUTION_ID"),
							objectMapper.readValue(executionManifest, TaskManifest.class));
					}
				}
				catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			});
			return result;
		} catch (EmptyResultDataAccessException erdae) {
			return Collections.emptyMap();
		}
	}

	@Override
	public int deleteManifestsByTaskExecutionIds(Set<Long> taskExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
			.addValue("taskExecutionIds", taskExecutionIds);
		String sql = SchemaUtilities.getQuery(DELETE_MANIFEST_BY_TASK_EXECUTION_IDS, tablePrefix);
		logger.debug("deleteManifestsByTaskExecutionIds:sql={}, parameters={}", sql, queryParameters);
		return this.jdbcTemplate.update(sql, queryParameters);
	}
}
