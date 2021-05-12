/*
 * Copyright 2017-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.cloud.common.security.core.support.OAuth2AccessTokenProvidingClientHttpRequestInterceptor;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.support.TaskExecutionTimeoutException;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Executes task launch request using Spring Cloud Data Flow's Restful API
 * then returns the execution id once the task launched.
 *
 * Note: This class is not thread-safe and as such should not be used as a singleton.
 *
 * @author Glenn Renfro
 */
public class TaskLauncherTasklet implements Tasklet {

	private ComposedTaskProperties composedTaskProperties;

	private TaskExplorer taskExplorer;

	private Map<String, String> properties;

	private List<String> arguments;

	private String taskName;

	private static final Log logger = LogFactory.getLog(org.springframework.cloud.dataflow.composedtaskrunner.TaskLauncherTasklet.class);

	private Long executionId;

	private long timeout;

	private ClientRegistrationRepository clientRegistrations;

	private OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient;

	private TaskOperations taskOperations;

	TaskProperties taskProperties;


	public TaskLauncherTasklet(
			ClientRegistrationRepository clientRegistrations,
			OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient,
			TaskExplorer taskExplorer,
			ComposedTaskProperties composedTaskProperties, String taskName,
			TaskProperties taskProperties) {
		Assert.hasText(taskName, "taskName must not be empty nor null.");
		Assert.notNull(taskExplorer, "taskExplorer must not be null.");
		Assert.notNull(composedTaskProperties,
				"composedTaskProperties must not be null");

		this.taskName = taskName;
		this.taskExplorer = taskExplorer;
		this.composedTaskProperties = composedTaskProperties;
		this.taskProperties = taskProperties;
		this.clientRegistrations = clientRegistrations;
		this.clientCredentialsTokenResponseClient = clientCredentialsTokenResponseClient;
	}

	public void setProperties(Map<String, String> properties) {
		if(properties != null) {
			this.properties = properties;
		}
		else {
			this.properties = new HashMap<>(0);
		}
	}

	public void setArguments(List<String> arguments) {
		if(arguments != null) {
			this.arguments = arguments;
		}
		else {
			this.arguments = new ArrayList<>(0);
		}
	}

	/**
	 * Executes the task as specified by the taskName with the associated
	 * properties and arguments.
	 *
	 * @param contribution mutable state to be passed back to update the current step execution
	 * @param chunkContext contains the task-execution-id used by the listener.
	 * @return Repeat status of FINISHED.
	 */
	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) {
		TaskOperations taskOperations = taskOperations();
		if (this.executionId == null) {
			this.timeout = System.currentTimeMillis() +
					this.composedTaskProperties.getMaxWaitTime();
			logger.debug("Wait time for this task to complete is " +
					this.composedTaskProperties.getMaxWaitTime());
			logger.debug("Interval check time for this task to complete is " +
					this.composedTaskProperties.getIntervalTimeBetweenChecks());

			String tmpTaskName = this.taskName.substring(0,
					this.taskName.lastIndexOf('_'));

			List<String> args = this.arguments;

			ExecutionContext stepExecutionContext = chunkContext.getStepContext().getStepExecution().
					getExecutionContext();
			if (stepExecutionContext.containsKey("task-arguments")) {
				args = (List<String>) stepExecutionContext.get("task-arguments");
			}
			List<String> cleansedArgs = new ArrayList<>();
			if(args != null) {
				for(String argument : args) {
					if(!argument.startsWith("--spring.cloud.task.parent-execution-id=")) {
						cleansedArgs.add(argument);
					}
				}
				args = cleansedArgs;
			}
			if(this.taskProperties.getExecutionid() != null) {
				args.add("--spring.cloud.task.parent-execution-id=" + this.taskProperties.getExecutionid());
			}
			if(StringUtils.hasText(this.composedTaskProperties.getPlatformName())) {
				properties.put("spring.cloud.dataflow.task.platformName", this.composedTaskProperties.getPlatformName());
			}
			this.executionId = taskOperations.launch(tmpTaskName,
					this.properties, args);

			stepExecutionContext.put("task-execution-id", executionId);
			stepExecutionContext.put("task-arguments", args);
		}
		else {
			try {
				Thread.sleep(this.composedTaskProperties.getIntervalTimeBetweenChecks());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e.getMessage(), e);
			}

			TaskExecution taskExecution =
					this.taskExplorer.getTaskExecution(this.executionId);
			if (taskExecution != null && taskExecution.getEndTime() != null) {
				if (taskExecution.getExitCode() == null) {
					throw new UnexpectedJobExecutionException("Task returned a null exit code.");
				}
				else if (taskExecution.getExitCode() != 0) {
					throw new UnexpectedJobExecutionException("Task returned a non zero exit code.");
				}
				else {
					return RepeatStatus.FINISHED;
				}
			}
			if (this.composedTaskProperties.getMaxWaitTime() > 0 &&
					System.currentTimeMillis() > timeout) {
				throw new TaskExecutionTimeoutException(String.format(
						"Timeout occurred while processing task with Execution Id %s",
						this.executionId));
			}
		}
		return RepeatStatus.CONTINUABLE;
	}

	public TaskOperations taskOperations() {
		if(this.taskOperations == null) {
			this.taskOperations = dataFlowOperations().taskOperations();
		}
		return this.taskOperations;
	}

	/**
	 * @return new instance of DataFlowOperations
	 */
	private DataFlowOperations dataFlowOperations() {

		final RestTemplate restTemplate = DataFlowTemplate.getDefaultDataflowRestTemplate();

		validateUsernamePassword(this.composedTaskProperties.getDataflowServerUsername(), this.composedTaskProperties.getDataflowServerPassword());
		HttpClientConfigurer clientHttpRequestFactoryBuilder = null;

		if (this.composedTaskProperties.getOauth2ClientCredentialsClientId() != null
				|| StringUtils.hasText(this.composedTaskProperties.getDataflowServerAccessToken())
				|| (StringUtils.hasText(this.composedTaskProperties.getDataflowServerUsername())
				&& StringUtils.hasText(this.composedTaskProperties.getDataflowServerPassword()))) {
			clientHttpRequestFactoryBuilder = HttpClientConfigurer.create(this.composedTaskProperties.getDataflowServerUri());
		}

		String accessTokenValue = null;

		if (this.composedTaskProperties.getOauth2ClientCredentialsClientId() != null) {
			final ClientRegistration clientRegistration = this.clientRegistrations.findByRegistrationId("default");
			final OAuth2ClientCredentialsGrantRequest grantRequest = new OAuth2ClientCredentialsGrantRequest(clientRegistration);
			final OAuth2AccessTokenResponse res = this.clientCredentialsTokenResponseClient.getTokenResponse(grantRequest);
			accessTokenValue = res.getAccessToken().getTokenValue();
			logger.debug("Configured OAuth2 Client Credentials for accessing the Data Flow Server");
		}
		else if (StringUtils.hasText(this.composedTaskProperties.getDataflowServerAccessToken())) {
			accessTokenValue = this.composedTaskProperties.getDataflowServerAccessToken();
			logger.debug("Configured OAuth2 Access Token for accessing the Data Flow Server");
		}
		else if (StringUtils.hasText(this.composedTaskProperties.getDataflowServerUsername())
				&& StringUtils.hasText(this.composedTaskProperties.getDataflowServerPassword())) {
			accessTokenValue = null;
			clientHttpRequestFactoryBuilder.basicAuthCredentials(composedTaskProperties.getDataflowServerUsername(), composedTaskProperties.getDataflowServerPassword());
			logger.debug("Configured basic security for accessing the Data Flow Server");
		}
		else {
			logger.debug("Not configuring basic security for accessing the Data Flow Server");
		}

		if (accessTokenValue != null) {
			restTemplate.getInterceptors().add(new OAuth2AccessTokenProvidingClientHttpRequestInterceptor(accessTokenValue));
		}

		if (this.composedTaskProperties.isSkipTlsCertificateVerification()) {
			if (clientHttpRequestFactoryBuilder == null) {
				clientHttpRequestFactoryBuilder = HttpClientConfigurer.create(this.composedTaskProperties.getDataflowServerUri());
			}
			clientHttpRequestFactoryBuilder.skipTlsCertificateVerification();
		}

		if (clientHttpRequestFactoryBuilder != null) {
			restTemplate.setRequestFactory(clientHttpRequestFactoryBuilder.buildClientHttpRequestFactory());
		}

		return new DataFlowTemplate(this.composedTaskProperties.getDataflowServerUri(), restTemplate);
	}

	private void validateUsernamePassword(String userName, String password) {
		if (StringUtils.hasText(password) && !StringUtils.hasText(userName)) {
			throw new IllegalArgumentException("A password may be specified only together with a username");
		}
		if (!StringUtils.hasText(password) && StringUtils.hasText(userName)) {
			throw new IllegalArgumentException("A username may be specified only together with a password");
		}
	}
}
