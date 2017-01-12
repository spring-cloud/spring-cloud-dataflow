/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.dataflow.registry;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * RDBMS implementation of {@link UriRegistry}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Thomas Risberg
 */
public class RdbmsUriRegistry implements UriRegistry {

	private static final String TABLE_NAME = "URI_REGISTRY";

	private static final String SELECT_URI_SQL = String.format("select URI from %s where NAME = ?", TABLE_NAME);

	private static final String SELECT_ALL_SQL = String.format("select NAME, URI from %s", TABLE_NAME);

	private static final String UPDATE_SQL = String.format("update %s set URI=? WHERE NAME=?", TABLE_NAME);

	private static final String INSERT_SQL = String.format("insert into %s (NAME, URI) values (?, ?)", TABLE_NAME);

	private static final String DELETE_SQL = String.format("delete from %s where NAME=?", TABLE_NAME);

	private final JdbcTemplate jdbcTemplate;

	public RdbmsUriRegistry(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public URI find(String name) {
		String uriString = null;
		try {
			uriString = jdbcTemplate.queryForObject(SELECT_URI_SQL, String.class, name);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
		return uriString != null ? toUri(uriString) : null;
	}

	@Override
	public Map<String, URI> findAll() {
		Map<String, URI> uriMap = new HashMap<>();
		List<Map<String, Object>> result = jdbcTemplate.queryForList(SELECT_ALL_SQL);
		for (Map<String, Object> map : result) {
			uriMap.put(String.valueOf(map.get("NAME")), toUri(String.valueOf(map.get("URI"))));
		}
		return uriMap;
	}

	@Override
	public void register(String name, URI uri) {
		Assert.notNull(uri, "Error when registering " + name + ": URI is required");
		Assert.hasText(uri.getScheme(), "Error when registering " + name + " with URI " + uri +
				": URI scheme must be specified");
		Assert.hasText(uri.getSchemeSpecificPart(), "Error when registering " + name + " with URI " + uri +
				": URI scheme-specific part must be specified");
		String uriString = uri.toString();
		if (find(name) != null) {
			jdbcTemplate.update(UPDATE_SQL, new Object[] { uriString, name },
					new int[] { Types.VARCHAR, Types.VARCHAR });
		}
		else {
			jdbcTemplate.update(INSERT_SQL, new Object[] { name, uriString },
					new int[] { Types.VARCHAR, Types.VARCHAR });
		}
	}

	@Override
	public void unregister(String name) {
		Assert.hasText(name, "name must not be empty nor null");
		jdbcTemplate.update(DELETE_SQL, name);
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
}
