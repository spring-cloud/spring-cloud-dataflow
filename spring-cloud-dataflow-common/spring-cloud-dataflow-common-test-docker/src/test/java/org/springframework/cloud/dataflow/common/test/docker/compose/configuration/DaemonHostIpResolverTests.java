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
package org.springframework.cloud.dataflow.common.test.docker.compose.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DaemonHostIpResolver.LOCALHOST;

import org.junit.Test;
import org.springframework.cloud.dataflow.common.test.docker.compose.configuration.DaemonHostIpResolver;

public class DaemonHostIpResolverTests {

    @Test
    public void return_local_host_with_null() {
		assertThat(new DaemonHostIpResolver().resolveIp(null)).isEqualTo(LOCALHOST);
    }

    @Test
    public void return_local_host_with_blank() {
		assertThat(new DaemonHostIpResolver().resolveIp("")).isEqualTo(LOCALHOST);
    }

    @Test
    public void return_local_host_with_arbitrary() {
		assertThat(new DaemonHostIpResolver().resolveIp("arbitrary")).isEqualTo(LOCALHOST);
    }

}
