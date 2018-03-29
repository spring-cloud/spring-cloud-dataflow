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
 * server works with initial schema creation.
 *
 * @author Janne Valkealahti
 *
 */
@ExtendWith(DockerComposeExtension.class)
public class SkipperServerInitialBootstrapTests {

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper100postgres.yml" }, services = { "skipper" })
	public void testSkipper100WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}

	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper101postgres.yml" }, services = { "skipper" })
	public void testSkipper101WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}

	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/postgres.yml" }, services = { "postgres" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper102postgres.yml" }, services = { "skipper" })
	public void testSkipper102WithPostgres(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper100mysql.yml" }, services = { "skipper" })
	public void testSkipper100WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper101mysql.yml" }, services = { "skipper" })
	public void testSkipper101WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/mysql.yml" }, services = { "mysql" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper102mysql.yml" }, services = { "skipper" })
	public void testSkipper102WithMysql(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper100oracle.yml" }, services = { "skipper" })
	public void testSkipper100WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper101oracle.yml" }, services = { "skipper" })
	public void testSkipper101WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}

	@Test
	@DockerCompose(id = "db", order = 0, locations = { "src/test/resources/oracle.yml" }, services = { "oracle" })
	@DockerCompose(id = "skipper", order = 1, locations = { "src/test/resources/skipper102oracle.yml" }, services = { "skipper" })
	public void testSkipper102WithOracle(DockerComposeInfo dockerComposeInfo) throws Exception {
		DockerPort port = dockerComposeInfo.id("skipper").getRule().containers().container("skipper").port(7577);
		String url = "http://" + port.getIp() + ":" + port.getExternalPort() + "/api/about";
		AssertUtils.assertServerRunning(url);
	}
}
