/*
 * Copyright 2018-2023 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.client.AppRegistryOperations;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.command.support.TablesInfo;
import org.springframework.cloud.dataflow.shell.completer.ApplicationNameValueProvider;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.hateoas.PagedModel;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.EnumValueProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.AbsoluteWidthSizeConstraints;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Commands for working with the application registry. Allows retrieval of information
 * about available applications, as well as creating and removing application
 * registrations.
 *
 * @author Glenn Renfro
 * @author Eric Bottard
 * @author Florent Biville
 * @author David Turanski
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Thomas Risberg
 * @author Gunnar Hillert
 * @author Christian Tzolov
 * @author Chris Schaefer
 * @author Chris Bono
 */
@ShellComponent
public class AppRegistryCommands implements ResourceLoaderAware {

	private static final String REGISTER_APPLICATION = "app register";
	private static final String IMPORT_APPLICATIONS = "app import";
	private static final String UNREGISTER_APPLICATION = "app unregister";
	private static final String UNREGISTER_ALL = "app all unregister";
	private static final String DEFAULT_APPLICATION = "app default";
	private static final String APPLICATION_INFO = "app info";
	private static final String LIST_APPLICATIONS = "app list";

	protected final DataFlowShell dataFlowShell;
	protected ResourceLoader resourceLoader = new DefaultResourceLoader();

	public AppRegistryCommands(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "resourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	public Availability availableWithViewRole() {
		return availabilityFor(RoleType.VIEW, OpsType.APP_REGISTRY);
	}

	public Availability availableWithCreateRole() {
		return availabilityFor(RoleType.CREATE, OpsType.APP_REGISTRY);
	}

	public Availability availableWithUModifyRole() {
		return availabilityFor(RoleType.MODIFY, OpsType.APP_REGISTRY);
	}

	public Availability availableWithDestroyRole() {
		return availabilityFor(RoleType.DESTROY, OpsType.APP_REGISTRY);
	}

	private Availability availabilityFor(RoleType roleType, OpsType opsType) {
		return dataFlowShell.hasAccess(roleType, opsType)
				? Availability.available()
				: Availability.unavailable("you do not have permissions");
	}

	@ShellMethod(key = APPLICATION_INFO, value = "Get information about an application")
	@ShellMethodAvailability("availableWithViewRole")
	public TablesInfo info(
			@ShellOption(value = { "--name" }, help = "name of the application to query", valueProvider = ApplicationNameValueProvider.class) String name,
			@ShellOption(help = "type of the application to query", valueProvider = EnumValueProvider.class) ApplicationType type,
			@ShellOption(help = "the version for the registered application", defaultValue = ShellOption.NULL) String version,
			@ShellOption(help = "return all metadata, including common Spring Boot properties", defaultValue = "false") boolean exhaustive) {
		TablesInfo result = new TablesInfo();
		try {
			DetailedAppRegistrationResource info = StringUtils.hasText(version) ?
					appRegistryOperations().info(name, type, version, exhaustive) :
					appRegistryOperations().info(name, type, exhaustive);
			if (info != null) {
				List<ConfigurationMetadataProperty> options = info.getOptions();
				result.addHeader(String.format("Information about %s application '%s':", type, name));
				if (info.getVersion() != null) {
					result.addHeader(String.format("Version: '%s':", info.getVersion()));
				}
				if (info.getDefaultVersion()) {
					result.addHeader(String.format("Default application version: '%s':", info.getDefaultVersion()));
				}
				result.addHeader(String.format("Resource URI: %s", info.getUri()));
				if (info.getShortDescription() != null) {
					result.addHeader(info.getShortDescription());
				}
				if (options == null) {
					result.addHeader("Application options metadata is not available");
				}
				else {
					TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
					modelBuilder.addRow().addValue("Option Name").addValue("Description").addValue("Default")
							.addValue("Type");
					for (ConfigurationMetadataProperty option : options) {
						modelBuilder.addRow().addValue(option.getId())
								.addValue(option.getDescription() == null ? "<unknown>" : option.getDescription())
								.addValue(prettyPrintDefaultValue(option))
								.addValue(option.getType() == null ? "<unknown>" : option.getType());
					}
					TableBuilder builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()))
							.on(CellMatchers.table()).addSizer(new AbsoluteWidthSizeConstraints(30)).and();
					result.addTable(builder.build());
				}
			}
			else {
				result.addHeader(String.format("Application info is not available for %s:%s", type, name));
			}
		}
		catch (Exception e) {
			result.addHeader(String.format("Application info is not available for %s:%s", type, name));
		}
		return result;
	}

	@ShellMethod(key = UNREGISTER_APPLICATION, value = "Unregister an application")
	@ShellMethodAvailability("availableWithDestroyRole")
	public String unregister(
			@ShellOption(value = { "", "--name" }, help = "name of the application to unregister", valueProvider = ApplicationNameValueProvider.class) String name,
			@ShellOption(help = "type of the application to unregister", valueProvider = EnumValueProvider.class) ApplicationType type,
			@ShellOption(help = "the version application to unregister", defaultValue = ShellOption.NULL) String version) {

		appRegistryOperations().unregister(name, type, version);

		List<AppRegistrationResource> appRegistrations = findAllAppsByNameAndType(name, type);
		Optional<AppRegistrationResource> defaultApp = appRegistrations.stream()
				.filter(a -> a.getDefaultVersion() == true).findFirst();

		if (!CollectionUtils.isEmpty(appRegistrations) && !defaultApp.isPresent()) {
			String appVersions = appRegistrations.stream().map(app -> app.getVersion())
					.collect(Collectors.joining(", ", "(", ")"));
			return String.format("Successfully unregistered application '%s' with type '%s'. " +
					"Please select new default version from: %s", name, type, appVersions);
		}

		return String.format("Successfully unregistered application '%s' with type '%s'.", name, type);

	}

	private List<AppRegistrationResource> findAllAppsByNameAndType(String appName, ApplicationType appType) {
		return appRegistryOperations().list().getContent().stream()
				.filter(a -> a.getName().equals(appName))
				.filter(a -> a.getType().equals(appType.toString()))
				.collect(Collectors.toList());
	}

	@ShellMethod(key = UNREGISTER_ALL, value = "Unregister all applications")
	@ShellMethodAvailability("availableWithDestroyRole")
	public String unregisterAll() {
		appRegistryOperations().unregisterAll();

		StringBuilder msg = new StringBuilder()
				.append("Successfully unregistered applications.");

		PagedModel<AppRegistrationResource> appRegistrationResources = appRegistryOperations().list();

		if (!appRegistrationResources.getContent().isEmpty()) {
			msg.append(" The following were not unregistered as they are associated with an existing stream:");

			for(AppRegistrationResource appRegistrationResource : appRegistrationResources) {
				msg.append(String.format(" [%s:%s:%s]", appRegistrationResource.getName(),
						appRegistrationResource.getType(), appRegistrationResource.getVersion()));
			}
		}

		return msg.toString();
	}

	@ShellMethod(key = DEFAULT_APPLICATION, value = "Change the default application version")
	@ShellMethodAvailability("availableWithUModifyRole")
	public String defaultApplication(
			@ShellOption(value = { "", "--id" }, help = "id of the application to query in the form of 'type:name'") QualifiedApplicationName application,
			@ShellOption(help = "the new default application version") String version) {

		appRegistryOperations().makeDefault(application.name, application.type, version);

		return String.format("New default Application %s:%s:%s", application.type, application.name, version);
	}

	@ShellMethod(key = REGISTER_APPLICATION, value = "Register a new application")
	@ShellMethodAvailability("availableWithCreateRole")
	public String register(
			@ShellOption(value = { "", "--name" }, help = "the name for the registered application") String name,
			@ShellOption(help = "the type for the registered application", valueProvider = EnumValueProvider.class) ApplicationType type,
			@ShellOption(help = "URI for the application artifact") String uri,
			@ShellOption(value = { "-m", "--metadata-uri", "--metadataUri"}, help = "Metadata URI for the application artifact", defaultValue = ShellOption.NULL) String metadataUri,
			@ShellOption(help = "force update if application is already registered (only if not in use)", defaultValue = "false") boolean force) {
		appRegistryOperations().register(name, type, uri, metadataUri, force);
		return String.format(("Successfully registered application '%s:%s"), type, name);
	}

	@ShellMethod(key = LIST_APPLICATIONS, value = "List all registered applications")
	@ShellMethodAvailability("availableWithViewRole")
	public Object list(
			@ShellOption(value = { "", "--id" }, help = "id of the application to query in the form of 'type:name'",
					defaultValue = ShellOption.NULL) QualifiedApplicationName application) {
		PagedModel<AppRegistrationResource> appRegistrations = appRegistryOperations().list();

		// TODO This can go outside the method
		final LinkedHashMap<String, List<String>> mappings = new LinkedHashMap<>();
		for (ApplicationType type : ApplicationType.values()) {
			mappings.put(type.name(), new ArrayList<>());
		}
		//

		int max = 0;
		for (AppRegistrationResource appRegistration : appRegistrations) {

			if (application != null &&
					(!application.name.equals(appRegistration.getName())
							|| !application.type.toString().equals(appRegistration.getType()))) {
				continue;
			}

			List<String> column = mappings.get(appRegistration.getType());
			String value = appRegistration.getName();
			if (application != null) {
				String version = StringUtils.hasLength(appRegistration.getVersion()) ?  ("-" + appRegistration.getVersion()) : "";
				value = value + version;
				if (appRegistration.getDefaultVersion()) {
					value = String.format("> %s <", value);
				}
			}
			if (!column.contains(value)) {
				column.add(value);
			}
			max = Math.max(max, column.size());
		}
		if (max == 0) {
			return String.format("No registered apps.%n" + "You can register new apps with the '%s' and '%s' commands.",
					REGISTER_APPLICATION, IMPORT_APPLICATIONS);
		}

		final List<String> keys = new ArrayList<>(mappings.keySet());
		final int rows = max + 1;
		final TableModel model = new TableModel() {

			@Override
			public int getRowCount() {
				return rows;
			}

			@Override
			public int getColumnCount() {
				return keys.size();
			}

			@Override
			public Object getValue(int row, int column) {
				String key = keys.get(column);
				if (row == 0) {
					return key;
				}
				int currentRow = row - 1;
				if (mappings.get(key).size() > currentRow) {
					return mappings.get(key).get(currentRow);
				}
				else {
					return null;
				}
			}
		};
		return DataFlowTables.applyStyle(new TableBuilder(model)).build();
	}

	@ShellMethod(key = IMPORT_APPLICATIONS, value = "Register all applications listed in a properties file")
	@ShellMethodAvailability("availableWithCreateRole")
	public String importFromResource(
			@ShellOption(help = "URI for the properties file") String uri,
			@ShellOption(help = "whether to resolve the URI locally (as opposed to on the server)", defaultValue = "true", arity = 1) boolean local,
			@ShellOption(help = "force update if any module already exists (only if not in use)", defaultValue = "false") boolean force) {
		if (local) {
			try {
				Resource resource = this.resourceLoader.getResource(uri);
				Properties applications = PropertiesLoaderUtils.loadProperties(resource);
				PagedModel<AppRegistrationResource> registered = null;
				try {
					registered = appRegistryOperations().registerAll(applications, force);
				}
				catch (Exception e) {
					return "Error when registering applications from " + uri + ": " + e.getMessage();
				}
				long numRegistered = registered.getMetadata().getTotalElements();
				return (applications.keySet().size() == numRegistered)
						? String.format("Successfully registered applications: %s", applications.keySet())
						: String.format("Successfully registered %d applications from %s", numRegistered,
						applications.keySet());
			}
			catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		else {
			PagedModel<AppRegistrationResource> registered = appRegistryOperations().importFromResource(uri, force);
			return String.format("Successfully registered %d applications from '%s'",
					registered.getMetadata().getTotalElements(), uri);
		}
	}

	protected AppRegistryOperations appRegistryOperations() {
		return dataFlowShell.getDataFlowOperations().appRegistryOperations();
	}

	/**
	 * Escapes some special values so that they don't disturb console rendering and are easier
	 * to read.
	 *
	 * @param o the configurationMetadataProperty to pretty print
	 * @return the pretty printed value
	 */
	protected String prettyPrintDefaultValue(ConfigurationMetadataProperty o) {
		if (o.getDefaultValue() == null) {
			return "<none>";
		}
		return o.getDefaultValue().toString().replace("\n", "\\n").replace("\t", "\\t").replace("\f", "\\f");
	}

	/**
	 * Unique identifier for an application, including the name and type.
	 */
	public static class QualifiedApplicationName {

		public ApplicationType type;

		public String name;

		public QualifiedApplicationName(String name, ApplicationType type) {
			this.name = name;
			this.type = type;
		}
	}
}
