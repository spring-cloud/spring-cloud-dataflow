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

import java.util.Map;
import java.util.Optional;

public enum DockerType implements HostIpResolver, EnvironmentValidator {
    DAEMON(DaemonEnvironmentValidator.instance(), new DaemonHostIpResolver()),
    REMOTE(RemoteEnvironmentValidator.instance(), new RemoteHostIpResolver());

    private final EnvironmentValidator validator;
    private final HostIpResolver resolver;

    DockerType(EnvironmentValidator validator, HostIpResolver resolver) {
        this.validator = validator;
        this.resolver = resolver;
    }

    @Override
    public void validateEnvironmentVariables(Map<String, String> dockerEnvironment) {
        validator.validateEnvironmentVariables(dockerEnvironment);
    }

    @Override
    public String resolveIp(String dockerHost) {
        return resolver.resolveIp(dockerHost);
    }

    public static Optional<DockerType> getFirstValidDockerTypeForEnvironment(Map<String, String> environment) {
        for (DockerType currType : DockerType.values()) {
            try {
                currType.validateEnvironmentVariables(environment);
                return Optional.of(currType);
            } catch (IllegalStateException e) {
                // ignore and try next type
            }
        }
        return Optional.empty();
    }

}
