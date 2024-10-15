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

import java.util.List;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class ContainerNameTests {

    @Test
    public void parse_a_semantic_and_raw_name_correctly_from_a_single_line() {
        ContainerName actual = ContainerName.fromPsLine("dir_db_1 other line contents");

        ContainerName expected = ContainerName.builder()
                .rawName("dir_db_1")
                .semanticName("db")
                .build();

		assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void can_handle_custom_container_names() {
        ContainerName name = ContainerName.fromPsLine("test-1.container.name   /docker-entrypoint.sh postgres   Up      5432/tcp");

        ContainerName expected = ContainerName.builder()
                .rawName("test-1.container.name")
                .semanticName("test-1.container.name")
                .build();

		assertThat(name).isEqualTo(expected);
    }

    @Test
    public void result_in_no_container_names_when_ps_output_is_empty() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\n");
		assertThat(names).isEqualTo(emptyList());
    }

    @Test
    public void result_in_a_single_container_name_when_ps_output_has_a_single_container() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents");
		assertThat(names).containsExactly(containerName("dir", "db", "1"));
    }

    @Test
    public void allow_windows_newline_characters() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\r\n----\r\ndir_db_1 other line contents");
		assertThat(names).containsExactly(containerName("dir", "db", "1"));
    }

    @Test
    public void allow_containers_with_underscores_in_their_name() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_left_right_1 other line contents");
		assertThat(names).containsExactly(containerName("dir", "left_right", "1"));
    }

    @Test
    public void result_in_two_container_names_when_ps_output_has_two_containers() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\ndir_db2_1 other stuff");
		assertThat(names).containsExactly(containerName("dir", "db", "1"), containerName("dir", "db2", "1"));
    }

    @Test
    public void ignore_an_empty_line_in_ps_output() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n\n");
		assertThat(names).containsExactly(containerName("dir", "db", "1"));
    }

    @Test
    public void ignore_a_line_with_ony_spaces_in_ps_output() {
        List<ContainerName> names = ContainerNames.parseFromDockerComposePs("\n----\ndir_db_1 other line contents\n   \n");
		assertThat(names).containsExactly(containerName("dir", "db", "1"));
    }

    private static ContainerName containerName(String project, String semantic, String number) {
        return ContainerName.builder()
                .rawName(project + "_" + semantic + "_" + number)
                .semanticName(semantic)
                .build();
    }
}
