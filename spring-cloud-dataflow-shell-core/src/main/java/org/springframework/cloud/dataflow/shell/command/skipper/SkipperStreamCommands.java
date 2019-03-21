/*
 * Copyright 2015-2018 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.common.AbstractStreamCommands;
import org.springframework.cloud.dataflow.shell.command.common.Assertions;
import org.springframework.cloud.dataflow.shell.command.common.DataFlowTables;
import org.springframework.cloud.dataflow.shell.command.common.UserInput;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_NAME;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PACKAGE_VERSION;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_PLATFORM_NAME;
import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_REPO_NAME;

/**
 * Stream commands.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Janne Valkealahti
 */
@Component
public class SkipperStreamCommands extends AbstractStreamCommands implements CommandMarker {

	private static final String STREAM_SKIPPER_DEPLOY = "stream deploy";

	private static final String STREAM_SKIPPER_UPDATE = "stream update";

	private static final String STREAM_SKIPPER_ROLLBACK = "stream rollback";

	private static final String STREAM_SKIPPER_MANIFEST_GET = "stream manifest";

	private static final String STREAM_SKIPPER_HISTORY = "stream history";

	private static final String STREAM_SKIPPER_PLATFORM_LIST = "stream platform-list";

	@Autowired
	public void setDataFlowShell(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	@Autowired
	public void setUserInput(UserInput userInput) {
		this.userInput = userInput;
	}

	@CliAvailabilityIndicator({ STREAM_SKIPPER_DEPLOY, STREAM_SKIPPER_UPDATE })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.STREAM);
	}

	@CliCommand(value = STREAM_SKIPPER_DEPLOY, help = "Deploy a previously created stream using Skipper")
	public String deployStream(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream to deploy", mandatory = true, optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = {
					PROPERTIES_OPTION }, help = "the properties for this deployment") String deploymentProperties,
			@CliOption(key = {
					PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)") File propertiesFile,
			@CliOption(key = "packageVersion", help = "the package version of the package to deploy.  Default is 1.0.0"
					+ "when using Skipper", unspecifiedDefaultValue = "1.0.0") String packageVersion,
			@CliOption(key = "platformName", help = "the name of the target platform to deploy when using Skipper") String platformName,
			@CliOption(key = "repoName", help = "the name of the local repository to upload the package when using "
					+ "Skipper") String repoName)
			throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, deploymentProperties, PROPERTIES_FILE_OPTION,
				propertiesFile);
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parseDeploymentProperties(deploymentProperties,
				propertiesFile, which);
		propertiesToUse.put(SKIPPER_PACKAGE_NAME, name);
		Assert.isTrue(StringUtils.hasText(packageVersion), "Package version must be set when using Skipper.");
		propertiesToUse.put(SKIPPER_PACKAGE_VERSION, packageVersion);
		if (StringUtils.hasText(platformName)) {
			propertiesToUse.put(SKIPPER_PLATFORM_NAME, platformName);
		}
		if (StringUtils.hasText(repoName)) {
			propertiesToUse.put(SKIPPER_REPO_NAME, repoName);
		}
		streamOperations().deploy(name, propertiesToUse);
		return String.format("Deployment request has been sent for stream '%s'", name);
	}

	@CliCommand(value = STREAM_SKIPPER_MANIFEST_GET, help = "Get manifest for the stream deployed using Skipper")
	public String getManifest(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream", mandatory = true, optionContext = "existing-stream "
					+ "disable-string-converter") String name,
			@CliOption(key = { "releaseVersion" }, help = "the Skipper release version to get the manifest for",
					unspecifiedDefaultValue = "0") int releaseVersion) {
		return streamOperations().getManifest(name, releaseVersion);
	}

	@CliCommand(value = STREAM_SKIPPER_HISTORY, help = "Get history for the stream deployed using Skipper")
	public Table history(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream", mandatory = true, optionContext = "existing-stream "
					+ "disable-string-converter") String name) {
		Collection<Release> releases = streamOperations().history(name);
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("version", "Version");
		headers.put("info.lastDeployed", "Last updated");
		headers.put("info.status.statusCode", "Status");
		headers.put("pkg.metadata.name", "Package Name");
		headers.put("pkg.metadata.version", "Package Version");
		headers.put("info.description", "Description");
		TableModel model = new BeanListTableModel<>(releases, headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		DataFlowTables.applyStyle(tableBuilder);
		return tableBuilder.build();
	}

	@CliCommand(value = STREAM_SKIPPER_PLATFORM_LIST, help = "List Skipper platforms")
	public Table listPlatforms() {
		Collection<Deployer> platforms = streamOperations().listPlatforms();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Name");
		headers.put("type", "Type");
		headers.put("description", "Description");
		BeanListTableModel<Deployer> model = new BeanListTableModel<>(platforms, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model)).build();
	}

	@CliCommand(value = STREAM_SKIPPER_UPDATE, help = "Update a previously created stream using Skipper")
	public String updateStream(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream to update", mandatory = true, optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = {
					"properties" }, help = "Flattened YAML style properties to update the stream", mandatory = false) String properties,
			@CliOption(key = {
					PROPERTIES_FILE_OPTION }, help = "the properties for the stream update (as a File)", mandatory = false) File propertiesFile,
			@CliOption(key = "packageVersion", help = "the package version of the package to update when using "
					+ "Skipper") String packageVersion,
			@CliOption(key = "repoName", help = "the name of the local repository to upload the package when using "
					+ "Skipper") String repoName)
			throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION,
				propertiesFile);
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parseDeploymentProperties(properties,
				propertiesFile, which);

		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(name);
		if (StringUtils.hasText(packageVersion)) {
			packageIdentifier.setPackageVersion(packageVersion);
		}
		if (StringUtils.hasText(repoName)) {
			packageIdentifier.setRepositoryName(repoName);
		}
		streamOperations().updateStream(name, name, packageIdentifier, propertiesToUse);
		return String.format("Update request has been sent for the stream '%s'", name);
	}

	@CliCommand(value = STREAM_SKIPPER_ROLLBACK, help = "Rollback a stream using Skipper")
	public String rollbackStreamUsingSkipper(
			@CliOption(key = { "", "name" }, help = "the name of the stream to rollback", mandatory = true,
					optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = { "releaseVersion" }, help = "the Skipper release version to rollback to",
					unspecifiedDefaultValue = "0") int releaseVersion) {
		this.streamOperations().rollbackStream(name, releaseVersion);
		return String.format("Rollback request has been sent for the stream '%s'", name);
	}
}
