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

import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class PortsTests {

	private static final String LOCALHOST_IP = "127.0.0.1";

	@Test
	public void result_in_no_ports_when_there_are_no_ports_in_ps_output() {
		String psOutput = "------";
		Ports ports = Ports.parseFromDockerComposePs(psOutput, null);
		Ports expected = new Ports(emptyList());
		assertThat(ports).isEqualTo(expected);
	}

	@Test
	public void result_in_single_port_when_there_is_single_tcp_port_mapping() {
		String psOutput = "0.0.0.0:5432->5432/tcp";
		Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
		Ports expected = new Ports(Arrays.asList(new DockerPort(LOCALHOST_IP, 5432, 5432)));
		assertThat(ports).isEqualTo(expected);
	}

	@Test
	public void
			result_in_single_port_with_ip_other_than_localhost_when_there_is_single_tcp_port_mapping() {
		String psOutput = "10.0.1.2:1234->2345/tcp";
		Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
		Ports expected = new Ports(Arrays.asList(new DockerPort("10.0.1.2", 1234, 2345)));
		assertThat(ports).isEqualTo(expected);
	}

	@Test
	public void result_in_two_ports_when_there_are_two_tcp_port_mappings() {
		String psOutput = "0.0.0.0:5432->5432/tcp, 0.0.0.0:5433->5432/tcp";
		Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
		Ports expected = new Ports(Arrays.asList(new DockerPort(LOCALHOST_IP, 5432, 5432),
												new DockerPort(LOCALHOST_IP, 5433, 5432)));
		assertThat(ports).isEqualTo(expected);
	}

	@Test
	public void result_in_no_ports_when_there_is_a_non_mapped_exposed_port() {
		String psOutput = "5432/tcp";
		Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
		Ports expected = new Ports(emptyList());
		assertThat(ports).isEqualTo(expected);
	}

	@Test
	public void parse_actual_docker_compose_output() {
		String psOutput =
				  "       Name                      Command               State                                         Ports                                        \n"
				+ "-------------------------------------------------------------------------------------------------------------------------------------------------\n"
				+ "postgres_postgres_1   /bin/sh -c /usr/local/bin/ ...   Up      0.0.0.0:8880->8880/tcp, 8881/tcp, 8882/tcp, 8883/tcp, 8884/tcp, 8885/tcp, 8886/tcp \n"
				+ "";
		Ports ports = Ports.parseFromDockerComposePs(psOutput, LOCALHOST_IP);
		Ports expected = new Ports(Arrays.asList(new DockerPort(LOCALHOST_IP, 8880, 8880)));
		assertThat(ports).isEqualTo(expected);
	}

	@Test
	public void throw_illegal_state_exception_when_no_running_container_found_for_service() {
		assertThatThrownBy(() -> Ports.parseFromDockerComposePs("", ""),
				"Expected Ports.parseFromDockerComposePs to throw, but it didn't")
				.hasMessageContaining("No container found")
				.isInstanceOf(IllegalStateException.class);
	}
}
