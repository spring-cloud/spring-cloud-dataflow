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

import org.junit.Test;

import org.springframework.cloud.dataflow.common.test.docker.compose.execution.Docker;
import org.springframework.cloud.dataflow.common.test.docker.compose.execution.DockerCompose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ContainerCacheTests {

    private static final String CONTAINER_NAME = "container";

    private final Docker docker = mock(Docker.class);
    private final DockerCompose dockerCompose = mock(DockerCompose.class);
    private final ContainerCache containers = new ContainerCache(docker, dockerCompose);

    @Test
    public void return_a_container_with_the_specified_name_when_getting_a_new_container() {
        Container container = containers.container(CONTAINER_NAME);
		assertThat(container).isEqualTo(new Container(CONTAINER_NAME, docker, dockerCompose));
    }

    @Test
    public void return_the_same_object_when_getting_a_container_twice() {
        Container container = containers.container(CONTAINER_NAME);
        Container sameContainer = containers.container(CONTAINER_NAME);
		assertThat(container).isSameAs(sameContainer);
    }

}
