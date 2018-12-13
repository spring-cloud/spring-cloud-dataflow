/*
 * Copyright 2018 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.dataflow.rest.client.DataFlowServerException;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.rest.resource.about.FeatureInfo;
import org.springframework.cloud.dataflow.rest.resource.about.RuntimeEnvironmentDetails;
import org.springframework.cloud.dataflow.rest.resource.about.SecurityInfo;
import org.springframework.cloud.dataflow.rest.resource.security.SecurityInfoResource;
import org.springframework.cloud.dataflow.rest.util.CheckableResource;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.rest.util.ProcessOutputResource;
import org.springframework.cloud.dataflow.rest.util.ResourceBasedAuthorizationInterceptor;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.TargetCredentials;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BorderSpecification;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.KeyValueHorizontalAligner;
import org.springframework.shell.table.KeyValueSizeConstraints;
import org.springframework.shell.table.KeyValueTextWrapper;
import org.springframework.shell.table.SimpleHorizontalAligner;
import org.springframework.shell.table.SimpleVerticalAligner;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.shell.table.Tables;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


/**
 * Configuration commands for the Shell. The default Data Flow Server location is
 * <code>http://localhost:9393</code>
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
@Configuration
public class ConfigCommands implements CommandMarker, InitializingBean, ApplicationListener<ApplicationReadyEvent>,
		ApplicationContextAware {

	public static final String HORIZONTAL_LINE = "-------------------------------------------------------------------------------\n";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private DataFlowShell shell;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${dataflow.uri:" + Target.DEFAULT_TARGET + "}")
	private String serverUri;

	@Value("${dataflow.username:" + Target.DEFAULT_USERNAME + "}")
	private String userName;

	@Value("${dataflow.password:" + Target.DEFAULT_SPECIFIED_PASSWORD + "}")
	private String password;

	@Value("${dataflow.skip-ssl-validation:" + Target.DEFAULT_UNSPECIFIED_SKIP_SSL_VALIDATION + "}")
	private boolean skipSslValidation;

	@Value("${dataflow.proxy.uri:" + Target.DEFAULT_PROXY_URI + "}")
	private String proxyUri;

	@Value("${dataflow.proxy.username:" + Target.DEFAULT_PROXY_USERNAME + "}")
	private String proxyUsername;

	@Value("${dataflow.proxy.password:" + Target.DEFAULT_PROXY_SPECIFIED_PASSWORD + "}")
	private String proxyPassword;

	@Value("${dataflow.credentials-provider-command:" + Target.DEFAULT_CREDENTIALS_PROVIDER_COMMAND + "}")
	private String credentialsProviderCommand;

	private UserInput userInput;

	private TargetHolder targetHolder;

	private ApplicationContext applicationContext;

	private volatile boolean initialized;

	@Autowired
	public void setUserInput(UserInput userInput) {
		this.userInput = userInput;
	}

	@Autowired
	public void setTargetHolder(TargetHolder targetHolder) {
		this.targetHolder = targetHolder;
	}

	// These should be ctor injection

	@Autowired
	public void setDataFlowShell(DataFlowShell shell) {
		this.shell = shell;
	}

	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Bean
	public RestTemplate restTemplate(Environment ev) {
		return DataFlowTemplate.getDefaultDataflowRestTemplate();
	}

	// This is for unit testing

	public void setServerUri(String serverUri) {
		this.serverUri = serverUri;
	}

	@CliCommand(value = { "dataflow config server" }, help = "Configure the Spring Cloud Data Flow REST server to use")
	public String target(
			@CliOption(mandatory = false, key = { "",
					"uri" }, help = "the location of the Spring Cloud Data Flow REST endpoint", unspecifiedDefaultValue = Target.DEFAULT_TARGET) String targetUriString,
			@CliOption(mandatory = false, key = {
					"username" }, help = "the username for authenticated access to the Admin REST endpoint", unspecifiedDefaultValue = Target.DEFAULT_USERNAME) String targetUsername,
			@CliOption(mandatory = false, key = {
					"password" }, help = "the password for authenticated access to the Admin REST endpoint (valid only with a "
					+ "username)", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_PASSWORD, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_PASSWORD) String targetPassword,
			@CliOption(mandatory = false, key = {
					"credentials-provider-command" }, help = "a command to run that outputs the HTTP credentials used for authentication", unspecifiedDefaultValue = Target.DEFAULT_CREDENTIALS_PROVIDER_COMMAND) String credentialsProviderCommand,
			@CliOption(mandatory = false, key = {
					"skip-ssl-validation" }, help = "accept any SSL certificate (even self-signed)", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_SKIP_SSL_VALIDATION, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_SKIP_SSL_VALIDATION) boolean skipSslValidation,

			@CliOption(mandatory = false, key = {
					"proxy-uri" }, help = "the uri of the proxy server", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_PASSWORD, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_PASSWORD) String proxyUri,
			@CliOption(mandatory = false, key = {
					"proxy-username" }, help = "the username for authenticated access to the secured proxy server", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_PASSWORD, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_PASSWORD) String proxyUsername,
			@CliOption(mandatory = false, key = {
					"proxy-password" }, help = "the password for authenticated access to the secured proxy server (valid only with a "
					+ "username)", specifiedDefaultValue = Target.DEFAULT_SPECIFIED_PASSWORD, unspecifiedDefaultValue = Target.DEFAULT_UNSPECIFIED_PASSWORD) String proxyPassword) {
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
			this.targetHolder.setTarget(new Target(targetUriString, targetUsername, targetPassword, skipSslValidation));

			final HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create(this.targetHolder.getTarget().getTargetUri())
					.skipTlsCertificateVerification(skipSslValidation);
			if (StringUtils.hasText(targetUsername) && StringUtils.hasText(targetPassword)) {
				httpClientConfigurer.basicAuthCredentials(targetUsername, targetPassword);
			}
			if (StringUtils.hasText(credentialsProviderCommand)) {
				this.targetHolder.getTarget().setTargetCredentials(new TargetCredentials(true));
				final CheckableResource credentialsResource = new ProcessOutputResource(credentialsProviderCommand.split("\\s+"));
				httpClientConfigurer.addInterceptor(new ResourceBasedAuthorizationInterceptor(credentialsResource));
			}
			if (StringUtils.hasText(proxyUri)) {
				if (StringUtils.isEmpty(proxyPassword) && !StringUtils.isEmpty(proxyUsername)) {
					// read password from the command line
					proxyPassword = userInput.prompt("Proxy Server Password", "", false);
				}
				httpClientConfigurer.withProxyCredentials(URI.create(proxyUri), proxyUsername, proxyPassword);
			}
			this.restTemplate.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());

			this.shell.setDataFlowOperations(
					new DataFlowTemplate(targetHolder.getTarget().getTargetUri(), this.restTemplate));
			this.targetHolder.getTarget()
					.setTargetResultMessage(String.format("Successfully targeted %s", targetUriString));

			final SecurityInfoResource securityInfoResource = restTemplate
					.getForObject(targetUriString + "/security/info", SecurityInfoResource.class);

			if (securityInfoResource.isAuthenticated()
					&& this.targetHolder.getTarget().getTargetCredentials() != null) {
				for (String roleAsString : securityInfoResource.getRoles()) {
					this.targetHolder.getTarget().getTargetCredentials().getRoles().add(RoleType.fromKey(roleAsString));
				}
			}

			this.targetHolder.getTarget().setAuthenticated(securityInfoResource.isAuthenticated());
			this.targetHolder.getTarget().setAuthenticationEnabled(securityInfoResource.isAuthenticationEnabled());
			this.targetHolder.getTarget().setTargetResultMessage(String.format("Successfully targeted %s", targetUriString));

		}
		catch (Exception e) {
			this.targetHolder.getTarget().setTargetException(e);
			this.shell.setDataFlowOperations(null);
			handleTargetException(this.targetHolder.getTarget());
		}
		return (this.targetHolder.getTarget().getTargetResultMessage());

	}

	@CliCommand(value = { "dataflow config info" }, help = "Show the Dataflow server being used")
	@SuppressWarnings("unchecked")
	public List<Object> info() {
		Target target = targetHolder.getTarget();
		if (target.getTargetException() != null) {
			handleTargetException(target);
			throw new DataFlowServerException(this.targetHolder.getTarget().getTargetResultMessage());
		}
		AboutResource about = this.shell.getDataFlowOperations().aboutOperation().get();

		List<Object> result = new ArrayList<>();
		int rowIndex = 0;
		List<Integer> rowsWithThinSeparators = new ArrayList<>();

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Target").addValue(target.getTargetUriAsString());
		rowIndex++;

		if (target.getTargetResultMessage() != null) {
			modelBuilder.addRow().addValue("Result").addValue(target.getTargetResultMessage());
			rowIndex++;
		}
		modelBuilder.addRow().addValue("Features").addValue(about.getFeatureInfo());
		rowIndex++;

		Map<String, String> versions = new LinkedHashMap<>();
		modelBuilder.addRow().addValue("Versions").addValue(versions);
		rowIndex++;
		versions.compute(about.getVersionInfo().getImplementation().getName(),
				(k, v) -> about.getVersionInfo().getImplementation().getVersion());
		versions.compute(about.getVersionInfo().getCore().getName(),
				(k, v) -> about.getVersionInfo().getCore().getVersion());
		versions.compute(about.getVersionInfo().getDashboard().getName(),
				(k, v) -> about.getVersionInfo().getDashboard().getVersion());
		versions.compute(about.getVersionInfo().getShell().getName(),
				(k, v) -> about.getVersionInfo().getShell().getVersion());

		SecurityInfo securityInfo = about.getSecurityInfo();
		modelBuilder.addRow().addValue("Security").addValue(securityInfo);
		rowIndex++;

		if (securityInfo.isAuthenticated()) {
			modelBuilder.addRow().addValue("Roles").addValue(securityInfo.getRoles());
			rowsWithThinSeparators.add(rowIndex++);
		}

		RuntimeEnvironmentDetails appDeployer = about.getRuntimeEnvironment().getAppDeployer();
		RuntimeEnvironmentDetails taskLauncher = about.getRuntimeEnvironment().getTaskLauncher();
		String deployerColumnName = "Skipper Deployer";
		modelBuilder.addRow().addValue(deployerColumnName).addValue(appDeployer);
		rowIndex++;
		if (!appDeployer.getPlatformSpecificInfo().isEmpty()) {
			modelBuilder.addRow().addValue("Platform Specific").addValue(appDeployer.getPlatformSpecificInfo());
			rowsWithThinSeparators.add(rowIndex++);
		}
		modelBuilder.addRow().addValue("Task Launcher").addValue(taskLauncher);
		rowIndex++;
		if (!taskLauncher.getPlatformSpecificInfo().isEmpty()) {
			modelBuilder.addRow().addValue("Platform Specific").addValue(taskLauncher.getPlatformSpecificInfo());
			rowsWithThinSeparators.add(rowIndex++);
		}

		TableBuilder builder = new TableBuilder(modelBuilder.build());
		builder.addOutlineBorder(BorderStyle.fancy_double)
				.paintBorder(BorderStyle.fancy_light, BorderSpecification.INNER).fromTopLeft().toBottomRight()
				.on(CellMatchers.table()).addAligner(SimpleHorizontalAligner.center).on(CellMatchers.table())
				.addAligner(SimpleVerticalAligner.middle);

		Tables.configureKeyValueRendering(builder, ": ");

		builder.on(CellMatchers.ofType(FeatureInfo.class)).addFormatter(new DataFlowTables.BeanWrapperFormatter(": "))
				.addAligner(new KeyValueHorizontalAligner(":")).addSizer(new KeyValueSizeConstraints(": "))
				.addWrapper(new KeyValueTextWrapper(": "));
		List<String> excludes = securityInfo.isAuthenticated() ? Arrays.asList("roles", "class")
				: Arrays.asList("roles", "class", "username");
		builder.on(CellMatchers.ofType(SecurityInfo.class))
				.addFormatter(new DataFlowTables.BeanWrapperFormatter(": ", null, excludes))
				.addAligner(new KeyValueHorizontalAligner(":")).addSizer(new KeyValueSizeConstraints(": "))
				.addWrapper(new KeyValueTextWrapper(": "));
		builder.on(CellMatchers.ofType(List.class))
				.addFormatter(value -> ((List<String>) value).toArray(new String[0]));
		builder.on(CellMatchers.ofType(RuntimeEnvironmentDetails.class))
				.addFormatter(new DataFlowTables.BeanWrapperFormatter(": ", null,
						Arrays.asList("class", "platformSpecificInfo")))
				.addAligner(new KeyValueHorizontalAligner(":")).addSizer(new KeyValueSizeConstraints(": "))
				.addWrapper(new KeyValueTextWrapper(": "));
		rowsWithThinSeparators.forEach(row -> builder.paintBorder(BorderStyle.fancy_light_quadruple_dash, BorderSpecification.TOP)
				.fromRowColumn(row, 0).toRowColumn(row + 1, builder.getModel().getColumnCount()));

		result.add(builder.build());

		if (Target.TargetStatus.ERROR.equals(target.getStatus())) {
			StringWriter stringWriter = new StringWriter();
			stringWriter.write("\nAn exception occurred during targeting:\n");
			target.getTargetException().printStackTrace(new PrintWriter(stringWriter));

			result.add(stringWriter.toString());
		}
		return result;
	}

	private void handleTargetException(Target target) {
		Exception targetException = target.getTargetException();
		Assert.isTrue(targetException != null, "TargetException must not be null");
		if (targetException instanceof DataFlowServerException) {
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
						.setTargetResultMessage(String.format("Unable to contact Data Flow " + "Server at '%s': '%s'.",
								target.getTargetUriAsString(), targetException.toString()));
			}
		}
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// Only invoke if the shell is executing in the same application context as the data flow server.
		if (!initialized) {
			target(
					this.serverUri,
					this.userName,
					this.password,
					this.credentialsProviderCommand,
					this.skipSslValidation,
					this.proxyUri,
					this.proxyUsername,
					this.proxyPassword
			);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Only invoke this lifecycle method if the shell is executing in stand-alone mode.
		if (applicationContext != null && !applicationContext.containsBean("streamDefinitionRepository")) {
			initialized = true;
			target(
					this.serverUri,
					this.userName,
					this.password,
					this.credentialsProviderCommand,
					this.skipSslValidation,
					this.proxyUri,
					this.proxyUsername,
					this.proxyPassword
			);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
