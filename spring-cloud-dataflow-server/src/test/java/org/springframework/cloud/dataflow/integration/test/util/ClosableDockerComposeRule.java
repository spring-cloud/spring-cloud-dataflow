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
package org.springframework.cloud.dataflow.integration.test.util;

import com.palantir.docker.compose.DockerComposeRule;
import org.junit.rules.ExternalResource;

/**
 * DockerComposeRule doesnt't release the created containers if the before() fails.
 * The ClosableDockerComposeRule ensures that all containers are shutdown in case of failure.
 *
 * @author Christian Tzolov
 */
public class ClosableDockerComposeRule extends ExternalResource {

	private DockerComposeRule dockerComposeRule;

	private ClosableDockerComposeRule(DockerComposeRule dockerComposeRule) {
		this.dockerComposeRule = dockerComposeRule;
	}

	/**
	 * Handy factory method
	 * @param dockerComposeRule
	 * @return ClosableDockerComposeRule instance that ensures all docker containers are removed on failure.
	 */
	public static ClosableDockerComposeRule of(DockerComposeRule dockerComposeRule) {
		return new ClosableDockerComposeRule(dockerComposeRule);
	}

	@Override
	protected void before() throws Throwable {
		try {
			dockerComposeRule.before();
		}
		catch (Exception ex) {
			dockerComposeRule.after();
			throw ex;
		}
	}

	@Override
	protected void after() {
		dockerComposeRule.after();
	}
}

