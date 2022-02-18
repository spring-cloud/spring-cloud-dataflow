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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.rest.client.RuntimeOperations;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BorderSpecification;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.CellMatchers;
import org.springframework.shell.table.SimpleHorizontalAligner;
import org.springframework.shell.table.SimpleVerticalAligner;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.shell.table.Tables;

/**
 * Commands for displaying the runtime state of deployed apps.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Chris Bono
 */
@ShellComponent
public class RuntimeCommands {

	private static final String LIST_APPS = "runtime apps";

	private final DataFlowShell dataFlowShell;

	public RuntimeCommands(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	public Availability availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.RUNTIME)
				? Availability.available()
				: Availability.unavailable("you do not have permissions");
	}

	@ShellMethod(key = LIST_APPS, value = "List runtime apps")
	@ShellMethodAvailability("availableWithViewRole")
	public Table list(
			@ShellOption(help = "whether to hide app instance details", defaultValue = "false") boolean summary,
			@ShellOption(value = { "--appId", "--appIds" }, help = "app id(s) to display, also supports '<group>.*' pattern",
					defaultValue = ShellOption.NULL) String[] appIds) {

		Set<String> filter = null;
		if (appIds != null) {
			filter = new HashSet<>(Arrays.asList(appIds));
		}

		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		if (!summary) {
			modelBuilder.addRow().addValue("App Id / Instance Id").addValue("Unit Status")
					.addValue("No. of Instances / Attributes");
		}
		else {
			modelBuilder.addRow().addValue("App Id").addValue("Unit Status").addValue("No. of Instances");
		}

		// In detailed mode, keep track of app vs instance lines, to use
		// a different border style later.
		List<Integer> splits = new ArrayList<>();
		int line = 1;
		// Optimise for the single app case, which is likely less resource intensive on
		// the server
		// than client side filtering
		Iterable<AppStatusResource> statuses;
		if (filter != null && filter.size() == 1 && !filter.iterator().next().endsWith(".*")) {
			statuses = Collections.singleton(runtimeOperations().status(filter.iterator().next()));
		}
		else {
			statuses = runtimeOperations().status();
		}
		for (AppStatusResource appStatusResource : statuses) {
			if (filter != null && !shouldRetain(filter, appStatusResource)) {
				continue;
			}
			modelBuilder.addRow().addValue(appStatusResource.getDeploymentId()).addValue(appStatusResource.getState())
					.addValue(appStatusResource.getInstances().getContent().size());
			splits.add(line);
			line++;
			if (!summary) {
				for (AppInstanceStatusResource appInstanceStatusResource : appStatusResource.getInstances()) {
					modelBuilder.addRow().addValue(appInstanceStatusResource.getInstanceId())
							.addValue(appInstanceStatusResource.getState())
							.addValue(appInstanceStatusResource.getAttributes());
					line++;
				}
			}
		}

		TableModel model = modelBuilder.build();
		final TableBuilder builder = new TableBuilder(model);
		DataFlowTables.applyStyle(builder);
		builder.on(CellMatchers.column(0)).addAligner(SimpleVerticalAligner.middle).on(CellMatchers.column(1)).addAligner(SimpleVerticalAligner.middle).on(CellMatchers.column(1)).addAligner(SimpleHorizontalAligner.center)
				// This will match the "number of instances" cells only
				.on(CellMatchers.ofType(Integer.class)).addAligner(SimpleHorizontalAligner.center);

		Tables.configureKeyValueRendering(builder, " = ");
		for (int i = 2; i < model.getRowCount(); i++) {
			if (splits.contains(i)) {
				builder.paintBorder(BorderStyle.fancy_light, BorderSpecification.TOP).fromRowColumn(i, 0).toRowColumn(i + 1, model.getColumnCount());
			}
			else {
				builder.paintBorder(BorderStyle.fancy_light_quadruple_dash, BorderSpecification.TOP).fromRowColumn(i, 0).toRowColumn(i + 1,
						model.getColumnCount());
			}
		}

		return builder.build();
	}

	private boolean shouldRetain(Set<String> filter, AppStatusResource appStatusResource) {
		String deploymentId = appStatusResource.getDeploymentId();
		boolean directMatch = filter.contains(deploymentId);
		if (directMatch) {
			return true;
		}
		for (String candidate : filter) {
			if (candidate.endsWith(".*")) {
				String pattern = candidate.substring(0, candidate.length() - "*".length());
				if (deploymentId.startsWith(pattern)) {
					return true;
				}
			}
		}
		return false;
	}

	private RuntimeOperations runtimeOperations() {
		return dataFlowShell.getDataFlowOperations().runtimeOperations();
	}
}
