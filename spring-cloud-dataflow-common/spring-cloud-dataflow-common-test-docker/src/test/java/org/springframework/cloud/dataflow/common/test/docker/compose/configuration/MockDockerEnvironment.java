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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerPort;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Ports;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting.SuccessOrFailure;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerCompose;

public class MockDockerEnvironment {

    private final DockerCompose dockerComposeProcess;

    public MockDockerEnvironment(DockerCompose dockerComposeProcess) {
        this.dockerComposeProcess = dockerComposeProcess;
    }

    public DockerPort availableService(String service, String ip, int externalPortNumber, int internalPortNumber) throws Exception {
        DockerPort port = port(service, ip, externalPortNumber, internalPortNumber);
        doReturn(true).when(port).isListeningNow();
        return port;
    }

    public DockerPort unavailableService(String service, String ip, int externalPortNumber, int internalPortNumber) throws Exception {
        DockerPort port = port(service, ip, externalPortNumber, internalPortNumber);
        doReturn(false).when(port).isListeningNow();
        return port;
    }

    public DockerPort availableHttpService(String service, String ip, int externalPortNumber, int internalPortNumber) throws Exception {
        DockerPort port = availableService(service, ip, externalPortNumber, internalPortNumber);
        doReturn(true).when(port).isHttpResponding(any(), eq(false));
        doReturn(SuccessOrFailure.success()).when(port).isHttpRespondingSuccessfully(any(), eq(false));
        return port;
    }

    public DockerPort unavailableHttpService(String service, String ip, int externalPortNumber, int internalPortNumber) throws Exception {
        DockerPort port = availableService(service, ip, externalPortNumber, internalPortNumber);
        doReturn(false).when(port).isHttpResponding(any(), eq(false));
        return port;
    }

    public DockerPort port(String service, String ip, int externalPortNumber, int internalPortNumber) throws IOException, InterruptedException {
        DockerPort port = dockerPortSpy(ip, externalPortNumber, internalPortNumber);
        when(dockerComposeProcess.ports(service)).thenReturn(new Ports(port));
        return port;
    }

    public void ephemeralPort(String service, String ip, int internalPortNumber) throws IOException, InterruptedException {
        AtomicInteger currentExternalPort = new AtomicInteger(33700);
        when(dockerComposeProcess.ports(service)).then(a -> {
            DockerPort port = dockerPortSpy(ip, currentExternalPort.incrementAndGet(), internalPortNumber);
            return new Ports(port);
        });
    }

    public void ports(String service, String ip, Integer... portNumbers) throws IOException, InterruptedException {
        List<DockerPort> ports = Arrays.asList(portNumbers)
                                         .stream()
                                         .map(portNumber -> dockerPortSpy(ip, portNumber, portNumber))
                                         .collect(Collectors.toList());
        when(dockerComposeProcess.ports(service)).thenReturn(new Ports(ports));
    }

    private static DockerPort dockerPortSpy(String ip, int externalPortNumber, int internalPortNumber) {
        DockerPort port = new DockerPort(ip, externalPortNumber, internalPortNumber);
        return spy(port);
    }
}
