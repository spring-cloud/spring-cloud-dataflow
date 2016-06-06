/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.registry;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.cloud.dataflow.server.repository.AbstractRdbmsKeyValueRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * RDBMS implementation of {@link UriRegistry}.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RdbmsUriRegistry extends AbstractRdbmsKeyValueRepository<String> implements UriRegistry {

	public RdbmsUriRegistry(DataSource dataSource) {
		super(dataSource, "URI_APP_", "REGISTRY", new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet resultSet, int i) throws SQLException {
				return resultSet.getString("NAME") + "=" + resultSet.getString("URI");
			}
		}, "NAME", "URI");
	}

	@Override
	public URI find(String name) {
		String entry = findOne(name);
		if (entry == null) {
			return null;
		}
		String[] splitEntries = entry.split("=");
		return toUri(splitEntries[1]);
	}

	@Override
	public Map<String, URI> findAll() {
		Iterable<String> entries = findAllEntries();
		Map<String, URI> map = new HashMap<>();
		for (String entry : entries) {
			String[] splitEntries = entry.split("=");
			map.put(splitEntries[0], toUri(splitEntries[1]));
		}
		return map;
	}

	/**
	 * Convert the provided string to a {@link URI}.
	 *
	 * @param s string to convert to URI
	 * @return URI for string
	 * @throws IllegalStateException if URI creation throws {@link URISyntaxException}
	 */
	private URI toUri(String s) {
		try {
			return new URI(s);
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void register(String name, URI uri) {
		save(name, uri.toString());
	}

	@Override
	public void unregister(String name) {
		delete(name);
	}

	public void save(String name, String uriString) {
		if (find(name) != null) {
			Object[] updateParameters = new Object[] { uriString, name };
			jdbcTemplate.update(updateValue, updateParameters, new int[] { Types.VARCHAR, Types.VARCHAR });
		}
		else {
			Object[] insertParameters = new Object[] { name, uriString };
			jdbcTemplate.update(saveRow, insertParameters, new int[]{Types.VARCHAR, Types.VARCHAR});
		}
	}

	public String findOne(String name) {
		Assert.hasText(name, "name must not be empty nor null");
		try {
			return jdbcTemplate.queryForObject(findAllWhereClauseByKey, rowMapper, name);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public void delete(String name) {
		Assert.hasText(name, "name must not be empty nor null");
		jdbcTemplate.update(deleteFromTableByKey, name);
	}
}
