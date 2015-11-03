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

package org.springframework.cloud.dataflow.shell.command;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.DataFlowServerException;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.VndErrorResponseErrorHandler;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration commands for the Shell.
 *
 * @author Gunnar Hillert
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 */
@Component
@Configuration
@EnableHypermediaSupport(type = HypermediaType.HAL)
public class ConfigCommands implements CommandMarker, InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CommandLine commandLine;

	@Autowired
	private DataFlowShell shell;

	@Autowired
	private RestTemplate restTemplate;

	public static final String DEFAULT_SCHEME = "http";

	public static final String DEFAULT_HOST = "localhost";

	public static final int DEFAULT_PORT = 9393;

	public static final String DEFAULT_TARGET = DEFAULT_SCHEME + "://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/";

	@CliCommand(value = {"admin config server"}, help = "Configure the Spring Cloud Data Flow REST server to use")
	public String target(
			@CliOption(mandatory = false, key = {"", "uri"},
					help = "the location of the Spring Cloud Data Flow REST endpoint",
					unspecifiedDefaultValue = DEFAULT_TARGET) String targetUriString) {
		try {
			URI baseURI = URI.create(targetUriString);
			this.shell.setDataFlowOperations(new DataFlowTemplate(baseURI, this.restTemplate));
			return(String.format("Successfully targeted %s", targetUriString));
		}
		catch (Exception e) {
			this.shell.setDataFlowOperations(null);
			if (e instanceof DataFlowServerException) {
				String message = String.format("Unable to parse admin response: %s - at URI '%s'.", e.getMessage(),
						targetUriString);
				if (logger.isDebugEnabled()) {
					logger.debug(message, e);
				}
				else {
					logger.warn(message);
				}
				return message;
			}
			else {
				return(String.format("Unable to contact Data Flow Admin at '%s': '%s'.",
								targetUriString, e.toString()));
			}
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

	@Bean
	public static RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new VndErrorResponseErrorHandler(restTemplate.getMessageConverters()));
		return restTemplate;
	}

}
