/*
 * Copyright 2016 the original author or authors.
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * RDBMS implementation of {@link DeploymentIdRepository}.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RdbmsDeploymentIdRepository extends AbstractRdbmsKeyValueRepository<String> implements DeploymentIdRepository {

	public RdbmsDeploymentIdRepository(DataSource dataSource) {
		super(dataSource, "DEPLOYMENT_", "IDS", new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet resultSet, int i) throws SQLException {
				return resultSet.getString("DEPLOYMENT_ID");
			}
		}, "DEPLOYMENT_KEY", "DEPLOYMENT_ID");
	}

	@Override
	public void save(String key, String deploymentId) {
		Object[] insertParameters = new Object[]{key, deploymentId};
		jdbcTemplate.update(saveRow, insertParameters, new int[]{Types.VARCHAR, Types.VARCHAR});
	}

	@Override
	public String save(String key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(String name) {
		Assert.hasText(name, "name must not be empty nor null");
		jdbcTemplate.update(deleteFromTableByKey, name);
	}
}
