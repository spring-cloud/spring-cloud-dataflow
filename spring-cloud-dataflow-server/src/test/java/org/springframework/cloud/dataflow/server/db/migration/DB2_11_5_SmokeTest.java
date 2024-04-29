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

import org.junit.jupiter.api.Disabled;
import org.springframework.cloud.dataflow.server.db.DB2_11_5_ContainerSupport;


/**
 * Basic database schema and JPA tests for DB2.
 *
 * @author Corneil du Plessis
 * @author Chris Bono
 */
//TODO: Boot3x - DB2 Driver has a bug.
//java.lang.NullPointerException: Cannot invoke "java.sql.Timestamp.toLocalDateTime()" because "<local4>" is null
//at com.ibm.db2.jcc.am.ResultSet.getObject(ResultSet.java:2020)
//at com.ibm.db2.jcc.am.ResultSet.getObject(ResultSet.java:2045)
//at com.zaxxer.hikari.pool.HikariProxyResultSet.getObject(HikariProxyResultSet.java)
//at org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao$TaskExecutionRowMapper.mapRow(JdbcTaskExecutionDao.java:621)
@Disabled("TODO: DB2 Driver and LocalDateTime has a bug when the row has is null in the column")
public class DB2_11_5_SmokeTest extends AbstractSmokeTest implements DB2_11_5_ContainerSupport {
}
