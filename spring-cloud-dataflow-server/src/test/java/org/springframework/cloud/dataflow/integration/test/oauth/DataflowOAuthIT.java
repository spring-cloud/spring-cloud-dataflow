/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.oauth;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;

import org.springframework.cloud.dataflow.integration.test.db.AbstractDataflowTests;
import org.springframework.cloud.dataflow.integration.test.tags.Oauth;
import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;

import static org.awaitility.Awaitility.with;
import static org.assertj.core.api.Assertions.assertThat;

@Oauth
@ActiveProfiles(TagNames.PROFILE_OAUTH)
class DataflowOAuthIT extends AbstractDataflowTests {

	private final Logger log = LoggerFactory.getLogger(DataflowOAuthIT.class);

	@Test
	void runningUAASecuredSetup() throws Exception {
		log.info("Running UAASecuredSetup");
		this.dataflowCluster.startIdentityProvider(TagNames.UAA_4_32);
		this.dataflowCluster.startSkipper(TagNames.SKIPPER_main);
		this.dataflowCluster.startDataflow(TagNames.DATAFLOW_main);

		// we can't do oauth flow from host due to how oauth works as we
		// need proper networking, so use separate tools container to run
		// curl command as we support basic auth and if we get good response
		// oauth is working with dataflow and skipper.

		AtomicReference<String> stderr = new AtomicReference<>();
		try {
			with()
				.pollInterval(5, TimeUnit.SECONDS)
				.and()
				.await()
				.ignoreExceptions()
				.atMost(90, TimeUnit.SECONDS)
				.untilAsserted(() -> {
					assertGetAboutWithUser("janne", "janne", stderr);
				});
			log.info("Checking without credentials using curl");
			assertAboutWithoutUser(stderr);
		}
		finally {
			String msg = stderr.get();
			if (StringUtils.hasText(msg)) {
				log.error("curl error: {}", msg);
			}
		}
	}

	@Test
	void runningKeycloakSecuredSetup() throws Exception {
		log.info("Running KeycloakSecuredSetup");
		this.dataflowCluster.startIdentityProvider(TagNames.KEYCLOAK_26);
		this.dataflowCluster.startSkipper(TagNames.SKIPPER_main);
		this.dataflowCluster.startDataflow(TagNames.DATAFLOW_main);

		// we can't do oauth flow from host due to how oauth works as we
		// need proper networking, so use separate tools container to run
		// curl command as we support basic auth and if we get good response
		// oauth is working with dataflow and skipper.

		AtomicReference<String> stderr = new AtomicReference<>();
		try {
			with()
				.pollInterval(5, TimeUnit.SECONDS)
				.and()
				.await()
				.ignoreExceptions()
				.atMost(90, TimeUnit.SECONDS)
				.untilAsserted(() -> {
					assertGetAboutWithUser("joe", "password", stderr);
				});
			log.info("Checking without credentials using curl");
			assertAboutWithoutUser(stderr);
		}
		finally {
			String msg = stderr.get();
			if (StringUtils.hasText(msg)) {
				log.error("curl error: {}", msg);
			}
		}
	}

	private void assertGetAboutWithUser(String username, String password, AtomicReference<String> stderr) throws IOException, InterruptedException {
		log.info("Checking auth using curl");
		ExecResult cmdResult = execInToolsContainer("curl", "-v", "-u", username + ":" + password, "http://dataflow:9393/about");
		String response = cmdResult.getStdout();
		if (StringUtils.hasText(response)) {
			log.info("Response is {}", response);
		}
		if(StringUtils.hasText(cmdResult.getStderr())) {
			log.error(cmdResult.getStderr());
		}
		stderr.set(cmdResult.getStderr());
		assertThat(response).contains("\"authenticated\":true");
		assertThat(response).contains("\"username\":\"" + username + "\"");
		stderr.set("");
	}

	private void assertAboutWithoutUser(AtomicReference<String> stderr) throws IOException, InterruptedException {
		ExecResult cmdResult = execInToolsContainer("curl", "-v", "-f", "http://dataflow:9393/about");
		String response = cmdResult.getStdout();
		if (StringUtils.hasText(response)) {
			log.info("Response is {}", response);
		}
		response = cmdResult.getStderr();
		if(StringUtils.hasText(response)) {
			log.warn("Error is {}", response);
		}
		stderr.set(cmdResult.getStderr());
		assertThat(cmdResult.getExitCode()).isNotZero();
		stderr.set("");
	}
}
