/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.configuration;

import java.io.IOException;

import org.springframework.cloud.dataflow.common.test.docker.compose.execution.Docker;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerCompose;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.GracefulShutdownStrategy;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.KillDownShutdownStrategy;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.SkipShutdownStrategy;

/**
 * How should a cluster of containers be shut down by the `after` method of
 * DockerComposeRule.
 */
public interface ShutdownStrategy {

    /**
     * Call docker-compose down, kill, then rm. Allows containers up to 10 seconds to shut down
     * gracefully.
     *
     * <p>With this strategy, you will need to take care not to accidentally write images
     * that ignore their down signal, for instance by putting their run command in as a
     * string (which is interpreted by a SIGTERM-ignoring bash) rather than an array of strings.
     */
    ShutdownStrategy GRACEFUL = new GracefulShutdownStrategy();
    /**
     * Call docker-compose kill then down.
     */
    ShutdownStrategy KILL_DOWN = new KillDownShutdownStrategy();
    /**
     * Skip shutdown, leaving containers running after tests finish executing.
     *
     * <p>You can use this option to speed up repeated test execution locally by leaving
     * images up between runs. Do <b>not</b> commit it! You will be potentially abandoning
     * long-running processes and leaking resources on your CI platform!
     */
    ShutdownStrategy SKIP = new SkipShutdownStrategy();

    void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException;

}
