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
import org.springframework.cloud.skipper.acceptance.tests.support.Migration;
import org.springframework.cloud.skipper.acceptance.tests.support.Mysql;
import org.springframework.cloud.skipper.acceptance.tests.support.Oracle;
import org.springframework.cloud.skipper.acceptance.tests.support.Postgres;
import org.springframework.cloud.skipper.acceptance.tests.support.SkipperAll;

/**
 * Tests going through start of skipper servers with databases and verifying
 * that newer versions work when older versions have created initial db schemas.
 *
 * @author Janne Valkealahti
 *
 */
@ExtendWith(DockerComposeExtension.class)
@Migration
public class SkipperServerMigrationTests extends AbstractSkipperServerTests {

	@Test
	@Postgres
	@SkipperAll
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper100", order = 1, locations = { "src/test/resources/skipper100postgres.yml" }, services = { "skipper" })
	@DockerCompose(id = "skipper101", order = 1, locations = { "src/test/resources/skipper101postgres.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper102", order = 1, locations = { "src/test/resources/skipper102postgres.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper103", order = 1, locations = { "src/test/resources/skipper103postgres.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper104", order = 1, locations = { "src/test/resources/skipper104postgres.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper105", order = 1, locations = { "src/test/resources/skipper105postgres.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper110", order = 1, locations = { "src/test/resources/skipper110postgres.yml" }, services = { "skipper" }, start = false)
	public void testMigrationFrom100ToLatestWithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {

		// DB and skipper 1.0.0 are coming up automatically in different
		// compose clusters. assert 1.0.0 gets running
		assertServerRunning(dockerComposeInfo, "skipper100", "skipper");

		// stop 1.0.0 and bring up 1.0.1
		// DB were kept running and now asserting that 1.0.1
		// starts ok with schema created with 1.0.0
		upgrade(dockerComposeInfo, "skipper100", "skipper101", "skipper");

		// stop 1.0.1 and bring up 1.0.2
		// DB were kept running and now asserting that 1.0.2
		// starts ok with schema created with 1.0.0 and possibly updated with 1.0.1
		upgrade(dockerComposeInfo, "skipper101", "skipper102", "skipper");

		// and then rest
		upgrade(dockerComposeInfo, "skipper102", "skipper103", "skipper");
		upgrade(dockerComposeInfo, "skipper103", "skipper104", "skipper");
		upgrade(dockerComposeInfo, "skipper104", "skipper105", "skipper");
		upgrade(dockerComposeInfo, "skipper105", "skipper110", "skipper");
	}

	@Test
	@Mysql
	@SkipperAll
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper100", order = 1, locations = { "src/test/resources/skipper100mysql.yml" }, services = { "skipper" })
	@DockerCompose(id = "skipper101", order = 1, locations = { "src/test/resources/skipper101mysql.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper102", order = 1, locations = { "src/test/resources/skipper102mysql.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper103", order = 1, locations = { "src/test/resources/skipper103mysql.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper104", order = 1, locations = { "src/test/resources/skipper104mysql.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper105", order = 1, locations = { "src/test/resources/skipper105mysql.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper110", order = 1, locations = { "src/test/resources/skipper110mysql.yml" }, services = { "skipper" }, start = false)
	public void testMigrationFrom100ToLatestWithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper100", "skipper");
		upgrade(dockerComposeInfo, "skipper100", "skipper101", "skipper");
		upgrade(dockerComposeInfo, "skipper101", "skipper102", "skipper");
		upgrade(dockerComposeInfo, "skipper102", "skipper103", "skipper");
		upgrade(dockerComposeInfo, "skipper103", "skipper104", "skipper");
		upgrade(dockerComposeInfo, "skipper104", "skipper105", "skipper");
		upgrade(dockerComposeInfo, "skipper105", "skipper110", "skipper");
	}

	@Test
	@Oracle
	@SkipperAll
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper100", order = 1, locations = { "src/test/resources/skipper100oracle.yml" }, services = { "skipper" })
	@DockerCompose(id = "skipper101", order = 1, locations = { "src/test/resources/skipper101oracle.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper102", order = 1, locations = { "src/test/resources/skipper102oracle.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper103", order = 1, locations = { "src/test/resources/skipper103oracle.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper104", order = 1, locations = { "src/test/resources/skipper104oracle.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper105", order = 1, locations = { "src/test/resources/skipper105oracle.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper110", order = 1, locations = { "src/test/resources/skipper110oracle.yml" }, services = { "skipper" }, start = false)
	public void testMigrationFrom100ToLatestWithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		assertServerRunning(dockerComposeInfo, "skipper100", "skipper");
		upgrade(dockerComposeInfo, "skipper100", "skipper101", "skipper");
		upgrade(dockerComposeInfo, "skipper101", "skipper102", "skipper");
		upgrade(dockerComposeInfo, "skipper102", "skipper103", "skipper");
		upgrade(dockerComposeInfo, "skipper103", "skipper104", "skipper");
		upgrade(dockerComposeInfo, "skipper104", "skipper105", "skipper");
		upgrade(dockerComposeInfo, "skipper105", "skipper110", "skipper");
	}
}
