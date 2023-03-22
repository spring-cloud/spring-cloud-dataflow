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
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Container;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerName;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Ports;

abstract class DelegatingDockerCompose implements DockerCompose {
    private final DockerCompose dockerCompose;

    protected DelegatingDockerCompose(DockerCompose dockerCompose) {
        this.dockerCompose = dockerCompose;
    }

    @Override
    public void pull() throws IOException, InterruptedException {
        dockerCompose.pull();
    }

    @Override
    public void build() throws IOException, InterruptedException {
        dockerCompose.build();
    }

    @Override
    public void up() throws IOException, InterruptedException {
        dockerCompose.up();
    }

    @Override
    public void down() throws IOException, InterruptedException {
        dockerCompose.down();
    }

    @Override
    public void kill() throws IOException, InterruptedException {
        dockerCompose.kill();
    }

    @Override
    public void rm() throws IOException, InterruptedException {
        dockerCompose.rm();
    }

    @Override
    public void up(Container container) throws IOException, InterruptedException {
        dockerCompose.up(container);
    }

    @Override
    public void start(Container container) throws IOException, InterruptedException {
        dockerCompose.start(container);
    }

    @Override
    public void stop(Container container) throws IOException, InterruptedException {
        dockerCompose.stop(container);
    }

    @Override
    public void kill(Container container) throws IOException, InterruptedException {
        dockerCompose.kill(container);
    }

    @Override
    public String exec(DockerComposeExecOption dockerComposeExecOption, String containerName,
            DockerComposeExecArgument dockerComposeExecArgument) throws IOException, InterruptedException {
        return dockerCompose.exec(dockerComposeExecOption, containerName, dockerComposeExecArgument);
    }

    @Override
    public String run(DockerComposeRunOption dockerComposeRunOption, String containerName,
            DockerComposeRunArgument dockerComposeRunArgument) throws IOException, InterruptedException {
        return dockerCompose.run(dockerComposeRunOption, containerName, dockerComposeRunArgument);
    }

    @Override
    public List<ContainerName> ps() throws IOException, InterruptedException {
        return dockerCompose.ps();
    }

    @Override
    public Optional<String> id(Container container) throws IOException, InterruptedException {
        return dockerCompose.id(container);
    }

    @Override
    public String config() throws IOException, InterruptedException {
        return dockerCompose.config();
    }

    @Override
    public List<String> services() throws IOException, InterruptedException {
        return dockerCompose.services();
    }

    @Override
    public boolean writeLogs(String container, OutputStream output) throws IOException {
        return dockerCompose.writeLogs(container, output);
    }

    @Override
    public Ports ports(String service) throws IOException, InterruptedException {
        return dockerCompose.ports(service);
    }

    protected final DockerCompose getDockerCompose() {
        return dockerCompose;
    }

}
