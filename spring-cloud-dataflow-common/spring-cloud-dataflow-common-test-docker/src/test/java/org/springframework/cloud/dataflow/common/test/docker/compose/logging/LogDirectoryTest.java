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
package org.springframework.cloud.dataflow.common.test.docker.compose.logging;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogDirectoryTest {

//    @Rule
//    public final EnvironmentVariables variablesRule = new EnvironmentVariables();

    @Test
    public void gradleDockerLogsDirectory_should_use_class_simple_name() {
        String directory = LogDirectory.gradleDockerLogsDirectory(SomeTestClass.class);
		assertThat(directory).isEqualTo("build/dockerLogs/SomeTestClass");
    }

//    @Test
//    public void circleAwareLogDirectory_should_match_gradleDockerLogsDirectory_by_default() {
//        variablesRule.set("CIRCLE_ARTIFACTS", null);
//        String directory = LogDirectory.circleAwareLogDirectory(SomeTestClass.class);
//        assertThat(directory, is("build/dockerLogs/SomeTestClass"));
//    }
//
//    @Test
//    public void circleAwareLogDirectory_should_use_circle_environment_variable_if_available() {
//        variablesRule.set("CIRCLE_ARTIFACTS", "/tmp/circle-artifacts.g4DjuuD");
//
//        String directory = LogDirectory.circleAwareLogDirectory(SomeTestClass.class);
//        assertThat(directory, is("/tmp/circle-artifacts.g4DjuuD/dockerLogs/SomeTestClass"));
//    }
//
//    @Test
//    public void circleAwareLogDirectory_should_append_logDirectoryName_to_path() {
//        variablesRule.set("CIRCLE_ARTIFACTS", "/tmp/circle-artifacts.123456");
//
//        String directory = LogDirectory.circleAwareLogDirectory("some-path");
//        assertThat(directory, is("/tmp/circle-artifacts.123456/dockerLogs/some-path"));
//    }

    private static class SomeTestClass {}
}
