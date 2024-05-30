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

import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;

import org.springframework.cloud.dataflow.integration.test.db.AbstractDataflowTests;
import org.springframework.cloud.dataflow.integration.test.tags.Oauth;
import org.springframework.cloud.dataflow.integration.test.tags.TagNames;
import org.springframework.cloud.dataflow.rest.client.support.VersionUtils;
import org.springframework.test.context.ActiveProfiles;

import static org.awaitility.Awaitility.with;

@Oauth
@ActiveProfiles({TagNames.PROFILE_OAUTH})
public class DataflowOAuthIT extends AbstractDataflowTests {

	private final Logger log = LoggerFactory.getLogger(DataflowOAuthIT.class);

	@Test
	public void testSecuredSetup() throws Exception {
		log.info("Running testSecuredSetup()");
		this.dataflowCluster.startIdentityProvider(TagNames.UAA_4_32);
		this.dataflowCluster.startSkipper(TagNames.SKIPPER_main);
		this.dataflowCluster.startDataflow(TagNames.DATAFLOW_main);

		// we can't do oauth flow from host due to how oauth works as we
		// need proper networking, so use separate tools container to run
		// curl command as we support basic auth and if we get good response
		// oauth is working with dataflow and skipper.
		with()
			.pollInterval(5, TimeUnit.SECONDS)
			.and()
			.await()
				.ignoreExceptions()
				.atMost(120, TimeUnit.SECONDS)
				.until(() -> {
					log.info("Checking auth using curl");
					ExecResult cmdResult = execInToolsContainer("curl", "-u", "janne:janne", "http://dataflow:9393/about");
					String response = cmdResult.getStdout();
					log.debug("Response is {}", response);
					Boolean authenticated = JsonPath.parse(response).read("$.securityInfo.authenticated", Boolean.class);
					String username = JsonPath.parse(response).read("$.securityInfo.username", String.class);
					boolean ok = Boolean.TRUE.equals(authenticated) && "janne".equals(username);
					log.info("Check for oauth {}", ok);
					if (ok) {
						String version = JsonPath.parse(response).read("$.versionInfo.core.version");
						log.info("Version=[{}]", version);
						String api = "tasks/executions";
						if (VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion(
								VersionUtils.getThreePartVersion(version), "2.11.3")) {
							api = "tasks/thinexecutions";
						}
						log.info("Checking {}", api);
						cmdResult = execInToolsContainer("curl", "-u", "janne:janne", "http://dataflow:9393/" + api);
						response = cmdResult.getStdout();
						log.debug("Response is {}", response);
						ok = !JsonPath.parse(response).read("$._links.self.href", String.class).isEmpty();
					}
					return ok;
				});
	}
}
