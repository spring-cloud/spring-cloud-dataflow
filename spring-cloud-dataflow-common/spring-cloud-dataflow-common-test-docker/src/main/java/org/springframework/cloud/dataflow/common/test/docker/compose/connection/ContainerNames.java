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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class ContainerNames {

    private ContainerNames() {}

    public static List<ContainerName> parseFromDockerComposePs(String psOutput) {
        String[] psHeadAndBody = psOutput.split("-+(\r|\n)+");
        if (psHeadAndBody.length < 2) {
            return emptyList();
        }

        String psBody = psHeadAndBody[1];
        return psBodyLines(psBody)
                .map(ContainerName::fromPsLine)
                .collect(toList());
    }

    private static Stream<String> psBodyLines(String psBody) {
        String[] lines = psBody.split("(\r|\n)+");
        return Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !line.isEmpty());
    }

}
