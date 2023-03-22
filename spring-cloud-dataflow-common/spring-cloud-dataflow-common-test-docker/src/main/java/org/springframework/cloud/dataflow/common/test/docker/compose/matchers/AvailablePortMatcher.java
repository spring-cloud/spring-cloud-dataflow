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
package org.springframework.cloud.dataflow.common.test.docker.compose.matchers;

import java.util.Collection;
import java.util.stream.Collectors;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerPort;

public class AvailablePortMatcher extends TypeSafeMatcher<Collection<DockerPort>> {

    @Override
    public void describeTo(Description description) {
        description.appendText("No ports to be unavailable");
    }

    @Override
    protected boolean matchesSafely(Collection<DockerPort> unavailablePorts) {
        return unavailablePorts.isEmpty();
    }

    @Override
    protected void describeMismatchSafely(Collection<DockerPort> unavailablePorts, Description mismatchDescription) {
        mismatchDescription.appendValueList("These ports were unavailable:\n", "\n", ".", buildClosedPortsErrorMessage(unavailablePorts));
    }

    private static Collection<String> buildClosedPortsErrorMessage(Collection<DockerPort> unavailablePorts) {
        return unavailablePorts.stream()
                               .map(port -> "For host with ip address: " + port.getIp() + " external port '" + port.getExternalPort() + "' mapped to internal port '" + port.getInternalPort() + "' was unavailable")
                               .collect(Collectors.toList());
    }

    public static AvailablePortMatcher areAvailable() {
        return new AvailablePortMatcher();
    }

}
