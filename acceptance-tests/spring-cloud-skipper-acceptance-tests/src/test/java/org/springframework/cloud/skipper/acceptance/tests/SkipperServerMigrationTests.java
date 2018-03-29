/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.acceptance.tests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.skipper.acceptance.core.DockerCompose;
import org.springframework.cloud.skipper.acceptance.core.DockerComposeExtension;
import org.springframework.cloud.skipper.acceptance.core.DockerComposeInfo;
import org.springframework.cloud.skipper.acceptance.tests.support.AssertUtils;

import com.palantir.docker.compose.connection.DockerPort;

/**
 * Tests going through start of skipper servers with databases and verifying
 * that newer versions work when older versions have created initial db schemas.
 *
 * @author Janne Valkealahti
 *
 */
@ExtendWith(DockerComposeExtension.class)
public class SkipperServerMigrationTests {

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper100", order = 1, locations = { "src/test/resources/skipper100postgres.yml" }, services = { "skipper" })
	@DockerCompose(id = "skipper101", order = 1, locations = { "src/test/resources/skipper101postgres.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper102", order = 1, locations = { "src/test/resources/skipper102postgres.yml" }, services = { "skipper" }, start = false)
	public void testMigrationFrom100To101WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {

		// DB and skipper 1.0.0 are coming up automatically in different
		// compose clusters. assert 1.0.0 gets running
		DockerPort port1 = dockerComposeInfo.id("skipper100").getRule().containers().container("skipper").port(7577);
		String url1 = "http://" + port1.getIp() + ":" + port1.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url1);

		// stop 1.0.0 and bring up 1.0.1
		dockerComposeInfo.id("skipper100").stop();
		dockerComposeInfo.id("skipper101").start();

		// DB were kept running and now asserting that 1.0.1
		// starts ok with schema created with 1.0.0
		DockerPort port2 = dockerComposeInfo.id("skipper101").getRule().containers().container("skipper").port(7577);
		String url2 = "http://" + port2.getIp() + ":" + port2.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url2);

		// stop 1.0.1 and bring up 1.0.2
		dockerComposeInfo.id("skipper101").stop();
		dockerComposeInfo.id("skipper102").start();

		// DB were kept running and now asserting that 1.0.2
		// starts ok with schema created with 1.0.0 and possibly updated with 1.0.1
		DockerPort port3 = dockerComposeInfo.id("skipper102").getRule().containers().container("skipper").port(7577);
		String url3 = "http://" + port3.getIp() + ":" + port3.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url3);
	}

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper100", order = 1, locations = { "src/test/resources/skipper100mysql.yml" }, services = { "skipper" })
	@DockerCompose(id = "skipper101", order = 1, locations = { "src/test/resources/skipper101mysql.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper102", order = 1, locations = { "src/test/resources/skipper102mysql.yml" }, services = { "skipper" }, start = false)
	public void testMigrationFrom100To101WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port1 = dockerComposeInfo.id("skipper100").getRule().containers().container("skipper").port(7577);
		String url1 = "http://" + port1.getIp() + ":" + port1.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url1);

		dockerComposeInfo.id("skipper100").stop();
		dockerComposeInfo.id("skipper101").start();

		DockerPort port2 = dockerComposeInfo.id("skipper101").getRule().containers().container("skipper").port(7577);
		String url2 = "http://" + port2.getIp() + ":" + port2.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url2);

		dockerComposeInfo.id("skipper101").stop();
		dockerComposeInfo.id("skipper102").start();

		DockerPort port3 = dockerComposeInfo.id("skipper102").getRule().containers().container("skipper").port(7577);
		String url3 = "http://" + port3.getIp() + ":" + port3.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url3);
	}

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper100", order = 1, locations = { "src/test/resources/skipper100oracle.yml" }, services = { "skipper" })
	@DockerCompose(id = "skipper101", order = 1, locations = { "src/test/resources/skipper101oracle.yml" }, services = { "skipper" }, start = false)
	@DockerCompose(id = "skipper102", order = 1, locations = { "src/test/resources/skipper102oracle.yml" }, services = { "skipper" }, start = false)
	public void testMigrationFrom100To101WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port1 = dockerComposeInfo.id("skipper100").getRule().containers().container("skipper").port(7577);
		String url1 = "http://" + port1.getIp() + ":" + port1.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url1);

		dockerComposeInfo.id("skipper100").stop();
		dockerComposeInfo.id("skipper101").start();

		DockerPort port2 = dockerComposeInfo.id("skipper101").getRule().containers().container("skipper").port(7577);
		String url2 = "http://" + port2.getIp() + ":" + port2.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url2);

		dockerComposeInfo.id("skipper101").stop();
		dockerComposeInfo.id("skipper102").start();

		DockerPort port3 = dockerComposeInfo.id("skipper102").getRule().containers().container("skipper").port(7577);
		String url3 = "http://" + port3.getIp() + ":" + port3.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url3);
	}
}
