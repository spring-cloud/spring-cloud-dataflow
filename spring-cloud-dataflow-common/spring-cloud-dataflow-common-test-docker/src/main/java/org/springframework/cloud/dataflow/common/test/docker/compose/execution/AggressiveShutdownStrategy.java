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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.ShutdownStrategy;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerName;

import static java.util.stream.Collectors.toList;

/**
 * Shuts down containers as fast as possible, without giving them time to finish
 * IO or clean up any resources.
 *
 * @deprecated Use {@link ShutdownStrategy#KILL_DOWN}
 */
@Deprecated
public class AggressiveShutdownStrategy implements ShutdownStrategy {

    private static final Logger log = LoggerFactory.getLogger(AggressiveShutdownStrategy.class);

    @Override
    public void shutdown(DockerCompose dockerCompose, Docker docker) throws IOException, InterruptedException {
        List<ContainerName> runningContainers = dockerCompose.ps();

        log.info("Shutting down {}", runningContainers.stream().map(ContainerName::semanticName).collect(toList()));
        if (removeContainersCatchingErrors(docker, runningContainers)) {
            return;
        }

        log.debug("First shutdown attempted failed due to btrfs volume error... retrying");
        if (removeContainersCatchingErrors(docker, runningContainers)) {
            return;
        }

        log.warn("Couldn't shut down containers due to btrfs volume error, "
                + "see https://circleci.com/docs/docker-btrfs-error/ for more info.");

        log.info("Pruning networks");
        docker.pruneNetworks();
    }

    private static boolean removeContainersCatchingErrors(Docker docker, List<ContainerName> runningContainers) throws IOException, InterruptedException {
        try {
            removeContainers(docker, runningContainers);
            return true;
        } catch (DockerExecutionException exception) {
            return false;
        }
    }

    private static void removeContainers(Docker docker, List<ContainerName> running) throws IOException, InterruptedException {
        List<String> rawContainerNames = running.stream()
                .map(ContainerName::rawName)
                .collect(toList());

        docker.rm(rawContainerNames);
        log.debug("Finished shutdown");
    }

}
