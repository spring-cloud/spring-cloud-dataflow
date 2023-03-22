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
package org.springframework.cloud.dataflow.common.test.docker.compose;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.ContainerName;

public class TestContainerNames {

    private TestContainerNames() {}

    public static List<ContainerName> of(String... semanticNames) {
        return Arrays.stream(semanticNames)
                .map(TestContainerNames::testContainerName)
                .collect(toList());
    }

    private static ContainerName testContainerName(String testName) {
        return ContainerName.builder()
                .semanticName(testName)
                .rawName("123456_" + testName + "_1")
                .build();
    }

}
