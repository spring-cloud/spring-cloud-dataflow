/*
 * Copyright 2014-2016 the original author or authors.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.DataFlowServerException;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.VndErrorResponseErrorHandler;
import org.springframework.cloud.dataflow.rest.client.support.ExecutionContextJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.ExitStatusJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.JobExecutionJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.JobInstanceJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.JobParameterJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.JobParametersJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.StepExecutionHistoryJacksonMixIn;
import org.springframework.cloud.dataflow.rest.client.support.StepExecutionJacksonMixIn;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;
import org.springframework.cloud.dataflow.shell.ShellConfiguration;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.Target.TargetStatus;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.shell.CommandLine;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

	public static final String HORIZONTAL_LINE = "-------------------------------------------------------------------------------\n";

	private CommandLine commandLine;

	private RestTemplate restTemplate;

	private DataFlowShell shell;

	private ShellConfiguration shellConfiguration;

	private UserInput userInput;

	@Autowired
	public void setCommandLine(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Autowired
	public void setShell(DataFlowShell shell) {
		this.shell = shell;
	}

	@Autowired
	public void setShellConfiguration(ShellConfiguration shellConfiguration) {
		this.shellConfiguration = shellConfiguration;
	}

	@Autowired
	public void setUserInput(UserInput userInput) {
		this.userInput = userInput;
	}

	@CliCommand(value = {"dataflow config server"}, help = "Configure the Spring Cloud Data Flow REST server to use")
	public String target(
			@CliOption(mandatory = false, key = {"", "uri"},
					help = "the location of the Spring Cloud Data Flow REST endpoint",
					unspecifiedDefaultValue = Target.DEFAULT_TARGET) String targetUriString,
			@CliOption(mandatory = false, key = {"username"},
					help = "the username for authenticated access to the Admin REST endpoint",
					unspecifiedDefaultValue = Target.DEFAULT_USERNAME) String targetUsername,
			@CliOption(mandatory = false, key = {"password"},
					help = "the password for authenticated access to the Admin REST endpoint (valid only with a username)",
					specifiedDefaultValue = Target.DEFAULT_SPECIFIED_PASSWORD,
					unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_PASSWORD) String targetPassword) {


		if (!StringUtils.isEmpty(targetPassword) && StringUtils.isEmpty(targetUsername)) {
			return "A password may be specified only together with a username";
		}
		if (StringUtils.isEmpty(targetPassword) && !StringUtils.isEmpty(targetUsername)) {
			// read password from the command line
			targetPassword = userInput.prompt("Password", "", false);
		}

		this.shellConfiguration.setTarget(new Target(targetUriString, targetUsername, targetPassword));

		establishConverters(this.restTemplate);

		try {
			if (StringUtils.hasText(targetUsername) && StringUtils.hasText(targetPassword)) {
				final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				credentialsProvider.setCredentials(AuthScope.ANY,
						new UsernamePasswordCredentials(
								shellConfiguration.getTarget().getTargetCredentials().getUsername(),
								shellConfiguration.getTarget().getTargetCredentials().getPassword()));
				final CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
				final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

				this.restTemplate.setRequestFactory(requestFactory);
			}

			this.shell.setDataFlowOperations(new DataFlowTemplate(shellConfiguration.getTarget().getTargetUri(), this.restTemplate));
			return(String.format("Successfully targeted %s", targetUriString));
		}
		catch (Exception e) {
			this.shellConfiguration.getTarget().setTargetException(e);
			this.shell.setDataFlowOperations(null);
			if (e instanceof DataFlowServerException) {
				String message = String.format("Unable to parse server response: %s - at URI '%s'.", e.getMessage(),
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
				return(String.format("Unable to contact Data Flow Server at '%s': '%s'.",
								targetUriString, e.toString()));
			}
		}
	}

	@CliCommand(value = {"dataflow config info"}, help = "Show the Dataflow server being used")
	public String info() {

		final Map<String, String> statusValues = new TreeMap<String, String>();

		final Target target = shellConfiguration.getTarget();

		statusValues.put("Target", target.getTargetUriAsString());
		if (target.getTargetCredentials() != null) {
			statusValues.put("Credentials", target.getTargetCredentials().getDisplayableContents());
		}
		statusValues.put("Result", target.getTargetResultMessage() != null ? target.getTargetResultMessage() : "");

		final TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();

		for (Map.Entry<String, String> row : statusValues.entrySet()) {
			modelBuilder.addRow().addValue(row.getKey()).addValue(row.getValue());
		}

		final TableBuilder builder = new TableBuilder(modelBuilder.build());
		DataFlowTables.applyStyle(builder);

		final StringBuilder sb = new StringBuilder(builder.build().render(66));

		if (TargetStatus.ERROR.equals(target.getStatus())) {
			sb.append(HORIZONTAL_LINE);
			sb.append("An exception ocurred during targeting:\n");

			final StringWriter stringWriter = new StringWriter();
			target.getTargetException().printStackTrace(new PrintWriter(stringWriter));

			sb.append(stringWriter.toString());
		}
		return sb.toString();
	}

	/**
	 * Initialize the default {@link Target} for the Dataflow Server. It will use
	 * the constants {@link Target#DEFAULT_HOST}, {@link Target#DEFAULT_PORT}
	 * and {@link Target#DEFAULT_SCHEME}.
	 *
	 * Alternatively, the host and port can also be set using the {@code --host}
	 * and {@code --port} command line parameters.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		target(getDefaultUri().toString(), getDefaultUsername(), getDefaultPassword());
	}

	private URI getDefaultUri() throws URISyntaxException {

		int port = Target.DEFAULT_PORT;
		String host = Target.DEFAULT_HOST;

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
		return new URI(Target.DEFAULT_SCHEME, null, host, port, null, null, null);
	}

	private String getDefaultUsername() {
		int indexOfUserParameter = ArrayUtils.indexOf(commandLine.getArgs(), "--username");
		// if '--username' exists and it is not the last in the list of arguments, the next argument is the password
		if (indexOfUserParameter >= 0 && indexOfUserParameter < commandLine.getArgs().length - 1) {
			return commandLine.getArgs()[indexOfUserParameter + 1];
		}
		else {
			return Target.DEFAULT_USERNAME;
		}
	}

	private String getDefaultPassword() {
		int indexOfPasswordParameter = ArrayUtils.indexOf(commandLine.getArgs(), "--password");
		// if '--password' exists and it is not the last in the list of arguments, the next argument is the password
		if (indexOfPasswordParameter >= 0) {
			if (indexOfPasswordParameter < commandLine.getArgs().length - 1) {
				return commandLine.getArgs()[indexOfPasswordParameter + 1];
			}
			else {
				return Target.DEFAULT_SPECIFIED_PASSWORD;
			}
		}
		else {
			return Target.DEFAULT_SPECIFIED_PASSWORD;
		}
	}
	private void establishConverters(RestTemplate restTemplate){
		for(HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				final MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
				jacksonConverter.getObjectMapper().registerModule(new Jackson2HalModule());
				jacksonConverter.getObjectMapper().addMixIn(JobExecution.class, JobExecutionJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(JobParameters.class, JobParametersJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(JobParameter.class, JobParameterJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(JobInstance.class, JobInstanceJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(ExitStatus.class, ExitStatusJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(StepExecution.class, StepExecutionJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(ExecutionContext.class, ExecutionContextJacksonMixIn.class);
				jacksonConverter.getObjectMapper().addMixIn(StepExecutionHistory.class, StepExecutionHistoryJacksonMixIn.class);
				jacksonConverter.getObjectMapper().registerModule(new Jackson2HalModule());
			}
		}
	}

	@Bean
	public static RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new VndErrorResponseErrorHandler(restTemplate.getMessageConverters()));
		return restTemplate;
	}

}
