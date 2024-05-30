/*
 * Copyright 2017-2024 the original author or authors.
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.core.Base64Utils;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;

/**
 * FactoryBean that creates a Spring Batch Step that executes a configured
 * TaskLaunchTasklet.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Corneil du Plessis
 */
public class ComposedTaskRunnerStepFactory implements FactoryBean<Step> {

	private final static Logger logger = LoggerFactory.getLogger(ComposedTaskRunnerStepFactory.class);

	@Autowired
	private ComposedTaskProperties composedTaskProperties;

	private ComposedTaskProperties composedTaskPropertiesFromEnv;

	private final String taskName;

	private final String taskNameId;

	private Map<String, String> taskSpecificProps = new HashMap<>();

	private List<String> arguments = new ArrayList<>();

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private StepExecutionListener composedTaskStepExecutionListener;

	@Autowired
	private TaskExplorer taskExplorer;

	@Autowired
	private TaskProperties taskProperties;

	@Autowired(required = false)
	private ClientRegistrationRepository clientRegistrations;

	@Autowired(required = false)
	private OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient;

	@Autowired(required = false)
	private ObjectMapper mapper;

	@Autowired
	private Environment environment;

	public ComposedTaskRunnerStepFactory(
			ComposedTaskProperties composedTaskPropertiesFromEnv, String taskName, String taskNameId
	) {
		Assert.notNull(composedTaskPropertiesFromEnv,
				"composedTaskProperties must not be null");
		Assert.hasText(taskName, "taskName must not be empty nor null");

		this.composedTaskPropertiesFromEnv = composedTaskPropertiesFromEnv;
		this.taskName = taskName;
		this.taskNameId = taskNameId;
	}

	public void setTaskSpecificProps(Map<String, String> taskSpecificProps) {
		if (taskSpecificProps != null) {
			this.taskSpecificProps = taskSpecificProps;
		}
	}

	public void setArguments(List<String> arguments) {
		if (arguments != null) {
			this.arguments = arguments;
		}
	}

	@Override
	public Step getObject() {
		if (this.mapper == null) {
			this.mapper = new ObjectMapper();
			this.mapper.registerModule(new Jdk8Module());
			this.mapper.registerModule(new Jackson2HalModule());
			this.mapper.registerModule(new JavaTimeModule());
			this.mapper.registerModule(new Jackson2DataflowModule());
		}
		TaskLauncherTasklet taskLauncherTasklet = new TaskLauncherTasklet(
				this.clientRegistrations,
				this.clientCredentialsTokenResponseClient,
				this.taskExplorer,
				this.composedTaskPropertiesFromEnv,
				this.taskName,
				taskProperties,
				environment, this.mapper);

		List<String> argumentsFromAppProperties = Base64Utils.decodeMap(this.composedTaskProperties.getComposedTaskAppArguments())
				.entrySet()
				.stream()
				.filter(e -> e.getKey().startsWith("app." + taskNameId + ".") || e.getKey().startsWith("app.*."))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());

		List<String> argumentsToUse = Stream.concat(this.arguments.stream(), argumentsFromAppProperties.stream())
				.collect(Collectors.toList());

		taskLauncherTasklet.setArguments(argumentsToUse);

		logger.debug("decoded composed-task-app-properties {}", composedTaskProperties.getComposedTaskAppProperties());

		Map<String, String> propertiesFrom = Base64Utils
				.decodeMap(this.composedTaskProperties.getComposedTaskAppProperties()).entrySet().stream()
				.filter(e ->
					e.getKey().startsWith("app." + taskNameId + ".") ||
					e.getKey().startsWith("app.*.") ||
					e.getKey().startsWith("deployer." + taskNameId + ".") ||
					e.getKey().startsWith("deployer.*."))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		Map<String, String> propertiesToUse = new HashMap<>();
		propertiesToUse.putAll(this.taskSpecificProps);
		propertiesToUse.putAll(propertiesFrom);

		taskLauncherTasklet.setProperties(propertiesToUse);
		logger.debug("Properties to use {}", propertiesToUse);
		StepBuilder stepBuilder = new StepBuilder(this.taskName, this.jobRepository);
		return stepBuilder
				.tasklet(taskLauncherTasklet, this.transactionManager)
				.transactionAttribute(getTransactionAttribute())
				.listener(this.composedTaskStepExecutionListener)
				.build();
	}

	/**
	 * Using the default transaction attribute for the job will cause the
	 * TaskLauncher not to see the latest state in the database but rather
	 * what is in its transaction.  By setting isolation to READ_COMMITTED
	 * The task launcher can see latest state of the db.  Since the changes
	 * to the task execution are done by the tasks.
	 *
	 * @return DefaultTransactionAttribute with isolation set to READ_COMMITTED.
	 */
	private TransactionAttribute getTransactionAttribute() {
		DefaultTransactionAttribute defaultTransactionAttribute =
				new DefaultTransactionAttribute();
		defaultTransactionAttribute.setIsolationLevel(
				Isolation.READ_COMMITTED.value());
		return defaultTransactionAttribute;
	}

	@Override
	public Class<?> getObjectType() {
		return Step.class;
	}

}
