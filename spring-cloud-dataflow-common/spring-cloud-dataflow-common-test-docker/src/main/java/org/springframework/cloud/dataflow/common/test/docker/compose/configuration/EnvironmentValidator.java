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

public interface EnvironmentValidator {

    /**
     * Validates that the entries in the provided map are valid for the current environment.
     * The provided map represents the environment variables that should be used for the
     * process, where the keys are the environment variable names and the values are the values.
     * If the validator determines the state represented by the map is invalid (either because
     * required values are missing or forbidden values are present), the method should throw
     * an exception.
     *
     * @param dockerEnvironment A map representing the docker environment
     */
    void validateEnvironmentVariables(Map<String, String> dockerEnvironment);

}
