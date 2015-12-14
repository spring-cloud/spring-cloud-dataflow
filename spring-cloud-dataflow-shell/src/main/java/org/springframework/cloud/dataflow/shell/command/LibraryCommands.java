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

import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.client.LibraryOperations;
import org.springframework.cloud.dataflow.rest.resource.LibraryRegistrationResource;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.stereotype.Component;

/**
 * Commands for working with libraries. Allows retrieval of information about
 * available libraries, as well as creating and removing library registrations.
 * @author Eric Bottard
 */
@Component
public class LibraryCommands implements CommandMarker {

	private final static String LIST_LIBRARIES = "library list";

	private final static String UNREGISTER_LIBRARY = "library unregister";

	private static final String REGISTER_LIBRARY = "library register";

	private DataFlowShell dataFlowShell;

	@Autowired
	public void setDataFlowShell(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	@CliAvailabilityIndicator({REGISTER_LIBRARY, UNREGISTER_LIBRARY, LIST_LIBRARIES})
	public boolean available() {
		return dataFlowShell.getDataFlowOperations() != null;
	}

	@CliCommand(value = REGISTER_LIBRARY, help = "Register a new library")
	public String register(
			@CliOption(mandatory = true,
					key = {"", "name"},
					help = "the name for the library to register")
			String name,
			@CliOption(mandatory = true,
					key = {"coordinates"},
					help = "coordinates to the library")
			String coordinates,
			@CliOption(key = "force",
					help = "force update if library already exists (only if not in use)",
					specifiedDefaultValue = "true",
					unspecifiedDefaultValue = "false")
			boolean force) {
		libraryOperations().register(name, coordinates, force);
		return String.format(("Successfully registered library '%s' as %s"), name, coordinates);
	}

	@CliCommand(value = UNREGISTER_LIBRARY, help = "Unregister a library")
	public String unregister(
			@CliOption(mandatory = true,
					key = {"", "name"},
					help = "name of the library to unregister")
			String name
	) {

		libraryOperations().unregister(name);
		return String.format(("Successfully unregistered library '%s'"), name);
	}

	@CliCommand(value = LIST_LIBRARIES, help = "List all libraries")
	public Table list() {
		PagedResources<LibraryRegistrationResource> modules = libraryOperations().list();
		LinkedHashMap<String, Object> header = new LinkedHashMap<>();
		header.put("name", "Library Name");
		header.put("coordinates", "Maven Coordinates");
		BeanListTableModel<LibraryRegistrationResource> tableModel = new BeanListTableModel<>(modules, header);

		return DataFlowTables.applyStyle(new TableBuilder(tableModel)).build();
	}

	private LibraryOperations libraryOperations() {
		return dataFlowShell.getDataFlowOperations().libraryOperations();
	}


}
