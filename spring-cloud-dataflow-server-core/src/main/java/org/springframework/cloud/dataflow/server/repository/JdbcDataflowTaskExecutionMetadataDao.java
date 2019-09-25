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
import java.util.Set;

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.server.repository.support.AppDefinitionMixin;
import org.springframework.cloud.dataflow.server.repository.support.AppDeploymentRequestMixin;
import org.springframework.cloud.dataflow.server.service.impl.ResourceDeserializer;
import org.springframework.cloud.dataflow.server.service.impl.ResourceMixin;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * JDBC implementation for the {@code DataflowTaskExecutionMetadataDao}
 *
 * @author Michael Minella
 * @since 2.3
 * @see DataflowTaskExecutionMetadataDao
 */
public class JdbcDataflowTaskExecutionMetadataDao implements DataflowTaskExecutionMetadataDao {

	private static final String INSERT_SQL = "INSERT INTO task_execution_metadata (id, task_execution_id, " +
			"task_execution_manifest) VALUES (:id, :taskExecutionId, :taskExecutionManifest)";

	private static final String FIND_LATEST_MANIFEST_BY_NAME = "select m.task_execution_manifest as task_execution_manifest\n" +
			"from task_execution_metadata m inner join\n" +
			"        TASK_EXECUTION e on m.task_execution_id = e.TASK_EXECUTION_ID\n" +
			"where e.TASK_NAME = :taskName\n" +
			"order by e.TASK_EXECUTION_ID desc\n" +
			"limit 0,1;";

	private static final String DELETE_MANIFEST_BY_TASK_EXECUTION_IDS =
			"DELETE FROM task_execution_metadata " +
			"WHERE task_execution_id " +
			"IN (:taskExecutionIds)";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	private final DataFieldMaxValueIncrementer incrementer;

	private final ObjectMapper objectMapper;

	public JdbcDataflowTaskExecutionMetadataDao(DataSource dataSource,
			DataFieldMaxValueIncrementer incrementer,
			ApplicationContext context) {

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
	}

	@Override
	public void save(TaskExecution taskExecution, TaskManifest manifest) {
		try {
			final String manifestJson = this.objectMapper.writeValueAsString(manifest);

			final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
					.addValue("id", incrementer.nextLongValue())
					.addValue("taskExecutionId", taskExecution.getExecutionId())
					.addValue("taskExecutionManifest", manifestJson);

			this.jdbcTemplate.update(INSERT_SQL, queryParameters);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unable to serialize manifest", e);
		}
	}

	@Override
	public TaskManifest getLatestManifest(String taskName) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskName", taskName);

		try {
			return this.jdbcTemplate.queryForObject(FIND_LATEST_MANIFEST_BY_NAME,
					queryParameters,
					(resultSet, i) -> {
						try {
							return objectMapper.readValue(resultSet.getString("task_execution_manifest"), TaskManifest.class);
						}
						catch (IOException e) {
							throw new IllegalArgumentException("Unable to deserialize manifest", e);
						}
					});
		}
		catch (EmptyResultDataAccessException erdae) {
			return null;
		}
	}

	@Override
	public int deleteManifestsByTaskExecutionIds(Set<Long> taskExecutionIds) {
		final MapSqlParameterSource queryParameters = new MapSqlParameterSource()
				.addValue("taskExecutionIds", taskExecutionIds);
		return this.jdbcTemplate.update(DELETE_MANIFEST_BY_TASK_EXECUTION_IDS, queryParameters);
	}
}
