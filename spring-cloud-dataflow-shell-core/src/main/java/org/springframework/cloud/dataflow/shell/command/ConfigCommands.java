/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.cloud.dataflow.rest.client.DataFlowServerException;
import org.springframework.cloud.dataflow.rest.client.DataFlowTemplate;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.rest.resource.about.FeatureInfo;
import org.springframework.cloud.dataflow.rest.resource.about.MonitoringDashboardInfo;
import org.springframework.cloud.dataflow.rest.resource.about.MonitoringDashboardType;
import org.springframework.cloud.dataflow.rest.resource.about.RuntimeEnvironmentDetails;
import org.springframework.cloud.dataflow.rest.resource.about.SecurityInfo;
import org.springframework.cloud.dataflow.rest.resource.security.SecurityInfoResource;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.cloud.dataflow.rest.util.CheckableResource;
import org.springframework.cloud.dataflow.rest.util.HttpClientConfigurer;
import org.springframework.cloud.dataflow.rest.util.ProcessOutputResource;
import org.springframework.cloud.dataflow.rest.util.ResourceBasedAuthorizationInterceptor;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.TargetCredentials;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.command.support.TablesInfo;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.dataflow.shell.config.DataFlowShellProperties;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration commands for the Shell.
 *
 * @author Gunnar Hillert
 * @author Marius Bogoevici
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @author Mark Pollack
 * @author Eric Bottard
 * @author Mike Heath
 * @author Chris Bono
 */
@ShellComponent
public class ConfigCommands {

	private static final Authentication DEFAULT_PRINCIPAL = createAuthentication("dataflow-shell-principal");

	private DataFlowShell shell;

	private DataFlowShellProperties shellProperties;

	private ConsoleUserInput userInput;

	private TargetHolder targetHolder;

	private RestTemplate restTemplate;

	private OAuth2ClientProperties oauth2ClientProperties;

	private final ObjectMapper mapper;

	ConfigCommands(
			DataFlowShell shell,
			DataFlowShellProperties shellProperties,
			ConsoleUserInput userInput,
			TargetHolder targetHolder,
			RestTemplate restTemplate,
			@Nullable OAuth2ClientProperties oauth2ClientProperties,
			@Nullable ObjectMapper mapper
	) {
		this.shell = shell;
		this.shellProperties = shellProperties;
		this.userInput = userInput;
		this.targetHolder = targetHolder;
		this.restTemplate = restTemplate;
		this.oauth2ClientProperties = oauth2ClientProperties;
		if (mapper == null) {
			mapper = new ObjectMapper();
			mapper.registerModule(new Jdk8Module());
			mapper.registerModule(new Jackson2HalModule());
			mapper.registerModule(new JavaTimeModule());
			mapper.registerModule(new Jackson2DataflowModule());
		}
		this.mapper = mapper;
	}

	@ShellMethod(key = "dataflow config server", value = "Configure the Spring Cloud Data Flow REST server to use")
	public String target(
			@ShellOption(value = {"", "--uri"}, help = "the location of the Spring Cloud Data Flow REST endpoint", defaultValue = Target.DEFAULT_TARGET) String uri,
			@ShellOption(help = "the username for authenticated access to the Admin REST endpoint", defaultValue = ShellOption.NULL) String username,
			@ShellOption(help = "the password for authenticated access to the Admin REST endpoint (valid only with a "
					+ "username)", defaultValue = ShellOption.NULL) String password,
			@ShellOption(help = "the registration id for oauth2 config ", defaultValue = Target.DEFAULT_CLIENT_REGISTRATION_ID) String clientRegistrationId,
			@ShellOption(help = "a command to run that outputs the HTTP credentials used for authentication",
					defaultValue = ShellOption.NULL) String credentialsProviderCommand,
			@ShellOption(help = "accept any SSL certificate (even self-signed)", defaultValue = "false") boolean skipSslValidation,
			@ShellOption(help = "the uri of the proxy server", defaultValue = ShellOption.NULL) String proxyUri,
			@ShellOption(help = "the username for authenticated access to the secured proxy server", defaultValue = ShellOption.NULL) String proxyUsername,
			@ShellOption(help = "the password for authenticated access to the secured proxy server (valid only with a "
					+ "username)", defaultValue = ShellOption.NULL) String proxyPassword
	) throws Exception {

		if (credentialsProviderCommand == null && password != null && username == null) {
			return "A password may be specified only together with a username";
		}

		try {
			this.targetHolder.setTarget(new Target(uri, username, password, skipSslValidation));

			final HttpClientConfigurer httpClientConfigurer = HttpClientConfigurer.create(this.targetHolder.getTarget().getTargetUri())
					.skipTlsCertificateVerification(skipSslValidation);

			if (StringUtils.hasText(proxyUri)) {
				if (proxyPassword == null && proxyUsername != null) {
					// read password from the command line
					proxyPassword = userInput.prompt("Proxy Server Password", "", false);
				}
				httpClientConfigurer.withProxyCredentials(URI.create(proxyUri), proxyUsername, proxyPassword);
			}

			this.restTemplate.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());

			final SecurityInfoResource securityInfoResourceBeforeLogin = restTemplate
					.getForObject(uri + "/security/info", SecurityInfoResource.class);

			boolean authenticationEnabled = false;
			if (securityInfoResourceBeforeLogin != null) {
				authenticationEnabled = securityInfoResourceBeforeLogin.isAuthenticationEnabled();
			}

			if (StringUtils.isEmpty(credentialsProviderCommand) &&
					StringUtils.isEmpty(username) && authenticationEnabled) {
				username = userInput.prompt("Username", "", true);
			}

			if (StringUtils.isEmpty(credentialsProviderCommand) && authenticationEnabled &&
					StringUtils.isEmpty(password) && !StringUtils.isEmpty(username)) {
				password = userInput.prompt("Password", "", false);
			}

			this.targetHolder.setTarget(new Target(uri, username, password, skipSslValidation));

			if (StringUtils.hasText(credentialsProviderCommand) && authenticationEnabled) {
				this.targetHolder.getTarget().setTargetCredentials(new TargetCredentials(true));
				final CheckableResource credentialsResource = new ProcessOutputResource(credentialsProviderCommand.split("\\s+"));
				httpClientConfigurer.addInterceptor(new ResourceBasedAuthorizationInterceptor(credentialsResource));
			}

			if (oauth2ClientProperties != null && !oauth2ClientProperties.getRegistration().isEmpty()) {
				ClientHttpRequestInterceptor bearerTokenResolvingInterceptor = bearerTokenResolvingInterceptor(
						oauth2ClientProperties, username, password, clientRegistrationId);
				this.restTemplate.getInterceptors().add(bearerTokenResolvingInterceptor);
			} else if (authenticationEnabled && StringUtils.hasText(username) && StringUtils.hasText(password)) {
				httpClientConfigurer.basicAuthCredentials(username, password);
			}

			this.restTemplate.setRequestFactory(httpClientConfigurer.buildClientHttpRequestFactory());

			this.shell.setDataFlowOperations(
					new DataFlowTemplate(targetHolder.getTarget().getTargetUri(), this.restTemplate, this.mapper));
			this.targetHolder.getTarget()
					.setTargetResultMessage(String.format("Successfully targeted %s", uri));

			final SecurityInfoResource securityInfoResource = restTemplate
					.getForObject(uri + "/security/info", SecurityInfoResource.class);

			if (securityInfoResource.isAuthenticated()
					&& this.targetHolder.getTarget().getTargetCredentials() != null) {
				for (String roleAsString : securityInfoResource.getRoles()) {
					final RoleType shellRoleType = RoleType.fromKey(roleAsString);
					this.targetHolder.getTarget().getTargetCredentials().getRoles().add(shellRoleType);
				}
			}

			this.targetHolder.getTarget().setAuthenticated(securityInfoResource.isAuthenticated());
			this.targetHolder.getTarget().setAuthenticationEnabled(securityInfoResource.isAuthenticationEnabled());
			this.targetHolder.getTarget().setTargetResultMessage(String.format("Successfully targeted %s", uri));

			return this.targetHolder.getTarget().getTargetResultMessage();
		} catch (Exception e) {
			this.targetHolder.getTarget().setTargetException(e);
			this.shell.setDataFlowOperations(null);
			throw e;
		}
	}

	/**
	 * Triggers a connection to the DataFlow server using the configured coordinates.
	 *
	 * @return result message of the connection attempt
	 * @throws Exception from target method.
	 */
	public String triggerTarget() throws Exception {
		return target(
				this.shellProperties.getUri(),
				this.shellProperties.getUsername(),
				this.shellProperties.getPassword(),
				this.shellProperties.getClientRegistrationId(),
				this.shellProperties.getCredentialsProviderCommand(),
				this.shellProperties.isSkipSslValidation(),
				this.shellProperties.getProxy().getUri(),
				this.shellProperties.getProxy().getUsername(),
				this.shellProperties.getProxy().getPassword());
	}

	@ShellMethod(key = "dataflow config info", value = "Show the Dataflow server being used")
	@SuppressWarnings("unchecked")
	public TablesInfo info() {
		Target target = targetHolder.getTarget();
		if (target.getTargetException() != null) {
			throw new DataFlowServerException(this.targetHolder.getTarget().getTargetResultMessage());
		}
		AboutResource about = this.shell.getDataFlowOperations().aboutOperation().get();

		TablesInfo result = new TablesInfo();
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
		List<RuntimeEnvironmentDetails> taskLaunchers = about.getRuntimeEnvironment().getTaskLaunchers();
		String deployerColumnName = "Skipper Deployer";
		modelBuilder.addRow().addValue(deployerColumnName).addValue(appDeployer);
		rowIndex++;
		if (!appDeployer.getPlatformSpecificInfo().isEmpty()) {
			modelBuilder.addRow().addValue("Platform Specific").addValue(appDeployer.getPlatformSpecificInfo());
			rowsWithThinSeparators.add(rowIndex++);
		}
		for (RuntimeEnvironmentDetails taskLauncher : taskLaunchers) {
			modelBuilder.addRow().addValue("Task Launcher").addValue(taskLauncher);
			rowIndex++;
			if (!taskLauncher.getPlatformSpecificInfo().isEmpty()) {
				modelBuilder.addRow().addValue("Platform Specific").addValue(taskLauncher.getPlatformSpecificInfo());
				rowsWithThinSeparators.add(rowIndex++);
			}
		}

		if (about.getMonitoringDashboardInfo().getDashboardType() != MonitoringDashboardType.NONE) {
			modelBuilder.addRow().addValue("Monitoring").addValue(about.getMonitoringDashboardInfo());
			rowIndex++;
		}

		TableBuilder builder = new TableBuilder(modelBuilder.build());
		builder.addOutlineBorder(BorderStyle.fancy_double)
				.paintBorder(BorderStyle.fancy_light, BorderSpecification.INNER).fromTopLeft().toBottomRight()
				.on(CellMatchers.table()).addAligner(SimpleHorizontalAligner.center).on(CellMatchers.table())
				.addAligner(SimpleVerticalAligner.middle);

		Tables.configureKeyValueRendering(builder, ": ");

		builder.on(CellMatchers.ofType(MonitoringDashboardInfo.class)).addFormatter(new DataFlowTables.BeanWrapperFormatter(": "))
				.addAligner(new KeyValueHorizontalAligner(":")).addSizer(new KeyValueSizeConstraints(": "))
				.addWrapper(new KeyValueTextWrapper(": "));
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


		result.addTable(builder.build());

		if (Target.TargetStatus.ERROR.equals(target.getStatus())) {
			StringWriter stringWriter = new StringWriter();
			stringWriter.write("\nAn exception occurred during targeting:\n");
			target.getTargetException().printStackTrace(new PrintWriter(stringWriter));

			result.addFooter(stringWriter.toString());
		}
		return result;
	}

	private ClientRegistrationRepository shellClientRegistrationRepository(OAuth2ClientProperties properties) {
		var oauthClientPropsMapper = new OAuth2ClientPropertiesMapper(properties);
		return new InMemoryClientRegistrationRepository(oauthClientPropsMapper.asClientRegistrations().values().stream().toList());
	}

	private OAuth2AuthorizedClientService shellAuthorizedClientService(ClientRegistrationRepository shellClientRegistrationRepository) {
		return new InMemoryOAuth2AuthorizedClientService(shellClientRegistrationRepository);
	}

	private OAuth2AuthorizedClientManager authorizedClientManager(
			ClientRegistrationRepository shellClientRegistrationRepository,
			OAuth2AuthorizedClientService shellAuthorizedClientService) {
		AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				shellClientRegistrationRepository, shellAuthorizedClientService);
		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				.password()
				.refreshToken()
				.build();
		manager.setAuthorizedClientProvider(authorizedClientProvider);
		manager.setContextAttributesMapper(request -> {
			Map<String, Object> contextAttributes = new HashMap<>();
			request.getAttributes().forEach((k, v) -> {
				if (OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME.equals(k)
						|| OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME.equals(k)) {
					contextAttributes.put(k, v);
				}
			});
			return contextAttributes;
		});
		return manager;
	}

	private ClientHttpRequestInterceptor bearerTokenResolvingInterceptor(
			OAuth2ClientProperties properties, String username, String password, String clientRegistrationId) {
		ClientRegistrationRepository shellClientRegistrationRepository = shellClientRegistrationRepository(properties);
		OAuth2AuthorizedClientService shellAuthorizedClientService = shellAuthorizedClientService(shellClientRegistrationRepository);
		OAuth2AuthorizedClientManager authorizedClientManager = authorizedClientManager(
				shellClientRegistrationRepository, shellAuthorizedClientService);

		if (properties.getRegistration() != null && properties.getRegistration().size() == 1) {
			// if we have only one, use that
			clientRegistrationId = properties.getRegistration().entrySet().iterator().next().getKey();
		}

		OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
				.principal(DEFAULT_PRINCIPAL)
				.attribute(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username)
				.attribute(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password)
				.build();

		return (request, body, execution) -> {
			OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
			request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
			return execution.execute(request, body);
		};
	}

	private static Authentication createAuthentication(final String principalName) {
		return new AbstractAuthenticationToken(null) {
			private static final long serialVersionUID = -2038812908189509872L;

			@Override
			public Object getCredentials() {
				return "";
			}

			@Override
			public Object getPrincipal() {
				return principalName;
			}
		};
	}
}
