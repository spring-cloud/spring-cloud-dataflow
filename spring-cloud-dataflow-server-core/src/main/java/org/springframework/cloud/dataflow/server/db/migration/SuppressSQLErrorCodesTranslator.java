/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.server.db.migration;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

/**
 * {@link SQLErrorCodeSQLExceptionTranslator} suppressing errors based on
 * configured list of codes by throwing dedicated {@link SuppressDataAccessException}.
 *
 * @author Janne Valkealahti
 *
 */
public class SuppressSQLErrorCodesTranslator extends SQLErrorCodeSQLExceptionTranslator {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SuppressSQLErrorCodesTranslator.class);

	private final List<Integer> errorCodes;

	public SuppressSQLErrorCodesTranslator(int errorCode) {
		this(Arrays.asList(errorCode));
	}

	public SuppressSQLErrorCodesTranslator(List<Integer> errorCodes) {
		super();
		this.errorCodes = errorCodes;
	}

	@Override
	protected DataAccessException customTranslate(String task, String sql, SQLException sqlEx) {
		logger.debug("Checking sql error code {} against {}", sqlEx.getErrorCode(), errorCodes);
		if (errorCodes.contains(sqlEx.getErrorCode())) {
			return new SuppressDataAccessException(task, sqlEx);
		}
		return super.customTranslate(task, sql, sqlEx);
	}
}
