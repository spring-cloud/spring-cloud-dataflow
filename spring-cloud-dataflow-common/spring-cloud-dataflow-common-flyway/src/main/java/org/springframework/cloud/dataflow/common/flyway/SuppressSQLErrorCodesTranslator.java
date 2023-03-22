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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.util.Assert;

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

	/**
	 * Instantiates a new suppress SQL error codes translator.
	 *
	 * @param errorCode the error code
	 */
	public SuppressSQLErrorCodesTranslator(int errorCode) {
		this(Arrays.asList(errorCode));
	}

	/**
	 * Instantiates a new suppress SQL error codes translator.
	 *
	 * @param errorCodes the error codes
	 */
	public SuppressSQLErrorCodesTranslator(List<Integer> errorCodes) {
		super();
		Assert.notNull(errorCodes, "errorCodes must be set");
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
