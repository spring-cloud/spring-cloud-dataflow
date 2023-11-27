/*
 * Copyright 2023 the original author or authors.
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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AbstractDaoTests {

	DataSource dataSource;
	JdbcTemplate jdbcTemplate;

	DataSource createDataSourceForContainer(JdbcDatabaseContainer dbContainer) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbContainer.getDriverClassName());
		dataSource.setUrl(dbContainer.getJdbcUrl());
		dataSource.setUsername(dbContainer.getUsername());
		dataSource.setPassword(dbContainer.getPassword());
		return dataSource;
	}
	void createDataFlowSchema(JdbcDatabaseContainer dbContainer, String schemaName) throws IOException {
		JdbcDatabaseDelegate containerDelegate = new JdbcDatabaseDelegate(dbContainer, "");
		ScriptUtils.runInitScript(containerDelegate, "schemas/drop-table-schema-" + schemaName + ".sql");

		getResourceFiles("schemas/" + schemaName).forEach(str -> {
			if (str.contains("dataflow"))
				ScriptUtils.runInitScript(containerDelegate, "schemas/" + schemaName + "/" + str);
		});
	}

	void prepareForTest(JdbcDatabaseContainer dbContainer, String schemaName) throws IOException {
		this.dataSource = createDataSourceForContainer(dbContainer);
		this.jdbcTemplate = new JdbcTemplate(this.dataSource);
		createDataFlowSchema(dbContainer, schemaName);
	}

	private List<String> getResourceFiles(String path) throws IOException {
		List<String> fileNames = new ArrayList<>();

		try (
			InputStream stream = getResourceFileAsStream(path);
			BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
			String fileName;

			while ((fileName = br.readLine()) != null) {
				fileNames.add(fileName);
			}
		}
		fileNames.sort(new SchemaComparator());
		return fileNames;
	}

	private InputStream getResourceFileAsStream(String resourceFile) {
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceFile);
		return stream == null ? getClass().getResourceAsStream(resourceFile) : stream;
	}

	private static class SchemaComparator implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			int result = 0;
			if (getVersion(o1) > getVersion(o2)) {
				result = 1;
			} else if (getVersion(o1) < getVersion(o2)) {
				result = -1;
			}
			return result;
		}

		private int getVersion(String fileName) {
			return Integer.valueOf(fileName.substring(1, fileName.indexOf("-")));
		}
	}
}
