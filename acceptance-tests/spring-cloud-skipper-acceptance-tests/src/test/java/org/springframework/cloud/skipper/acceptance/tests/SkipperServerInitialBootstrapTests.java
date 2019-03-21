/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.acceptance.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.skipper.acceptance.core.DockerCompose;
import org.springframework.cloud.skipper.acceptance.core.DockerComposeExtension;
import org.springframework.cloud.skipper.acceptance.core.DockerComposeInfo;
import org.springframework.cloud.skipper.acceptance.tests.support.Bootstrap;
import org.springframework.cloud.skipper.acceptance.tests.support.Db2;
import org.springframework.cloud.skipper.acceptance.tests.support.MsSql;
import org.springframework.cloud.skipper.acceptance.tests.support.Mysql;
import org.springframework.cloud.skipper.acceptance.tests.support.Oracle;
import org.springframework.cloud.skipper.acceptance.tests.support.Postgres;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper100;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper101;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper102;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper103;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper104;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper105;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper110;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper11x;
import org.springframework.cloud.skipper.acceptance.tests.support.Skipper20x;

/**
 * Tests going through start of skipper servers with databases and verifying
 * server works with initial schema creation.
 *
 * @author Janne Valkealahti
 *
 */
@ExtendWith(DockerComposeExtension.class)
@Bootstrap
public class SkipperServerInitialBootstrapTests extends AbstractSkipperServerTests {

	// postgres

	@Test
	@Postgres
	@Skipper100
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper100postgres.yml" }, services = { "skipper" })
	public void testSkipper100WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Postgres
	@Skipper101
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper101postgres.yml" }, services = { "skipper" })
	public void testSkipper101WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Postgres
	@Skipper102
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper102postgres.yml" }, services = { "skipper" })
	public void testSkipper102WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Postgres
	@Skipper103
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper103postgres.yml" }, services = { "skipper" })
	public void testSkipper103WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Postgres
	@Skipper104
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper104postgres.yml" }, services = { "skipper" })
	public void testSkipper104WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Postgres
	@Skipper105
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper105postgres.yml" }, services = { "skipper" })
	public void testSkipper105WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Postgres
	@Skipper110
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper110postgres.yml" }, services = { "skipper" })
	public void testSkipper110WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Postgres
	@Skipper11x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper11xpostgres.yml" }, services = { "skipper" })
	public void testSkipper11xWithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Postgres
	@Skipper20x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper20xpostgres.yml" }, services = { "skipper" })
	public void testSkipper20xWithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	// mysql

	@Test
	@Mysql
	@Skipper100
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper100mysql.yml" }, services = { "skipper" })
	public void testSkipper100WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Mysql
	@Skipper101
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper101mysql.yml" }, services = { "skipper" })
	public void testSkipper101WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Mysql
	@Skipper102
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper102mysql.yml" }, services = { "skipper" })
	public void testSkipper102WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Mysql
	@Skipper103
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper103mysql.yml" }, services = { "skipper" })
	public void testSkipper103WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Mysql
	@Skipper104
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper104mysql.yml" }, services = { "skipper" })
	public void testSkipper104WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Mysql
	@Skipper105
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper105mysql.yml" }, services = { "skipper" })
	public void testSkipper105WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Mysql
	@Skipper110
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper110mysql.yml" }, services = { "skipper" })
	public void testSkipper110WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Mysql
	@Skipper11x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper11xmysql.yml" }, services = { "skipper" })
	public void testSkipper11xWithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Mysql
	@Skipper20x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper20xmysql.yml" }, services = { "skipper" })
	public void testSkipper20xWithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	// oracle

	@Test
	@Oracle
	@Skipper100
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper100oracle.yml" }, services = { "skipper" })
	public void testSkipper100WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Oracle
	@Skipper101
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper101oracle.yml" }, services = { "skipper" })
	public void testSkipper101WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Oracle
	@Skipper102
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper102oracle.yml" }, services = { "skipper" })
	public void testSkipper102WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Oracle
	@Skipper103
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper103oracle.yml" }, services = { "skipper" })
	public void testSkipper103WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Oracle
	@Skipper104
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper104oracle.yml" }, services = { "skipper" })
	public void testSkipper104WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Oracle
	@Skipper105
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper105oracle.yml" }, services = { "skipper" })
	public void testSkipper105WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Oracle
	@Skipper110
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper110oracle.yml" }, services = { "skipper" })
	public void testSkipper110WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Oracle
	@Skipper11x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper11xoracle.yml" }, services = { "skipper" })
	public void testSkipper11xWithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Oracle
	@Skipper20x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper20xoracle.yml" }, services = { "skipper" })
	public void testSkipper20xWithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	// mssql

	@Test
	@MsSql
	@Skipper11x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mssql.yml" }, services = { "mssql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper11xmssql.yml" }, services = { "skipper" })
	public void testSkipper11xWithMsSql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@MsSql
	@Skipper20x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mssql.yml" }, services = { "mssql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper20xmssql.yml" }, services = { "skipper" })
	public void testSkipper20xWithMsSql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	// db2

	@Test
	@Db2
	@Skipper11x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/db2.yml" }, services = { "db2" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper11xdb2.yml" }, services = { "skipper" })
	public void testSkipper11xWithDb2(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}

	@Test
	@Db2
	@Skipper20x
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/db2.yml" }, services = { "db2" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper20xdb2.yml" }, services = { "skipper" })
	public void testSkipper20xWithDb2(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper", "skipper");
	}
}
