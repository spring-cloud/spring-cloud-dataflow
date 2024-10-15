/*
 * Copyright 2018-2024 the original author or authors.
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

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DockerTests {

    private DockerExecutable executor = mock(DockerExecutable.class);
    private Docker docker = new Docker(executor);
    private Process executedProcess = mock(Process.class);

    @BeforeEach
    void prepareForTest() throws IOException {
        when(executor.commandName()).thenReturn("docker-compose");
        when(executor.execute(anyBoolean())).thenReturn(executedProcess);
        when(executor.execute(anyBoolean(), any(String[].class))).thenReturn(executedProcess);
        when(executedProcess.exitValue()).thenReturn(0);
    }
    
    @Test
    void callDockerRmWithForceFlagOnRm() throws Exception {
        when(executedProcess.getInputStream()).thenReturn(IOUtils.toInputStream(""));
        docker.rm("testContainer");
        verify(executor).execute(false,"rm", "-f", "testContainer");
    }

    @Test
    void callDockerNetworkLs() throws Exception {
        String lsOutput = "0.0.0.0:7000->7000/tcp";
        when(executedProcess.getInputStream()).thenReturn(IOUtils.toInputStream(lsOutput));
		assertThat(docker.listNetworks()).isEqualTo(lsOutput);
        verify(executor).execute(false, "network", "ls");
    }

    @Test
    void callDockerNetworkPrune() throws Exception {
        String lsOutput = "0.0.0.0:7000->7000/tcp";
        when(executedProcess.getInputStream()).thenReturn(IOUtils.toInputStream(lsOutput));
		assertThat(docker.pruneNetworks()).isEqualTo(lsOutput);
        verify(executor).execute(false,"network", "prune", "--force");
    }

    @Test
    void understandOldVersionFormat() throws Exception {
        when(executedProcess.getInputStream()).thenReturn(IOUtils.toInputStream("Docker version 1.7.2"));
        Version version = docker.configuredVersion();
		assertThat(version).isEqualTo(Version.valueOf("1.7.2"));
    }

    @Test
    void understandNewVersionFormat() throws Exception {
        when(executedProcess.getInputStream()).thenReturn(IOUtils.toInputStream("Docker version 17.03.1-ce"));
        Version version = docker.configuredVersion();
		assertThat(version).isEqualTo(Version.valueOf("17.3.1"));
    }
}
