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
package org.springframework.cloud.dataflow.common.test.docker.compose.connection.waiting;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionsTests {
    @Test
    public void print_out_a_condensed_version_of_the_stacktrace() {
        RuntimeException exception = new RuntimeException("foo", new IllegalStateException("bar", new UnsupportedOperationException("baz")));
		assertThat(Exceptions.condensedStacktraceFor(exception)).isEqualTo("java.lang.RuntimeException: foo\n"
				+ "java.lang.IllegalStateException: bar\n"
				+ "java.lang.UnsupportedOperationException: baz");
    }
}
