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

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * RDBMS implementation of {@link TaskDefinitionRepository}.
 *
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
public class RdbmsTaskDefinitionRepository extends AbstractRdbmsKeyValueRepository<TaskDefinition>
		implements TaskDefinitionRepository {

	public RdbmsTaskDefinitionRepository(DataSource dataSource) {
		super(dataSource, "TASK_", "DEFINITIONS", new RowMapper<TaskDefinition>() {
			@Override
			public TaskDefinition mapRow(ResultSet resultSet, int i) throws SQLException {
				return new TaskDefinition(resultSet.getString("DEFINITION_NAME"), resultSet.getString("DEFINITION"));
			}
		}, "DEFINITION_NAME", "DEFINITION");
	}

	@Override
	public <S extends TaskDefinition> S save(S definition) {
		Assert.notNull(definition, "definition must not be null");
		if (exists(definition.getName())) {
			throw new DuplicateTaskException(String.format(
					"Cannot register task %s because another one has already " + "been registered with the same name",
					definition.getName()));
		}
		Object[] insertParameters = new Object[] { definition.getName(), definition.getDslText() };
		jdbcTemplate.update(saveRow, insertParameters, new int[] { Types.VARCHAR, Types.LONGVARCHAR });
		return definition;
	}

	@Override
	public void delete(TaskDefinition definition) {
		Assert.notNull(definition, "definition must not null");
		delete(definition.getName());
	}
}
