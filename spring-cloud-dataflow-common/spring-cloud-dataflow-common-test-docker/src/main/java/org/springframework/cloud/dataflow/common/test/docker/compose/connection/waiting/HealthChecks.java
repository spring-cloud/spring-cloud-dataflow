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
package org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting;

import java.util.function.Function;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.Container;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerPort;

public final class HealthChecks {

    private HealthChecks() {}

    public static HealthCheck<Container> toRespondOverHttp(int internalPort, Function<DockerPort, String> urlFunction) {
        return container -> container.portIsListeningOnHttp(internalPort, urlFunction);
    }

    public static HealthCheck<Container> toRespond2xxOverHttp(int internalPort, Function<DockerPort, String> urlFunction) {
        return container -> container.portIsListeningOnHttpAndCheckStatus2xx(internalPort, urlFunction);
    }

    public static HealthCheck<Container> toHaveAllPortsOpen() {
        return Container::areAllPortsOpen;
    }
}
