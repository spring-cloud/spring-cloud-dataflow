/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.dataflow.integration.test;

import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.configuration.DockerComposeFiles;
import com.palantir.docker.compose.connection.DockerMachine;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.springframework.cloud.dataflow.integration.test.util.ClosableDockerComposeRule;

/**
 * IT Tests with Postgres, InfluxDB and RabbitMQ
 *
 * @author Christian Tzolov
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		DockerComposeTestPlatform.class,
		DockerComposeTestStream.class,
		DockerComposeTestTask.class
})
public class DockerComposePostgresRabbitInfluxIT {

	@ClassRule
	public static ExternalResource dockerRuleWrapper = ClosableDockerComposeRule.of(
			DockerComposeRule.builder()
					.files(DockerComposeFiles.from(
							"docker-compose.yml",
							"docker-compose-influxdb.yml",
							"docker-compose-postgres.yml",
							"docker-compose-rabbitmq.yml"))
					.machine(DockerMachine.localMachine()
							.withAdditionalEnvironmentVariable("DATAFLOW_VERSION", DockerComposeIT.DATAFLOW_VERSION)
							.withAdditionalEnvironmentVariable("SKIPPER_VERSION", DockerComposeIT.SKIPPER_VERSION)
							.withAdditionalEnvironmentVariable("STREAM_APPS_URI", DockerComposeIT.RABBITMQ_MAVEN_STREAM_APPS_URI)
							.withAdditionalEnvironmentVariable("TASK_APPS_URI", DockerComposeIT.TASK_APPS_URI)
							.build())
					.saveLogsTo("target/dockerLogs/DockerComposePostgresRabbitInfluxIT")
					.waitingForService("dataflow-server", HealthChecks.toRespond2xxOverHttp(9393,
							(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")))
					.waitingForService("skipper-server", HealthChecks.toRespond2xxOverHttp(7577,
							(port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT")))
					.pullOnStartup(true)
					.build());
}
