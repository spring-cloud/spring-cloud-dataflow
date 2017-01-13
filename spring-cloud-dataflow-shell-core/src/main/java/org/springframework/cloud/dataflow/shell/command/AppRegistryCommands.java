/*
 * Copyright 2015-2017 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.client.AppRegistryOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.AbsoluteWidthSizeConstraints;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Commands for working with the application registry. Allows retrieval of information about
 * available applications, as well as creating and removing application registrations.
 *
 * @author Glenn Renfro
 * @author Eric Bottard
 * @author Florent Biville
 * @author David Turanski
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Thomas Risberg
 */
@Component
public class AppRegistryCommands implements CommandMarker, ResourceLoaderAware {

	private final static String LIST_APPLICATIONS = "app list";

	private final static String APPLICATION_INFO = "app info";

	private final static String UNREGISTER_APPLICATION = "app unregister";

	private static final String REGISTER_APPLICATION = "app register";

	private static final String IMPORT_APPLICATIONS = "app import";

	private DataFlowShell dataFlowShell;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Autowired
	public void setDataFlowShell(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "resourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	@CliAvailabilityIndicator({LIST_APPLICATIONS, APPLICATION_INFO, UNREGISTER_APPLICATION,
		REGISTER_APPLICATION, IMPORT_APPLICATIONS})
	public boolean available() {
		DataFlowOperations dataFlowOperations = dataFlowShell.getDataFlowOperations();
		return dataFlowOperations != null && dataFlowOperations.appRegistryOperations() != null;
	}

	@CliCommand(value = APPLICATION_INFO, help = "Get information about an application")
	public List<Object> info(
			@CliOption(mandatory = true,
					key = {"", "id"},
					help = "id of the application to query in the form of 'type:name'")
			QualifiedApplicationName application) {
		List<Object> result = new ArrayList<>();
		try {
			DetailedAppRegistrationResource info = appRegistryOperations().info(application.name, application.type);
			if (info != null) {
				List<ConfigurationMetadataProperty> options = info.getOptions();
				result.add(String.format("Information about %s application '%s':", application.type, application.name));
				result.add(String.format("Resource URI: %s", info.getUri()));
				if (info.getShortDescription() != null) {
					result.add(info.getShortDescription());
				}
				if (options == null) {
					result.add("Application options metadata is not available");
				}
				else {
					TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
					modelBuilder.addRow()
							.addValue("Option Name")
							.addValue("Description")
							.addValue("Default")
							.addValue("Type");
					for (ConfigurationMetadataProperty option : options) {
						modelBuilder.addRow()
								.addValue(option.getId())
								.addValue(option.getDescription() == null ? "<unknown>" : option.getDescription())
								.addValue(prettyPrintDefaultValue(option))
								.addValue(option.getType() == null ? "<unknown>" : option.getType());
					}
					TableBuilder builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()))
							.on(CellMatchers.table()).addSizer(new AbsoluteWidthSizeConstraints(30))
							.and();
					result.add(builder.build());
				}
			}
			else {
				result.add(String.format("Application info is not available for %s:%s", application.type, application.name));
			}
		}
		catch (Exception e) {
			result.add(String.format("Application info is not available for %s:%s", application.type, application.name));
		}
		return result;
	}

	@CliCommand(value = REGISTER_APPLICATION, help = "Register a new application")
	public String register(
			@CliOption(mandatory = true,
					key = {"", "name"},
					help = "the name for the registered application")
			String name,
			@CliOption(mandatory = true,
					key = {"type"},
					help = "the type for the registered application")
			ApplicationType type,
			@CliOption(mandatory = true,
					key = {"uri"},
					help = "URI for the application artifact")
			String uri,
			@CliOption(key = "force",
					help = "force update if application is already registered (only if not in use)",
					specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false")
			boolean force) {
		appRegistryOperations().register(name, type, uri, force);
		return String.format(("Successfully registered application '%s:%s'"), type, name);
	}

	@CliCommand(value = UNREGISTER_APPLICATION, help = "Unregister an application")
	public String unregister(
			@CliOption(mandatory = true,
					key = {"", "name"},
					help = "name of the application to unregister")
					String name,
			@CliOption(mandatory = true,
					key = {"type"},
					help = "type of the application to unregister")
					ApplicationType type) {

		appRegistryOperations().unregister(name, type);
		return String.format(("Successfully unregistered application '%s' with type %s"),
				name, type);
	}

	@CliCommand(value = LIST_APPLICATIONS, help = "List all registered applications")
	public Object list() {
		PagedResources<AppRegistrationResource> appRegistrations = appRegistryOperations().list();
		final LinkedHashMap<String, List<String>> mappings = new LinkedHashMap<>();
		for (ApplicationType type : ApplicationType.values()) {
			mappings.put(type.name(), new ArrayList<String>());
		}
		int max = 0;
		for (AppRegistrationResource appRegistration : appRegistrations) {
			List<String> column = mappings.get(appRegistration.getType());
			column.add(appRegistration.getName());
			max = Math.max(max, column.size());
		}
		if (max == 0) {
			return String.format("No registered apps.%n" +
					"You can register new apps with the '%s' and '%s' commands.", REGISTER_APPLICATION, IMPORT_APPLICATIONS);
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

	@CliCommand(value = IMPORT_APPLICATIONS, help = "Register all applications listed in a properties file")
	public String importFromResource(
			@CliOption(mandatory = true,
					key = {"", "uri"},
					help = "URI for the properties file")
			String uri,
			@CliOption(key = "local",
				help = "whether to resolve the URI locally (as opposed to on the server)",
				specifiedDefaultValue = "true",
				unspecifiedDefaultValue = "true")
			boolean local,
			@CliOption(key = "force",
				help = "force update if any module already exists (only if not in use)",
				specifiedDefaultValue = "true",
				unspecifiedDefaultValue = "false")
			boolean force) {
		if (local) {
			try {
				Resource resource = this.resourceLoader.getResource(uri);
				Properties applications = PropertiesLoaderUtils.loadProperties(resource);
				PagedResources<AppRegistrationResource> registered = null;
				try {
					registered = appRegistryOperations().registerAll(applications, force);
				}
				catch (Exception e) {
					return "Error when registering applications from " + uri + ": " + e.getMessage();
				}
				long numRegistered = registered.getMetadata().getTotalElements();
				return (applications.keySet().size() == numRegistered)
						? String.format("Successfully registered applications: %s", applications.keySet())
						: String.format("Successfully registered %d applications from %s", numRegistered, applications.keySet());
			}
			catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		else {
			PagedResources<AppRegistrationResource> registered = appRegistryOperations().importFromResource(uri, force);
			return String.format("Successfully registered %d applications from '%s'", registered.getMetadata().getTotalElements(), uri);
		}
	}

	/**
	 * Escapes some special values so that they don't disturb console
	 * rendering and are easier to read.
	 */
	private String prettyPrintDefaultValue(ConfigurationMetadataProperty o) {
		if (o.getDefaultValue() == null) {
			return "<none>";
		}
		return o.getDefaultValue().toString()
				.replace("\n", "\\n")
				.replace("\t", "\\t")
				.replace("\f", "\\f");
	}

	private AppRegistryOperations appRegistryOperations() {
		return dataFlowShell.getDataFlowOperations().appRegistryOperations();
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
