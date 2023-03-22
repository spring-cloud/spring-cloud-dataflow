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

import java.io.File;
import java.io.IOException;

import org.springframework.util.FileCopyUtils;

/**
 * Check whether Mac OS X users have pointed localunixsocket to localhost.
 *
 * <p>docker-compose takes an order of magnitude longer to run commands without this tip!
 *
 * @see <a href="https://github.com/docker/compose/issues/3419#issuecomment-221793401">Docker Compose Issue #3419</a>
 */
public class DockerForMacHostsIssue {

	private static final String REDIRECT_LINE = "127.0.0.1 localunixsocket\n";
	private static final String WARNING_MESSAGE = "\n\n **** WARNING: Your tests may be slow ****\n"
			+ "Please add the following line to /etc/hosts:\n    "
			+ REDIRECT_LINE
			+ "\nFor more information, see https://github.com/docker/compose/issues/3419#issuecomment-221793401\n\n";
	private static volatile boolean checkPerformed = false;

	@SuppressWarnings("checkstyle:BanSystemErr")
	public static void issueWarning() {
		if (!checkPerformed) {
			if (onMacOsX() && !localunixsocketRedirectedInEtcHosts()) {
				System.err.print(WARNING_MESSAGE);
			}
		}
		checkPerformed = true;
	}

	private static boolean onMacOsX() {
		return System.getProperty("os.name", "generic").equals("Mac OS X");
	}

	private static boolean localunixsocketRedirectedInEtcHosts() {
		try {
			byte[] bytes = FileCopyUtils.copyToByteArray(new File("/etc/hosts"));
			String content = new String(bytes);
			return content.contains(REDIRECT_LINE);
		} catch (IOException e) {
			return true;  // Better to be silent than issue false warnings
		}
	}

	public static void main(String[] args) {
		issueWarning();
	}

	private DockerForMacHostsIssue() {}
}
