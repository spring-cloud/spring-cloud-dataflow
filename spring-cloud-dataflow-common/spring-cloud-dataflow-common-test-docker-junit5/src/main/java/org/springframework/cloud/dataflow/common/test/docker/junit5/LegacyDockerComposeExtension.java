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
package org.springframework.cloud.dataflow.common.test.docker.junit5;

import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.cloud.dataflow.common.test.docker.compose.DockerComposeRule;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DockerComposeFiles;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ProjectName;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerMachine;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.ClusterWait;
import org.springframework.cloud.dataflow.common.test.docker.compose.logging.LogCollector;

public class LegacyDockerComposeExtension extends DockerComposeRule implements BeforeAllCallback, AfterAllCallback {

	private LegacyDockerComposeExtension(DockerComposeFiles files, List<ClusterWait> clusterWaits,
			LogCollector logCollector, DockerMachine machine, boolean pullOnStartup, ProjectName projectName) {
		super(files, clusterWaits, logCollector, machine, pullOnStartup, projectName);
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		after();
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		before();
	}

    public static Builder builder() {
        return new Builder();
    }

	public static class Builder extends DockerComposeRule.Builder<Builder>  {

		@Override
		public LegacyDockerComposeExtension build() {
			return new LegacyDockerComposeExtension(files, clusterWaits, logCollector, machine, pullOnStartup,
					projectName);
		}
	}
}
