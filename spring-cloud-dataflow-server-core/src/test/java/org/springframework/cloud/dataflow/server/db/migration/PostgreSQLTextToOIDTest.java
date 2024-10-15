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
package org.springframework.cloud.dataflow.server.db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sql.DataSource;

import com.github.dockerjava.zerodep.shaded.org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureJdbc;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests conversion of text column to oid in PostgreSQL.
 *
 * @author Corneil du Plessis
 */
@AutoConfigureJdbc
@ExtendWith(SpringExtension.class)
class PostgreSQLTextToOIDTest {
	@SuppressWarnings("rawtypes")
	static PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse("postgres:14"));

	@BeforeAll
	static void startContainer() {
		container.start();
	}

	@Autowired
	DataSource dataSource;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", container::getJdbcUrl);
		registry.add("spring.datasource.username", container::getUsername);
		registry.add("spring.datasource.password", container::getPassword);
		registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
	}

	@Test
	void convertText() {
		final List<Pair<String, String>> data = new ArrayList<>();
		final Random random = new Random(System.currentTimeMillis());
		for (int i = 0; i < 5000; i++) {
			data.add(new ImmutablePair<>(randomString(random, 1024, 8), randomString(random, 128, 4)));
		}
		createTable("simple_table", "text", data);
		PostgreSQLTextToOID.convertColumnToOID("simple_table", "id", "big_string", dataSource);
		final String selectTable = "select id, convert_from(lo_get(cast(big_string as bigint)), 'UTF8'), short_string from simple_table";
		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<Triple<Long, String, String>> result = template.query(selectTable, (rs, rowNum) -> new ImmutableTriple<>(
						rs.getLong(1),
						rs.getString(2),
						rs.getString(3)
				)
		);
		for (Triple<Long, String, String> item : result) {
			Pair<String, String> right = data.get(item.getLeft().intValue() - 1);
			assertThat(right).isNotNull();
			assertThat(item.getMiddle()).isEqualTo(right.getLeft());
			assertThat(item.getRight()).isEqualTo(right.getRight());
		}
		PostgreSQLTextToOID.convertColumnFromOID("simple_table", "id", "big_string", dataSource);
		final String selectTextTable = "select id, big_string, short_string from simple_table";
		template = new JdbcTemplate(dataSource);
		result = template.query(selectTextTable, (rs, rowNum) -> new ImmutableTriple<>(
						rs.getLong(1),
						rs.getString(2),
						rs.getString(3)
				)
		);
		for (Triple<Long, String, String> item : result) {
			Pair<String, String> right = data.get(item.getLeft().intValue() - 1);
			assertThat(right).isNotNull();
			assertThat(item.getMiddle()).isEqualTo(right.getLeft());
			assertThat(item.getRight()).isEqualTo(right.getRight());
		}
	}

	@Test
	void convertVarChar() {
		final List<Pair<String, String>> data = new ArrayList<>();
		final Random random = new Random(System.currentTimeMillis());
		for (int i = 0; i < 5000; i++) {
			data.add(new ImmutablePair<>(randomString(random, 1024, 8), randomString(random, 128, 4)));
		}
		createTable("simple_table2", "varchar(2048)", data);
		PostgreSQLTextToOID.convertColumnToOID("simple_table2", "id", "big_string", dataSource);
		final String selectTable = "select id, convert_from(lo_get(cast(big_string as bigint)), 'UTF8'), short_string from simple_table2";
		JdbcTemplate template = new JdbcTemplate(dataSource);
		List<Triple<Long, String, String>> result = template.query(selectTable, (rs, rowNum) -> new ImmutableTriple<>(
						rs.getLong(1),
						rs.getString(2),
						rs.getString(3)
				)
		);
		for (Triple<Long, String, String> item : result) {
			Pair<String, String> right = data.get(item.getLeft().intValue() - 1);
			assertThat(right).isNotNull();
			assertThat(item.getMiddle()).isEqualTo(right.getLeft());
			assertThat(item.getRight()).isEqualTo(right.getRight());
		}
		PostgreSQLTextToOID.convertColumnFromOID("simple_table2", "id", "big_string", dataSource);
		final String selectTextTable = "select id, big_string, short_string from simple_table2";
		template = new JdbcTemplate(dataSource);
		result = template.query(selectTextTable, (rs, rowNum) -> new ImmutableTriple<>(
						rs.getLong(1),
						rs.getString(2),
						rs.getString(3)
				)
		);
		for (Triple<Long, String, String> item : result) {
			Pair<String, String> right = data.get(item.getLeft().intValue() - 1);
			assertThat(right).isNotNull();
			assertThat(item.getMiddle()).isEqualTo(right.getLeft());
			assertThat(item.getRight()).isEqualTo(right.getRight());
		}
	}

	private void createTable(String tableName, String colType, final List<Pair<String, String>> data) {

		final String createTable = """
				create table %s (
				  id int8 not null,
				  big_string %s,
				  short_string varchar(255),\
				  primary key (id)
				)""";
		final String insertTable = "insert into %s(id, big_string, short_string) values(?,?,?)";
		JdbcTemplate template = new JdbcTemplate(dataSource);
		template.update(String.format(createTable, tableName, colType));

		template.batchUpdate(String.format(insertTable, tableName), new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, i + 1);
				Pair<String, String> item = data.get(i);
				ps.setString(2, item.getLeft());
				ps.setString(3, item.getRight());
			}

			@Override
			public int getBatchSize() {
				return data.size();
			}
		});
	}

	private String randomString(Random random, int len, int minLen) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < Math.max(minLen, len); i++) {
			char c = (char) (random.nextInt(26) + 'a');
			result.append(random.nextBoolean() ? Character.toUpperCase(c) : Character.toLowerCase(c));
		}
		return new String(result.toString().getBytes(StandardCharsets.UTF_8), Charsets.UTF_8);
	}
}
