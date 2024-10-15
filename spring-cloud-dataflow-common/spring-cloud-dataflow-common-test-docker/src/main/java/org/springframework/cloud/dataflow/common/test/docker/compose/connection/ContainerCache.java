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
package org.springframework.cloud.dataflow.common.test.docker.compose.connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.dataflow.common.test.docker.compose.execution.Docker;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerCompose;

import static java.util.stream.Collectors.toSet;

public class ContainerCache {

    private final Map<String, Container> containers = new HashMap<>();
    private final Docker docker;
    private final DockerCompose dockerCompose;

    public ContainerCache(Docker docker, DockerCompose dockerCompose) {
        this.docker = docker;
        this.dockerCompose = dockerCompose;
    }

    public Container container(String containerName) {
        containers.putIfAbsent(containerName, new Container(containerName, docker, dockerCompose));
        return containers.get(containerName);
    }

    public Set<Container> containers() throws IOException, InterruptedException {
        return dockerCompose.services().stream().map(this::container).collect(toSet());
    }

}
