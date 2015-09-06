/*
 * Copyright 2015 the original author or authors.
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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ModuleType;
import org.springframework.cloud.dataflow.rest.client.ModuleOperations;
import org.springframework.cloud.dataflow.rest.resource.DetailedModuleRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedModuleRegistrationResource.Option;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.support.table.TableRow;
import org.springframework.stereotype.Component;
import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;


/**
 * Commands for working with modules. Allows retrieval of information about
 * available modules, as well as creating and removing module registrations.
 *
 * @author Glenn Renfro
 * @author Eric Bottard
 * @author Florent Biville
 * @author David Turanski
 * @author Patrick Peralta
 */
@Component
public class ModuleCommands implements CommandMarker {

	private final static String LIST_MODULES = "module list";

	private final static String MODULE_INFO = "module info";

	private final static String UNREGISTER_MODULE = "module unregister";

	private static final String REGISTER_MODULE = "module register";

	@Autowired
	private DataFlowShell dataFlowShell;


	@CliAvailabilityIndicator({LIST_MODULES, MODULE_INFO, UNREGISTER_MODULE, REGISTER_MODULE})
	public boolean available() {
		return dataFlowShell.getDataFlowOperations() != null;
	}

	@CliCommand(value = MODULE_INFO, help = "Get information about a module")
	public String info(
			@CliOption(mandatory = true,
					key = {"", "name"},
					help = "name of the module to query")
			String name,
			@CliOption(mandatory = false,
					key = {"type"},
					help = "type of the module to query")
			ModuleType type,
			@CliOption(key = "hidden",
					help = "whether to show 'hidden' options",
					specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false")
			boolean showHidden) {
		QualifiedModuleName module = processArgs(name, type);
		DetailedModuleRegistrationResource info = moduleOperations().info(module.name, module.type);
		List<Option> options = info.getOptions();
		StringBuilder result = new StringBuilder();
		result.append("Information about ")
				.append(module.type.name())
				.append(" module '")
				.append(module.name)
				.append("':\n\n");

		result.append("Maven coordinates: ").append(info.getCoordinates()).append("\n\n");

		if (info.getShortDescription() != null) {
			result.append(info.getShortDescription()).append("\n\n");
		}
		if (options == null) {
			result.append("Module options metadata is not available");
		}
		else {
			Table table = new Table()
					.addHeader(1, new TableHeader("Option Name"))
					.addHeader(2, new TableHeader("Description"))
					.addHeader(3, new TableHeader("Default"))
					.addHeader(4, new TableHeader("Type"));
			for (DetailedModuleRegistrationResource.Option o : options) {
				if (!showHidden && o.isHidden()) {
					continue;
				}
				final TableRow row = new TableRow();
				row.addValue(1, o.getName())
						.addValue(2, o.getDescription())
						.addValue(3, prettyPrintDefaultValue(o))
						.addValue(4, o.getType() == null ? "<unknown>" : o.getType());
				table.getRows().add(row);
			}
			result.append(table.toString());
		}
		return result.toString();
	}

	@CliCommand(value = REGISTER_MODULE, help = "Register a new module")
	public String register(
			@CliOption(mandatory = true,
					key = {"", "name"},
					help = "the name for the registered module")
			String name,
			@CliOption(mandatory = true,
					key = {"type"},
					help = "the type for the registered module")
			ModuleType type,
			@CliOption(mandatory = true,
					key = {"coordinates"},
					help = "coordinates to the module archive")
			String coordinates,
			@CliOption(key = "force",
					help = "force update if module already exists (only if not in use)",
					specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false")
			boolean force) {
		moduleOperations().register(name, type, coordinates, force);
		return String.format(("Successfully registered module '%s:%s'"), type, name);
	}

	@CliCommand(value = UNREGISTER_MODULE, help = "Unregister a module")
	public String unregister(
			@CliOption(mandatory = true,
					key = {"", "name"},
					help = "name of the module to unregister")
			String name,
			@CliOption(mandatory = false,
					key = {"type"},
					help = "type of the module to unregister")
			ModuleType type) {

		QualifiedModuleName module = processArgs(name, type);
		moduleOperations().unregister(module.name, module.type);
		return String.format(("Successfully unregistered module '%s' with type %s"),
				module.name, module.type);
	}

	@CliCommand(value = LIST_MODULES, help = "List all modules")
	public Table list() {
		PagedResources<ModuleRegistrationResource> modules = moduleOperations().list();
		return new ModuleList(modules).renderByType();
	}

	/**
	 * Escapes some special values so that they don't disturb console
	 * rendering and are easier to read.
	 */
	private String prettyPrintDefaultValue(Option o) {
		if (o.getDefaultValue() == null) {
			return "<none>";
		}
		return o.getDefaultValue()
				.replace("\n", "\\n")
				.replace("\t", "\\t")
				.replace("\f", "\\f");
	}

	private ModuleOperations moduleOperations() {
		return dataFlowShell.getDataFlowOperations().moduleOperations();
	}

	/**
	 * Return a {@link QualifiedModuleName} for the given arguments.
	 * If {@code type} is {@code null}, the module type may be obtained
	 * from the module name if the module name is in the format
	 * {@code name:type}.
	 *
	 * @param name module name
	 * @param type module type; may be {@code null}
	 * @return {@code QualifiedModuleName} for the provided arguments
	 */
	private QualifiedModuleName processArgs(String name, ModuleType type) {
		if (type == null) {
			String[] split = name.split("\\:");
			if (split.length != 2) {
				throw new IllegalArgumentException(
						String.format("Expected format of 'name:type' for module name %s", name));
			}
			return new QualifiedModuleName(split[0], ModuleType.valueOf(split[1]));
		}
		else {
			return new QualifiedModuleName(name, type);
		}
	}

	/**
	 * Unique identifier for a module, including the name and type.
	 */
	private static class QualifiedModuleName {

		public ModuleType type;

		public String name;

		public QualifiedModuleName(String name, ModuleType type) {
			this.name = name;
			this.type = type;
		}
	}

}
