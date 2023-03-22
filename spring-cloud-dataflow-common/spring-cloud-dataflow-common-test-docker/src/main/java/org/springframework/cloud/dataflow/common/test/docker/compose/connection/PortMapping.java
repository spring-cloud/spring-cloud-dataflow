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

import java.util.Objects;

public class PortMapping {

    private final int externalPort;
    private final int internalPort;

    public PortMapping(int externalPort, int internalPort) {
        this.externalPort = externalPort;
        this.internalPort = internalPort;
    }

    public int getExternalPort() {
        return externalPort;
    }

    public int getInternalPort() {
        return internalPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalPort, internalPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PortMapping other = (PortMapping) obj;
        return Objects.equals(externalPort, other.externalPort)
                && Objects.equals(internalPort, other.internalPort);
    }

    @Override
    public String toString() {
        return "PortMapping [externalPort=" + externalPort + ", internalPort="
                + internalPort + "]";
    }

}
