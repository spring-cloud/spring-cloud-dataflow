/*
 * Copyright 2017-2018 the original author or authors.
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

import javax.sql.DataSource;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.server.controller.StreamAlreadyDeployedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * RDBMS implementation of {@link StreamDeploymentRepository}.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RdbmsStreamDeploymentRepository implements StreamDeploymentRepository {

	private static final String TABLE_NAME = "STREAM_DEPLOYMENTS";

	private static final String SELECT_ONE_SQL = String.format("select STREAM_NAME, DEPLOYMENT_PROPS from %s where STREAM_NAME = ?", TABLE_NAME);

	private static final String SELECT_ALL_SQL = String.format("select STREAM_NAME, DEPLOYMENT_PROPS from %s", TABLE_NAME);

	private static final String INSERT_SQL = String.format("insert into %s (STREAM_NAME, DEPLOYMENT_PROPS) values (?, ?)", TABLE_NAME);

	private static final String DELETE_SQL = String.format("delete from %s where STREAM_NAME=?", TABLE_NAME);

	private final JdbcTemplate jdbcTemplate;

	public RdbmsStreamDeploymentRepository(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public StreamDeployment save(StreamDeployment streamDeployment) {
		if (findOne(streamDeployment.getStreamName()) != null) {
			throw new StreamAlreadyDeployedException(streamDeployment.getStreamName());
		}
		int rows = jdbcTemplate.update(INSERT_SQL, new Object[] { streamDeployment.getStreamName(),
						streamDeployment.getDeploymentProperties() },
				new int[] { Types.VARCHAR, Types.VARCHAR });
		return (rows == 1) ? streamDeployment : null;
	}

	@Override
	public StreamDeployment findOne(String streamName) {
		Assert.hasText(streamName, "Stream name must not be empty or null");
		List<Map<String, Object>> result = jdbcTemplate.queryForList(SELECT_ONE_SQL, streamName);
		if (!result.isEmpty()) {
			Map<String, Object> map = result.get(0);
			return new StreamDeployment(get(map, "STREAM_NAME"), get(map, "DEPLOYMENT_PROPS"));
		}
		return null;
	}

	@Override
	public Iterable<StreamDeployment> findAll() {
		List<Map<String, Object>> result = jdbcTemplate.queryForList(SELECT_ALL_SQL);
		List<StreamDeployment> streamDeployments = new ArrayList<>(result.size());
		for (Map<String, Object> map : result) {
			streamDeployments.add(new StreamDeployment(get(map, "STREAM_NAME"), get(map, "DEPLOYMENT_PROPS")));
		}
		return streamDeployments;
	}

	private String get(Map<String, Object> map, String key) {
		return String.valueOf(map.get(key));
	}

	@Override
	public void delete(String streamName) {
		Assert.hasText(streamName, "Stream name must not be empty or null");
		jdbcTemplate.update(DELETE_SQL, streamName);
	}
}
