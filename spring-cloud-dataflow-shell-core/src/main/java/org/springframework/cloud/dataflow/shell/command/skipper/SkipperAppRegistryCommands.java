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

package org.springframework.cloud.dataflow.shell.command.skipper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.cloud.dataflow.shell.command.common.AbstractAppRegistryCommands;
import org.springframework.cloud.dataflow.shell.command.common.DataFlowTables;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.AbsoluteWidthSizeConstraints;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.stereotype.Component;
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
 */
@Component
public class SkipperAppRegistryCommands extends AbstractAppRegistryCommands implements CommandMarker, ResourceLoaderAware {

	private final static String APPLICATION_INFO = "app info";

	private final static String UNREGISTER_APPLICATION = "app unregister";

	private static final String DEFAULT_APPLICATION = "app default";

	@Autowired
	public void setDataFlowShell(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "resourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	@CliAvailabilityIndicator({ APPLICATION_INFO })
	public boolean availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.APP_REGISTRY);
	}

	@CliAvailabilityIndicator({ UNREGISTER_APPLICATION, DEFAULT_APPLICATION })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.APP_REGISTRY);
	}

	@CliCommand(value = APPLICATION_INFO, help = "Get information about an application")
	public List<Object> info(
			@CliOption(mandatory = true, key = { "",
					"name" }, help = "name of the application to query") String name,
			@CliOption(mandatory = true, key = {
					"type" }, help = "type of the application to query") ApplicationType type,
			@CliOption(key = { "version" }, help = "the version for the registered application") String version,
			@CliOption(key = { "exhaustive" }, help = "return all metadata, including common Spring Boot properties",
					specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean exhaustive) {
		List<Object> result = new ArrayList<>();
		try {
			DetailedAppRegistrationResource info = StringUtils.hasText(version) ?
					appRegistryOperations().info(name, type, version, exhaustive) :
					appRegistryOperations().info(name, type, exhaustive);
			if (info != null) {
				List<ConfigurationMetadataProperty> options = info.getOptions();
				result.add(String.format("Information about %s application '%s':", type, name));
				if (info.getVersion() != null) {
					result.add(String.format("Version: '%s':", info.getVersion()));
				}
				if (info.getDefaultVersion()) {
					result.add(String.format("Default application version: '%s':", info.getDefaultVersion()));
				}
				result.add(String.format("Resource URI: %s", info.getUri()));
				if (info.getShortDescription() != null) {
					result.add(info.getShortDescription());
				}
				if (options == null) {
					result.add("Application options metadata is not available");
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
					result.add(builder.build());
				}
			}
			else {
				result.add(String.format("Application info is not available for %s:%s", type, name));
			}
		}
		catch (Exception e) {
			result.add(String.format("Application info is not available for %s:%s", type, name));
		}
		return result;
	}

	@CliCommand(value = UNREGISTER_APPLICATION, help = "Unregister an application")
	public String unregister(
			@CliOption(mandatory = true, key = { "",
					"name" }, help = "name of the application to unregister") String name,
			@CliOption(mandatory = true, key = {
					"type" }, help = "type of the application to unregister") ApplicationType type,
			@CliOption(key = { "version" }, help = "the version application to unregister") String version) {

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

	@CliCommand(value = DEFAULT_APPLICATION, help = "Change the default application version")
	public String defaultApplicaiton(
			@CliOption(mandatory = true, key = { "",
					"id" }, help = "id of the application to query in the form of 'type:name'") QualifiedApplicationName application,
			@CliOption(mandatory = true, key = {
					"version" }, help = "the new default application version") String version) {

		appRegistryOperations().makeDefault(application.name, application.type, version);

		return String.format("New default Application %s:%s:%s", application.type, application.name, version);
	}
}
