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

public enum State {
    DOWN, PAUSED, UNHEALTHY, HEALTHY;

    /**
     * Returns true if the container is up, unpaused and healthy.
     *
     * @return true if the container is up, unpaused and healthy
     */
    public boolean isHealthy() {
        return this == HEALTHY;
    }

    /**
     * Returns true if the container is up but not necessarily unpaused or healthy.
     *
     * @return true if the container is up but not necessarily unpaused or healthy
     */
    public boolean isUp() {
        return this != DOWN;
    }

    /**
     * Returns true if the container is paused.
     *
     * @return true if the container is paused
     */
    public boolean isPaused() {
        return this == PAUSED;
    }
}
