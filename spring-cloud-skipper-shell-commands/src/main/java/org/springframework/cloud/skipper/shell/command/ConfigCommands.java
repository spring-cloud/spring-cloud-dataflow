/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.skipper.client.DefaultSkipperClient;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.client.SkipperClientProperties;
import org.springframework.cloud.skipper.client.SkipperServerException;
import org.springframework.cloud.skipper.client.util.HttpClientConfigurer;
import org.springframework.cloud.skipper.client.util.ProcessOutputResource;
import org.springframework.cloud.skipper.client.util.ResourceBasedAuthorizationInterceptor;
import org.springframework.cloud.skipper.domain.AboutInfo;
import org.springframework.cloud.skipper.shell.command.support.*;
import org.springframework.context.*;
import org.springframework.core.io.Resource;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.springframework.cloud.skipper.client.SkipperClientProperties.*;

/**
 * Configuration commands for the Shell. The default Skipper Server location is
 * <code>http://localhost:7577</code>
 *
 * @author Gunnar Hillert
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Mike Heath
 */
@Component
public class ConfigCommands implements CommandMarker, InitializingBean, ApplicationListener<ApplicationReadyEvent>,
		ApplicationEventPublisherAware,
		ApplicationContextAware {

	public static final String DEFAULT_UNSPECIFIED_SKIP_SSL_VALIDATION = "false";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private TargetHolder targetHolder;

	private SkipperClient skipperClient;

	private RestTemplate restTemplate;

	private SkipperClientProperties config;

	private ApplicationContext applicationContext;

	private ApplicationEventPublisher applicationEventPublisher;

	private volatile boolean initialized;

	private ConsoleUserInput userInput = new ConsoleUserInput();

	@Autowired
	public ConfigCommands(TargetHolder targetHolder, RestTemplate restTemplate,
			SkipperClientProperties config) {
		this.targetHolder = targetHolder;
		this.restTemplate = restTemplate;
		this.config = config;
	}

	// @formatter:off
	@CliCommand(value = {"skipper config server"}, help = "Configure the Spring Cloud Skipper REST server to use")
	public String target(
			@CliOption(mandatory = false, key = {"", "uri"},
					help = "the location of the Spring Cloud Skipper REST endpoint",
					unspecifiedDefaultValue = DEFAULT_TARGET) String targetUrlString,
			@CliOption(mandatory = false, key = {"username"},
					help = "the username for authenticated access to the Admin REST endpoint",
					unspecifiedDefaultValue = DEFAULT_USERNAME) String targetUsername,
			@CliOption(mandatory = false, key = {"password"},
					help = "the password for authenticated access to the Admin REST endpoint (valid only with a "
							+ "username)",
					specifiedDefaultValue = DEFAULT_PASSWORD) String targetPassword,
			@CliOption(mandatory = false, key = {"credentials-provider-command"},
					help = "a command to run that outputs the HTTP credentials used for authentication",
					unspecifiedDefaultValue = DEFAULT_CREDENTIALS_PROVIDER_COMMAND) String credentialsProviderCommand,
			@CliOption(mandatory = false, key = {"skip-ssl-validation"},
					help = "accept any SSL certificate (even self-signed)",
					specifiedDefaultValue = DEFAULT_SKIP_SSL_VALIDATION,
					unspecifiedDefaultValue = DEFAULT_UNSPECIFIED_SKIP_SSL_VALIDATION) boolean skipSslValidation) {
		// @formatter:on
		if (StringUtils.isEmpty(credentialsProviderCommand) &&
				!StringUtils.isEmpty(targetPassword) && StringUtils.isEmpty(targetUsername)) {
			return "A password may be specified only together with a username";
		}

		if (StringUtils.isEmpty(credentialsProviderCommand) &&
				StringUtils.isEmpty(targetPassword) && !StringUtils.isEmpty(targetUsername)) {
			// read password from the command line
			targetPassword = userInput.prompt("Password", "", false);
		}

		try {
			this.targetHolder.setTarget(new Target(targetUrlString, targetUsername, targetPassword, skipSslValidation));

			final HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create()
					.targetHost(this.targetHolder.getTarget().getTargetUri())
					.skipTlsCertificateVerification(skipSslValidation);
			if (StringUtils.hasText(targetUsername) && StringUtils.hasText(targetPassword)) {
				httpClientConfigurer.basicAuthCredentials(targetUsername, targetPassword);
			}
			if (StringUtils.hasText(credentialsProviderCommand)) {
				this.targetHolder.getTarget().setTargetCredentials(new TargetCredentials(true));
				final Resource credentialsResource = new ProcessOutputResource(
						credentialsProviderCommand.split("\\s+"));
				httpClientConfigurer.addInterceptor(new ResourceBasedAuthorizationInterceptor(credentialsResource));
			}
			this.restTemplate.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());

			this.skipperClient = new DefaultSkipperClient(targetHolder.getTarget().getTargetUri().toURL().toString(),
					this.restTemplate);

			// Other commands will be notified of the new SkipperClient object
			applicationEventPublisher.publishEvent(new SkipperClientUpdatedEvent(skipperClient));

			// TODO - note, we don't yet know if we can access the specified URI
			this.targetHolder.getTarget()
					.setTargetResultMessage(String.format("Successfully targeted %s", targetUrlString));

		}
		catch (Exception e) {
			this.targetHolder.getTarget().setTargetException(e);
			// TODO do we really want to pass around null?
			applicationEventPublisher.publishEvent(new SkipperClientUpdatedEvent(null));
			handleTargetException(this.targetHolder.getTarget());
		}
		return (this.targetHolder.getTarget().getTargetResultMessage());
	}

	@CliCommand(value = { "skipper config info" }, help = "Show the Skipper server being used")
	public AboutInfo info() {
		Target target = targetHolder.getTarget();
		if (target.getTargetException() != null) {
			handleTargetException(target);
			throw new SkipperServerException(this.targetHolder.getTarget().getTargetResultMessage());
		}
		return this.skipperClient.getAboutInfo();
	}

	private void handleTargetException(Target target) {
		Exception targetException = target.getTargetException();
		Assert.isTrue(targetException != null, "TargetException must not be null");
		if (targetException instanceof SkipperServerException) {
			String message = String.format("Unable to parse server response: %s - at URI '%s'.",
					targetException.getMessage(), target.getTargetUriAsString());
			if (logger.isDebugEnabled()) {
				logger.debug(message, targetException);
			}
			else {
				logger.warn(message);
			}
			this.targetHolder.getTarget().setTargetResultMessage(message);
		}
		else {
			if (targetException instanceof HttpClientErrorException && targetException.getMessage().startsWith("401")) {
				this.targetHolder.getTarget()
						.setTargetResultMessage(String.format(
								"Unable to access Data Flow Server"
										+ " at '%s': '%s'. Unauthorized, did you forget to authenticate?",
								target.getTargetUriAsString(), targetException.toString()));
			}
			else {
				this.targetHolder.getTarget()
						.setTargetResultMessage(String.format("Unable to contact Skipper Server at '%s': '%s'.",
								target.getTargetUriAsString(), targetException.toString()));
			}
		}
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// Only invoke if the shell is executing in the same application context as the
		// data flow server.
		if (!initialized) {
			callTarget();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Only invoke this lifecycle method if the shell is executing in stand-alone
		// mode.
		if (applicationContext != null && !applicationContext.containsBean("streamDefinitionRepository")) {
			initialized = true;
			callTarget();
		}
	}

	protected void callTarget() {
		target(this.config.getServerUrl(), this.config.getUsername(),
				this.config.getPassword(), this.config.getCredentialsProviderCommand(),
				this.config.isSkipSllValidation());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
