/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.shell.command;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.data.rest.client.CloudDataTemplate;
import org.springframework.cloud.data.shell.config.CloudDataShell;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

/**
 * Configuration commands for the Shell.
 *
 * @author Gunnar Hillert
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 */
@Component
public class ConfigCommands implements CommandMarker, InitializingBean {

	@Autowired
	private CommandLine commandLine;

	@Autowired
	private CloudDataShell shell;

	public static final String DEFAULT_SCHEME = "http";

	public static final String DEFAULT_HOST = "localhost";

	public static final int DEFAULT_PORT = 9393;

	public static final String DEFAULT_TARGET = DEFAULT_SCHEME + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/";

	@CliCommand(value = {"admin config server"}, help = "Configure the spring cloud data REST server to use")
	public String target(
			@CliOption(mandatory = false, key = {"", "uri"},
					help = "the location of the Spring Cloud data REST endpoint",
					unspecifiedDefaultValue = DEFAULT_TARGET) String targetUriString) {
		try {
			URI baseURI = URI.create(targetUriString);
			this.shell.setCloudDataOperations(new CloudDataTemplate(baseURI));
			return(String.format("Successfully targeted %s", targetUriString));
		}
		catch (Exception e) {
			this.shell.setCloudDataOperations(null);
			e.printStackTrace();
			return(String.format("Unable to contact Cloud data main at '%s'.",
							targetUriString));
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		target(getDefaultUri().toString());
	}

	private URI getDefaultUri() throws URISyntaxException {

		int port = DEFAULT_PORT;
		String host = DEFAULT_HOST;

		if (commandLine.getArgs() != null) {
			String[] args = commandLine.getArgs();
			int i = 0;
			while (i < args.length) {
				String arg = args[i++];
				if (arg.equals("--host")) {
					host = args[i++];
				}
				else if (arg.equals("--port")) {
					port = Integer.valueOf(args[i++]);
				}
				else {
					i--;
					break;
				}
			}
		}
		return new URI(DEFAULT_SCHEME, null, host, port, null, null, null);
	}

}
