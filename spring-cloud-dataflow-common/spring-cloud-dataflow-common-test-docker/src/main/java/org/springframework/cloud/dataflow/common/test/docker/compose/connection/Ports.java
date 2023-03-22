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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class Ports {

	private static final Pattern PORT_PATTERN = Pattern.compile("((\\d+).(\\d+).(\\d+).(\\d+)):(\\d+)->(\\d+)/tcp");
	private static final int IP_ADDRESS = 1;
	private static final int EXTERNAL_PORT = 6;
	private static final int INTERNAL_PORT = 7;

	private static final String NO_IP_ADDRESS = "0.0.0.0";

	private final List<DockerPort> ports;

	public Ports(List<DockerPort> ports) {
		this.ports = ports;
	}

	public Ports(DockerPort port) {
		this(Collections.singletonList(port));
	}

	public Stream<DockerPort> stream() {
		return ports.stream();
	}

	public static Ports parseFromDockerComposePs(String psOutput, String dockerMachineIp) {
		Assert.state(StringUtils.hasText(psOutput), "No container found");
		Matcher matcher = PORT_PATTERN.matcher(psOutput);
		List<DockerPort> ports = new ArrayList<>();
		while (matcher.find()) {
			String matchedIpAddress = matcher.group(IP_ADDRESS);
			String ip = matchedIpAddress.equals(NO_IP_ADDRESS) ? dockerMachineIp : matchedIpAddress;
			int externalPort = Integer.parseInt(matcher.group(EXTERNAL_PORT));
			int internalPort = Integer.parseInt(matcher.group(INTERNAL_PORT));

			ports.add(new DockerPort(ip, externalPort, internalPort));
		}
		return new Ports(ports);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ports);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Ports other = (Ports) obj;
		return Objects.equals(ports, other.ports);
	}

	@Override
	public String toString() {
		return "Ports [ports=" + ports + "]";
	}

}
