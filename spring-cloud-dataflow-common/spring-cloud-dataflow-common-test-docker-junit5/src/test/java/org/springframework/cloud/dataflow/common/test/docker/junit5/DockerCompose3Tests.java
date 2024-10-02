/*
 * Copyright 2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerExecutionException;
import org.springframework.cloud.dataflow.common.test.docker.junit5.DockerCompose;
import org.springframework.cloud.dataflow.common.test.docker.junit5.DockerComposeInfo;

@DockerCompose(locations = {"classpath:org/springframework/cloud/dataflow/common/test/docker/junit5/docker-compose-cp1.yml"})
class DockerCompose3Tests {

	@Test
	void compose(DockerComposeInfo dockerComposeInfo) throws IOException, InterruptedException {
		assertThat(dockerComposeInfo).isNotNull();
		assertThat(dockerComposeInfo.id("").getRule()).isNotNull();
		assertThat(dockerComposeInfo.id("").getRule().containers().container("testservice1")).isNotNull();

		Throwable thrown = catchThrowable(() -> {
			dockerComposeInfo.id("").getRule().containers().container("testservice2").state();
		});
		assertThat(thrown).isInstanceOf(DockerExecutionException.class).hasNoCause()
				.message().containsIgnoringCase("No such service: testservice2");
	}
}
