/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.common.flyway;

import java.sql.Connection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.util.ObjectUtils;

/**
 * Simple utility class to run commands with a connection and possibly suppress
 * errors.
 *
 * @author Janne Valkealahti
 *
 */
public class SqlCommandsRunner {

	private static final Logger logger = LoggerFactory.getLogger(SqlCommandsRunner.class);

	/**
	 * Execute list of {@code SqlCommand} by suppressing errors if those are given
	 * with a command.
	 *
	 * @param connection the connection
	 * @param commands the sql commands
	 */
	public void execute(Connection connection, List<SqlCommand> commands) {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
		SQLExceptionTranslator origExceptionTranslator = jdbcTemplate.getExceptionTranslator();

		for (SqlCommand command : commands) {
			if (command.canHandleInJdbcTemplate()) {
				command.handle(jdbcTemplate, connection);
			}
			else {
				if(!ObjectUtils.isEmpty(command.getSuppressedErrorCodes())) {
					jdbcTemplate.setExceptionTranslator(new SuppressSQLErrorCodesTranslator(command.getSuppressedErrorCodes()));
				}
				try {
					logger.debug("Executing command {}", command.getCommand());
					jdbcTemplate.execute(command.getCommand());
				} catch (SuppressDataAccessException e) {
					logger.debug("Suppressing error {}", e);
				}
				// restore original translator in case next command
				// doesn't define suppressing codes.
				jdbcTemplate.setExceptionTranslator(origExceptionTranslator);
			}
		}
	}
}
