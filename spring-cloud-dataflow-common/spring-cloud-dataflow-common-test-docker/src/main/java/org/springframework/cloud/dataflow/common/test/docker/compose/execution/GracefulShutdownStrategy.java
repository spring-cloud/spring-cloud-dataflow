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
package org.springframework.cloud.dataflow.common.test.docker.compose.execution;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ShutdownStrategy;

/**
 * Send SIGTERM to containers first, allowing them up to 10 seconds to
 * terminate before killing and rm-ing them.
 */
public class GracefulShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownStrategy.class);

    @Override
    public void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException {
        log.debug("Killing docker-compose cluster");
        dockerCompose.down();
        dockerCompose.kill();
        dockerCompose.rm();
        docker.pruneNetworks();
    }

}
