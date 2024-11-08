/*
 * Copyright 2006-2007 the original author or authors.
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

import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;

/**
 * @author Dave Syer
 * 
 */
public class JdbcSearchableJobInstanceDao extends JdbcJobInstanceDao implements SearchableJobInstanceDao {

	private static final String GET_COUNT_BY_JOB_NAME = "SELECT COUNT(1) from %PREFIX%JOB_INSTANCE "
			+ "where JOB_NAME LIKE ?";

	/**
	 * @see JdbcJobExecutionDao#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {

		setJobInstanceIncrementer(new AbstractDataFieldMaxValueIncrementer() {
			@Override
			protected long getNextKey() {
				return 0;
			}
		});

		super.afterPropertiesSet();

	}

	/**
	 * 
	 * @see SearchableJobInstanceDao#countJobInstances (String)
	 */
	public int countJobInstances(String name) {
		return getJdbcTemplate().queryForObject(getQuery(GET_COUNT_BY_JOB_NAME), Integer.class, name);
	}

}
