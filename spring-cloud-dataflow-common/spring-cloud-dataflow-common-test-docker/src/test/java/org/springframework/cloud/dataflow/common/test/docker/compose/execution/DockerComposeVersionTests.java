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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;


public class DockerComposeVersionTests {

    @Test
    public void compare_major_versions_first() {
        assertThat(Version.valueOf("2.1.0").compareTo(Version.valueOf("1.2.1")), greaterThan(0));
    }

    @Test
    public void compare_minor_versions_when_major_versions_are_the_same() {
        assertThat(Version.valueOf("2.1.7").compareTo(Version.valueOf("2.3.2")), lessThan(0));
    }

    @Test
    public void return_equals_for_the_same_version_strings() {
        assertThat(Version.valueOf("2.1.2").compareTo(Version.valueOf("2.1.2")), is(0));
    }

    @Test
    public void remove_non_digits_when_passing_version_string() {
        assertThat(
                DockerComposeVersion.parseFromDockerComposeVersion("docker-compose version 1.7.0rc1, build 1ad8866"),
                is(Version.valueOf("1.7.0")));
    }
	public void check_for_docker_version() {
		assertThat(
			DockerComposeVersion.parseFromDockerComposeVersion("Docker version 26.1.1, build 1ad8866"),
			is(Version.valueOf("26.1.1")));
	}
}
