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
package org.springframework.cloud.dataflow.common.test.docker.compose.execution;

import com.github.zafarkhaja.semver.Version;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DockerComposeVersionTests {

    @Test
    public void compare_major_versions_first() {
		assertThat(Version.parse("2.1.0").compareTo(Version.parse("1.2.1"))).isGreaterThan(0);
    }

    @Test
    public void compare_minor_versions_when_major_versions_are_the_same() {
		assertThat(Version.parse("2.1.7").compareTo(Version.parse("2.3.2"))).isLessThan(0);
    }

    @Test
    public void return_equals_for_the_same_version_strings() {
		assertThat(Version.parse("2.1.2").compareTo(Version.parse("2.1.2"))).isEqualTo(0);
    }

    @Test
    public void remove_non_digits_when_passing_version_string() {
		assertThat(DockerComposeVersion.parseFromDockerComposeVersion("docker-compose version 1.7.0rc1, build 1ad8866")).isEqualTo(Version.parse("1.7.0"));
    }
	public void check_for_docker_version() {
		assertThat(DockerComposeVersion.parseFromDockerComposeVersion("Docker version 26.1.1, build 1ad8866")).isEqualTo(Version.parse("26.1.1"));
	}
}
