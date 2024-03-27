/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.cloud.dataflow.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

public class CSVLoader {
	public interface DeriveType {
		int deriveFromColumnName(String columnName);
	}

	public static int loadCSV(String tableName, DataSource dataSource, Resource cvsResource, DeriveType deriveType) throws IOException {
		CSVParser parser = CSVFormat.RFC4180.withFirstRecordAsHeader()
			.parse(new InputStreamReader(cvsResource.getInputStream()));
		List<String> headerNames = parser.getHeaderNames();
		final List<CSVRecord> records = parser.getRecords();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		String sql = "insert into " + tableName + " (" + StringUtils.collectionToCommaDelimitedString(headerNames) + ") values (";
		sql += StringUtils.collectionToCommaDelimitedString(headerNames.stream().map(s -> "?").collect(Collectors.toList())) + ")";
		jdbcTemplate.batchUpdate(sql, records, 100, (ps, record) -> {
			for (int i = 0; i < headerNames.size(); i++) {
				String name = headerNames.get(i);
				int type = deriveType.deriveFromColumnName(name);
				String str = record.get(name);
				try {
					if (str == null || str.trim().isEmpty()) {
						ps.setObject(i + 1, null);
					} else {
						switch (type) {
							case Types.VARCHAR:
								ps.setString(i + 1, str);
								break;
							case Types.DATE: {
								LocalDate dateTime = LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE);
								ps.setDate(i + 1, java.sql.Date.valueOf(dateTime));
								break;
							}
							case Types.TIMESTAMP: {
								LocalDateTime dateTime = LocalDateTime.parse(str.replace(' ', 'T'),
										DateTimeFormatter.ISO_LOCAL_DATE_TIME);
								ps.setTimestamp(i + 1, Timestamp.valueOf(dateTime));
								break;
							}
							case Types.BIGINT:
								ps.setLong(i + 1, Long.parseLong(str));
								break;
							case Types.INTEGER:
								ps.setInt(i + 1, Integer.parseInt(str));
								break;
							case Types.DOUBLE:
								ps.setDouble(i + 1, Double.parseDouble(str));
								break;
							default:
								throw new IllegalArgumentException("Unknown type for " + name + ":" + type);
						}
					}
				}
				catch (Throwable x) {
					String message = "Exception processing:" + tableName + ":" + name + ":" + type + ":" + str;
					LoggerFactory.getLogger(CSVLoader.class).error(message, x);
					throw new RuntimeException(message, x);
				}
			}
		});
		return records.size();
	}

}
