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

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerName;

public class RetryingDockerCompose extends DelegatingDockerCompose {
    private final Retryer retryer;

    public RetryingDockerCompose(int retryAttempts, DockerCompose dockerCompose) {
        this(new Retryer(retryAttempts, Retryer.STANDARD_DELAY), dockerCompose);
    }

    public RetryingDockerCompose(Retryer retryer, DockerCompose dockerCompose) {
        super(dockerCompose);
        this.retryer = retryer;
    }

    @Override
    public void up() throws IOException, InterruptedException {
        retryer.<Void>runWithRetries(() -> {
            super.up();
            return null;
        });
    }

    @Override
    public List<ContainerName> ps() throws IOException, InterruptedException {
        return retryer.runWithRetries(super::ps);
    }
}
