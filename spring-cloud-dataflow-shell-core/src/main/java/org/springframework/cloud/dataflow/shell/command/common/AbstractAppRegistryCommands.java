/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.client.AppRegistryOperations;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.Assert;
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
 */
public abstract class AbstractAppRegistryCommands implements CommandMarker {

	private final static String LIST_APPLICATIONS = "app list";

	private static final String REGISTER_APPLICATION = "app register";

	private static final String IMPORT_APPLICATIONS = "app import";

	protected DataFlowShell dataFlowShell;

	protected ResourceLoader resourceLoader = new DefaultResourceLoader();

	public void setDataFlowShell(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "resourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	@CliAvailabilityIndicator({ LIST_APPLICATIONS })
	public boolean availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.APP_REGISTRY);
	}

	@CliAvailabilityIndicator({ REGISTER_APPLICATION, IMPORT_APPLICATIONS })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.APP_REGISTRY);
	}


	@CliCommand(value = REGISTER_APPLICATION, help = "Register a new application")
	public String register(
			@CliOption(mandatory = true, key = { "",
					"name" }, help = "the name for the registered application") String name,
			@CliOption(mandatory = true, key = {
					"type" }, help = "the type for the registered application") ApplicationType type,
			@CliOption(mandatory = true, key = { "uri" }, help = "URI for the application artifact") String uri,
			@CliOption(key = { "metadata-uri" }, help = "Metadata URI for the application artifact") String metadataUri,
			@CliOption(key = "force", help = "force update if application is already registered (only if not in use)", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean force) {

		appRegistryOperations().register(name, type, uri, metadataUri, force);

		return String.format(("Successfully registered application '%s:%s'"), type, name);
	}

	@CliCommand(value = LIST_APPLICATIONS, help = "List all registered applications")
	public Object list(@CliOption(
			key = { "", "id" },
			help = "id of the application to query in the form of 'type:name'") QualifiedApplicationName application) {
		PagedResources<AppRegistrationResource> appRegistrations = appRegistryOperations().list();

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
				String version = StringUtils.isEmpty(appRegistration.getVersion()) ? "" : "-"+appRegistration.getVersion();
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

	@CliCommand(value = IMPORT_APPLICATIONS, help = "Register all applications listed in a properties file")
	public String importFromResource(
			@CliOption(mandatory = true, key = { "", "uri" }, help = "URI for the properties file") String uri,
			@CliOption(key = "local", help = "whether to resolve the URI locally (as opposed to on the server)", specifiedDefaultValue = "true", unspecifiedDefaultValue = "true") boolean local,
			@CliOption(key = "force", help = "force update if any module already exists (only if not in use)", specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean force) {
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
						: String.format("Successfully registered %d applications from %s", numRegistered,
						applications.keySet());
			}
			catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		else {
			PagedResources<AppRegistrationResource> registered = appRegistryOperations().importFromResource(uri, force);
			return String.format("Successfully registered %d applications from '%s'",
					registered.getMetadata().getTotalElements(), uri);
		}
	}


	protected AppRegistryOperations appRegistryOperations() {
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

}
