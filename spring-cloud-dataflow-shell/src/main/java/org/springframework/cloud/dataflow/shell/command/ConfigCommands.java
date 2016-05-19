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
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Configuration commands for the Shell.  The default Data Flow Server location is
 * <code>http://localhost:9393</code>
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
	private DataFlowShell shell;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${dataflow.uri:" + DEFAULT_TARGET + "}")
	private String serverUri;

	@Value("${dataflow.user:#{null}")
	private String user;

	@Value("${dataflow.password:#{null}")
	private String password;

	public static final String DEFAULT_TARGET = "http://localhost:9393/";

	@CliCommand(value = {"dataflow config server"}, help = "Configure the Spring Cloud Data Flow REST server to use")
	public String target(
			@CliOption(mandatory = false, key = {"", "uri"},
					help = "the location of the Spring Cloud Data Flow REST endpoint",
					unspecifiedDefaultValue = DEFAULT_TARGET) String targetUriString) {
		try {
			URI baseURI = URI.create(targetUriString);
			establishConverters(this.restTemplate);
			this.shell.setDataFlowOperations(new DataFlowTemplate(baseURI, this.restTemplate));
			return(String.format("Successfully targeted %s", targetUriString));
		}
		catch (Exception e) {
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

	@Override
	public void afterPropertiesSet() throws Exception {
		target(new URI(serverUri).toString());
	}

	private void establishConverters(RestTemplate restTemplate){
		for(HttpMessageConverter converter : restTemplate.getMessageConverters()) {
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
