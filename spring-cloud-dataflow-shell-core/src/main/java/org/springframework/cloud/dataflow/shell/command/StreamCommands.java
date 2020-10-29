/*
 * Copyright 2018-2019 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.rest.client.StreamOperations;
import org.springframework.cloud.dataflow.rest.resource.StreamAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.command.support.ShellUtils;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.hateoas.PagedModel;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.shell.table.TableModelBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Stream commands.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @author Janne Valkealahti
 * @author Christian Tzolov
 */
@Component
public class StreamCommands implements CommandMarker {

	private static final String PROPERTIES_OPTION = "properties";
	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";

	protected DataFlowShell dataFlowShell;
	private UserInput userInput;

	// Create Role

	private static final String CREATE_STREAM = "stream create";

	// Deploy Role

	private static final String STREAM_DEPLOY = "stream deploy";
	private static final String UNDEPLOY_STREAM = "stream undeploy";
	private static final String UNDEPLOY_STREAM_ALL = "stream all undeploy";

	// Destroy Role

	private static final String DESTROY_STREAM = "stream destroy";
	private static final String DESTROY_STREAM_ALL = "stream all destroy";

	// Modify Role

	private static final String STREAM_ROLLBACK = "stream rollback";
	private static final String STREAM_UPDATE = "stream update";
	private static final String STREAM_SCALE = "stream scale app instances";

	// View Role

	private static final String INFO_STREAM = "stream info";
	private static final String LIST_STREAM = "stream list";
	private static final String STREAM_HISTORY = "stream history";
	private static final String STREAM_MANIFEST_GET = "stream manifest";
	private static final String STREAM_PLATFORM_LIST = "stream platform-list";
	private static final String VALIDATE_STREAM = "stream validate";

	@Autowired
	public void setDataFlowShell(DataFlowShell dataFlowShell) {
		this.dataFlowShell = dataFlowShell;
	}

	@Autowired
	public void setUserInput(UserInput userInput) {
		this.userInput = userInput;
	}

	@CliAvailabilityIndicator({ CREATE_STREAM })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.STREAM);
	}

	@CliAvailabilityIndicator({ STREAM_DEPLOY, UNDEPLOY_STREAM, UNDEPLOY_STREAM_ALL })
	public boolean availableWithDeployRole() {
		return dataFlowShell.hasAccess(RoleType.DEPLOY, OpsType.STREAM);
	}

	@CliAvailabilityIndicator({ DESTROY_STREAM, DESTROY_STREAM_ALL })
	public boolean availableWithDestroyRole() {
		return dataFlowShell.hasAccess(RoleType.DESTROY, OpsType.STREAM);
	}

	@CliAvailabilityIndicator({ STREAM_ROLLBACK, STREAM_UPDATE, STREAM_SCALE })
	public boolean availableWithModifyRole() {
		return dataFlowShell.hasAccess(RoleType.MODIFY, OpsType.STREAM);
	}

	@CliAvailabilityIndicator({ INFO_STREAM, LIST_STREAM, STREAM_HISTORY, STREAM_MANIFEST_GET, STREAM_PLATFORM_LIST, VALIDATE_STREAM })
	public boolean availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.STREAM);
	}

	@CliCommand(value = STREAM_DEPLOY, help = "Deploy a previously created stream using Skipper")
	public String deployStream(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream to deploy", mandatory = true, optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = {
					PROPERTIES_OPTION }, help = "the properties for this deployment") String deploymentProperties,
			@CliOption(key = {
					PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)") File propertiesFile,
			@CliOption(key = "packageVersion", help = "the package version of the package to deploy.  Default is 1.0.0", unspecifiedDefaultValue = "1.0.0") String packageVersion,
			@CliOption(key = "platformName", help = "the name of the target platform to deploy to") String platformName,
			@CliOption(key = "repoName", help = "the name of the local repository to upload the package to") String repoName)
			throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, deploymentProperties, PROPERTIES_FILE_OPTION,
				propertiesFile);
		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parseDeploymentProperties(deploymentProperties,
				propertiesFile, which);
		propertiesToUse.put(SkipperStream.SKIPPER_PACKAGE_NAME, name);
		Assert.isTrue(StringUtils.hasText(packageVersion), "Package version must be set.");
		propertiesToUse.put(SkipperStream.SKIPPER_PACKAGE_VERSION, packageVersion);
		if (StringUtils.hasText(platformName)) {
			propertiesToUse.put(SkipperStream.SKIPPER_PLATFORM_NAME, platformName);
		}
		if (StringUtils.hasText(repoName)) {
			propertiesToUse.put(SkipperStream.SKIPPER_REPO_NAME, repoName);
		}
		streamOperations().deploy(name, propertiesToUse);
		return String.format("Deployment request has been sent for stream '%s'", name);
	}

	@CliCommand(value = STREAM_MANIFEST_GET, help = "Get manifest for the stream deployed using Skipper")
	public String getManifest(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream", mandatory = true, optionContext = "existing-stream "
					+ "disable-string-converter") String name,
			@CliOption(key = { "releaseVersion" }, help = "the Skipper release version to get the manifest for",
					unspecifiedDefaultValue = "0") int releaseVersion) {
		return streamOperations().getManifest(name, releaseVersion);
	}

	@CliCommand(value = STREAM_HISTORY, help = "Get history for the stream deployed using Skipper")
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

	@CliCommand(value = STREAM_PLATFORM_LIST, help = "List Skipper platforms")
	public Table listPlatforms() {
		Collection<Deployer> platforms = streamOperations().listPlatforms();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Name");
		headers.put("type", "Type");
		headers.put("description", "Description");
		BeanListTableModel<Deployer> model = new BeanListTableModel<>(platforms, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model)).build();
	}

	@CliCommand(value = STREAM_UPDATE, help = "Update a previously created stream using Skipper")
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
					+ "Skipper") String repoName,
			@CliOption(key = "force", help = "force the update", mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") boolean force,
			@CliOption(key = "appNames", help = "the application names to force update", mandatory = false) String appNames)
			throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION,
				propertiesFile);
		if (StringUtils.hasText(appNames)) {
			Assert.isTrue(force, "App names can be used only when the stream update is forced.");
		}
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
		streamOperations().updateStream(name, name, packageIdentifier, propertiesToUse, force,
				new ArrayList<>(StringUtils.commaDelimitedListToSet(appNames)));
		return String.format("Update request has been sent for the stream '%s'", name);
	}

	@CliCommand(value = STREAM_SCALE, help = "Scale app instances in a stream")
	public String scaleStream(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream to scale", mandatory = true, optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = {
					"applicationName" }, help = "the name/label of the application to scale", mandatory = true) String applicationName,
			@CliOption(key = {
					"count" }, help = "desired number of application instances", mandatory = true) Integer count,
			@CliOption(key = {
					PROPERTIES_OPTION }, help = "the properties for this scale") String scaleProperties) throws IOException{

		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parseDeploymentProperties(scaleProperties,
				null, 0);
		streamOperations().scaleApplicationInstances(name, applicationName, count, propertiesToUse);
		return String.format("Scale request has been sent for the stream '%s'", name);
	}

	@CliCommand(value = STREAM_ROLLBACK, help = "Rollback a stream using Skipper")
	public String rollbackStreamUsingSkipper(
			@CliOption(key = { "", "name" }, help = "the name of the stream to rollback", mandatory = true,
					optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = { "releaseVersion" }, help = "the Skipper release version to rollback to",
					unspecifiedDefaultValue = "0") int releaseVersion) {
		this.streamOperations().rollbackStream(name, releaseVersion);
		return String.format("Rollback request has been sent for the stream '%s'", name);
	}

	@CliCommand(value = CREATE_STREAM, help = "Create a new stream definition")
	public String createStream(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the stream") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a stream definition, using the DSL (e.g. "
					+ "\"http --port=9000 | hdfs\")", optionContext = "disable-string-converter completion-stream") String dsl,
			@CliOption(mandatory = false, key = {"description"}, help = "a short description about the stream", unspecifiedDefaultValue = "") String description,
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean deploy) {
		streamOperations().createStream(name, dsl, description, deploy);
		String message = String.format("Created new stream '%s'", name);
		if (deploy) {
			message += "\nDeployment request has been sent";
		}
		return message;
	}

	@CliCommand(value = LIST_STREAM, help = "List created streams")
	public Table listStreams() {
		final PagedModel<StreamDefinitionResource> streams = streamOperations().list();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Stream Name");
		headers.put("description", "Description");
		headers.put("originalDslText", "Stream Definition");
		headers.put("statusDescription", "Status");
		BeanListTableModel<StreamDefinitionResource> model = new BeanListTableModel<>(streams, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model)).build();
	}

	@CliCommand(value = INFO_STREAM, help = "Show information about a specific stream")
	public List<Object> streamInfo(@CliOption(key = { "",
			"name" }, help = "the name of the stream to show", mandatory = true, optionContext = "existing-stream disable-string-converter") String name) {
		List<Object> result = new ArrayList<>();
		final StreamDeploymentResource stream = streamOperations().info(name);
		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Stream Name").addValue("Stream Definition").addValue("Description").addValue("Status");
		modelBuilder.addRow().addValue(stream.getStreamName())
				.addValue(stream.getDslText())
				.addValue(stream.getDescription())
				.addValue(stream.getStatus());
		TableBuilder builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));
		result.add(builder.build());
		if (StringUtils.hasText(stream.getDeploymentProperties())) {
			//TODO: rename Deployment properties for Skipper as it includes apps' info (app:version) as well
			result.add(String.format("Stream Deployment properties: %s", ShellUtils.prettyPrintIfJson(stream.getDeploymentProperties())));
		}
		return result;
	}

	@CliCommand(value = UNDEPLOY_STREAM, help = "Un-deploy a previously deployed stream")
	public String undeployStream(@CliOption(key = { "",
			"name" }, help = "the name of the stream to un-deploy", mandatory = true, optionContext = "existing-stream disable-string-converter") String name) {
		streamOperations().undeploy(name);
		return String.format("Un-deployed stream '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_STREAM_ALL, help = "Un-deploy all previously deployed stream")
	public String undeployAllStreams(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really undeploy all streams?", "n", "y", "n"))) {
			streamOperations().undeployAll();
			return String.format("Un-deployed all the streams");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = DESTROY_STREAM, help = "Destroy an existing stream")
	public String destroyStream(@CliOption(key = { "",
			"name" }, help = "the name of the stream to destroy", mandatory = true, optionContext = "existing-stream disable-string-converter") String name) {
		streamOperations().destroy(name);
		return String.format("Destroyed stream '%s'", name);
	}

	@CliCommand(value = DESTROY_STREAM_ALL, help = "Destroy all existing streams")
	public String destroyAllStreams(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all streams?", "n", "y", "n"))) {
			streamOperations().destroyAll();
			return "Destroyed all streams";
		}
		else {
			return "";
		}
	}

	@CliCommand(value = VALIDATE_STREAM, help = "Verify that apps contained in the stream are valid.")
	public List<Object> validateStream(@CliOption(key = { "",
			"name" }, help = "the name of the stream to validate", mandatory = true, optionContext = "existing-stream disable-string-converter") String name) throws OperationNotSupportedException {
		List<Object> result = new ArrayList<>();
		final StreamAppStatusResource stream = streamOperations().validateStreamDefinition(name);
		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Stream Name").addValue("Stream Definition");
		modelBuilder.addRow().addValue(stream.getAppName())
				.addValue(stream.getDsl());
		TableBuilder builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));
		result.add(builder.build());

		modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("App Name").addValue("Validation Status");
		boolean isValidStream = true;
		for(Map.Entry<String,String> entry : stream.getAppStatuses().entrySet()) {
			modelBuilder.addRow().addValue(entry.getKey())
					.addValue(entry.getValue());
			if (entry.getValue().equals("invalid")) {
				isValidStream = false;
			}
		}
		builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));

		if (isValidStream) {
			result.add(String.format("\n%s is a valid stream.", stream.getAppName()));
		}
		else {
			result.add(String.format("\n%s is an invalid stream.", stream.getAppName()));
		}
		result.add(builder.build());
		return result;
	}

	protected StreamOperations streamOperations() {
		return dataFlowShell.getDataFlowOperations().streamOperations();
	}
}
