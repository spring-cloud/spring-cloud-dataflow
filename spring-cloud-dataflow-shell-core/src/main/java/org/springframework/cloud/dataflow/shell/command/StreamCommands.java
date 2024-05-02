/*
 * Copyright 2018-2024 the original author or authors.
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
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.rest.client.StreamOperations;
import org.springframework.cloud.dataflow.rest.resource.StreamAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.command.support.ShellUtils;
import org.springframework.cloud.dataflow.shell.command.support.TablesInfo;
import org.springframework.cloud.dataflow.shell.completer.StreamNameValueProvider;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.hateoas.PagedModel;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.shell.table.TableModelBuilder;
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
 * @author Chris Bono
 */
@ShellComponent
public class StreamCommands {

	private DataFlowShell dataFlowShell;
	private ConsoleUserInput userInput;

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

	public StreamCommands(DataFlowShell dataFlowShell, ConsoleUserInput userInput) {
		this.dataFlowShell = dataFlowShell;
		this.userInput = userInput;
	}

	public Availability availableWithCreateRole() {
		return availabilityFor(RoleType.CREATE, OpsType.STREAM);
	}

	public Availability availableWithDeployRole() {
		return availabilityFor(RoleType.DEPLOY, OpsType.STREAM);
	}

	public Availability availableWithDestroyRole() {
		return availabilityFor(RoleType.DESTROY, OpsType.STREAM);
	}

	public Availability availableWithModifyRole() {
		return availabilityFor(RoleType.MODIFY, OpsType.STREAM);
	}

	public Availability availableWithViewRole() {
		return availabilityFor(RoleType.VIEW, OpsType.STREAM);
	}

	private Availability availabilityFor(RoleType roleType, OpsType opsType) {
		return dataFlowShell.hasAccess(roleType, opsType)
				? Availability.available()
				: Availability.unavailable("you do not have permissions");
	}

	@ShellMethod(key = STREAM_DEPLOY, value = "Deploy a previously created stream using Skipper")
	@ShellMethodAvailability("availableWithDeployRole")
	public String deployStream(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream to deploy", valueProvider = StreamNameValueProvider.class) String name,
			@ShellOption(value = "--properties", help = "the properties for this deployment", defaultValue = ShellOption.NULL) String deploymentProperties,
			@ShellOption(value = "--propertiesFile", help = "the properties for this deployment (as a File)", defaultValue = ShellOption.NULL) File propertiesFile,
			@ShellOption(value = "--packageVersion", help = "the package version of the package to deploy.  Default is 1.0.0", defaultValue = "1.0.0") String packageVersion,
			@ShellOption(value = "--platformName", help = "the name of the target platform to deploy to", defaultValue = ShellOption.NULL) String platformName,
			@ShellOption(value = "--repoName", help = "the name of the local repository to upload the package to", defaultValue = ShellOption.NULL) String repoName)
			throws IOException {
		int which = Assertions.atMostOneOf("--properties", deploymentProperties, "--propertiesFile",
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

	@ShellMethod(key = STREAM_MANIFEST_GET, value = "Get manifest for the stream deployed using Skipper")
	@ShellMethodAvailability("availableWithViewRole")
	public String getManifest(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream", valueProvider = StreamNameValueProvider.class) String name,
			@ShellOption(value = "--releaseVersion", help = "the Skipper release version to get the manifest for",
					defaultValue = "0") int releaseVersion) {
		return streamOperations().getManifest(name, releaseVersion);
	}

	@ShellMethod(key = STREAM_HISTORY, value = "Get history for the stream deployed using Skipper")
	@ShellMethodAvailability("availableWithViewRole")
	public Table history(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream", valueProvider = StreamNameValueProvider.class) String name) {
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

	@ShellMethod(key = STREAM_PLATFORM_LIST, value = "List Skipper platforms")
	@ShellMethodAvailability("availableWithViewRole")
	public Table listPlatforms() {
		Collection<Deployer> platforms = streamOperations().listPlatforms();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Name");
		headers.put("type", "Type");
		headers.put("description", "Description");
		BeanListTableModel<Deployer> model = new BeanListTableModel<>(platforms, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model)).build();
	}

	@ShellMethod(key = STREAM_UPDATE, value = "Update a previously created stream using Skipper")
	@ShellMethodAvailability("availableWithModifyRole")
	public String updateStream(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream", valueProvider = StreamNameValueProvider.class) String name,
			@ShellOption(value = "--properties", help = "Flattened YAML style properties to update the stream", defaultValue = ShellOption.NULL) String properties,
			@ShellOption(value = "--propertiesFile", help = "the properties for the stream update (as a File)", defaultValue = ShellOption.NULL) File propertiesFile,
			@ShellOption(value = "--packageVersion", help = "the package version of the package to update when using Skipper", defaultValue = ShellOption.NULL) String packageVersion,
			@ShellOption(value = "--repoName", help = "the name of the local repository to upload the package when using Skipper", defaultValue = ShellOption.NULL) String repoName,
			@ShellOption(help = "force the update", defaultValue = "false") boolean force,
			@ShellOption(value = "--appNames", help = "the application names to force update", defaultValue = ShellOption.NULL) String appNames)
			throws IOException {
		int which = Assertions.atMostOneOf("--properties", properties, "--propertiesFile", propertiesFile);
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

	@ShellMethod(key = STREAM_SCALE, value = "Scale app instances in a stream")
	@ShellMethodAvailability("availableWithModifyRole")
	public String scaleStream(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream to scale", valueProvider = StreamNameValueProvider.class) String name,
			@ShellOption(value = "--applicationName", help = "the name/label of the application to scale") String applicationName,
			@ShellOption(help = "desired number of application instances") Integer count,
			@ShellOption(value = "--properties", help = "the properties for this scale", defaultValue =  ShellOption.NULL) String scaleProperties) throws IOException{

		Map<String, String> propertiesToUse = DeploymentPropertiesUtils.parseDeploymentProperties(scaleProperties,
				null, 0);
		streamOperations().scaleApplicationInstances(name, applicationName, count, propertiesToUse);
		return String.format("Scale request has been sent for the stream '%s'", name);
	}

	@ShellMethod(key = STREAM_ROLLBACK, value = "Rollback a stream using Skipper")
	@ShellMethodAvailability("availableWithModifyRole")
	public String rollbackStreamUsingSkipper(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream to rollback", valueProvider = StreamNameValueProvider.class) String name,
			@ShellOption(value = "--releaseVersion", help = "the Skipper release version to rollback to", defaultValue = "0") int releaseVersion) {
		this.streamOperations().rollbackStream(name, releaseVersion);
		return String.format("Rollback request has been sent for the stream '%s'", name);
	}

	@ShellMethod(key = CREATE_STREAM, value = "Create a new stream definition")
	@ShellMethodAvailability("availableWithCreateRole")
	public String createStream(
			@ShellOption(value = { "", "--name" }, help = "the name to give to the stream") String name,
			@ShellOption(value = { "--definition" }, help = "a stream definition, using the DSL (e.g. \"http --port=9000 | hdfs\")") String dsl,
			@ShellOption(help = "a short description about the stream", defaultValue = "") String description,
			@ShellOption(help = "whether to deploy the stream immediately", defaultValue = "false") boolean deploy) {
		streamOperations().createStream(name, dsl, description, deploy);
		String message = String.format("Created new stream '%s'", name);
		if (deploy) {
			message += "\nDeployment request has been sent";
		}
		return message;
	}

	@ShellMethod(key = LIST_STREAM, value = "List created streams")
	@ShellMethodAvailability("availableWithViewRole")
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

	@ShellMethod(key = INFO_STREAM, value = "Show information about a specific stream")
	@ShellMethodAvailability("availableWithViewRole")
	public TablesInfo streamInfo(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream to show",
					valueProvider = StreamNameValueProvider.class) String name) {
		TablesInfo result = new TablesInfo();
		final StreamDeploymentResource stream = streamOperations().info(name);
		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Stream Name").addValue("Stream Definition").addValue("Description").addValue("Status");
		modelBuilder.addRow().addValue(stream.getStreamName())
				.addValue(stream.getDslText())
				.addValue(stream.getDescription())
				.addValue(stream.getStatus());
		TableBuilder builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));
		result.addTable(builder.build());
		if (StringUtils.hasText(stream.getDeploymentProperties())) {
			//TODO: rename Deployment properties for Skipper as it includes apps' info (app:version) as well
			result.addFooter(String.format("Stream Deployment properties: %s", ShellUtils.prettyPrintIfJson(stream.getDeploymentProperties())));
		}
		return result;
	}

	@ShellMethod(key = UNDEPLOY_STREAM, value = "Un-deploy a previously deployed stream")
	@ShellMethodAvailability("availableWithDeployRole")
	public String undeployStream(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream to un-deploy",
					valueProvider = StreamNameValueProvider.class) String name) {
		streamOperations().undeploy(name);
		return String.format("Un-deployed stream '%s'", name);
	}

	@ShellMethod(key = UNDEPLOY_STREAM_ALL, value = "Un-deploy all previously deployed stream")
	@ShellMethodAvailability("availableWithDeployRole")
	public String undeployAllStreams(
			@ShellOption(help = "bypass confirmation prompt", defaultValue = "false") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really undeploy all streams?", "n", "y", "n"))) {
			streamOperations().undeployAll();
			return String.format("Un-deployed all the streams");
		}
		else {
			return "";
		}
	}

	@ShellMethod(key = DESTROY_STREAM, value = "Destroy an existing stream")
	@ShellMethodAvailability("availableWithDestroyRole")
	public String destroyStream(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream to destroy",
					valueProvider = StreamNameValueProvider.class) String name) {
		streamOperations().destroy(name);
		return String.format("Destroyed stream '%s'", name);
	}

	@ShellMethod(key = DESTROY_STREAM_ALL, value = "Destroy all existing streams")
	@ShellMethodAvailability("availableWithDestroyRole")
	public String destroyAllStreams(
			@ShellOption(help = "bypass confirmation prompt", defaultValue = "false") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all streams?", "n", "y", "n"))) {
			streamOperations().destroyAll();
			return "Destroyed all streams";
		}
		else {
			return "";
		}
	}

	@ShellMethod(key = VALIDATE_STREAM, value = "Verify that apps contained in the stream are valid.")
	@ShellMethodAvailability("availableWithViewRole")
	public TablesInfo validateStream(
			@ShellOption(value = { "", "--name" }, help = "the name of the stream to validate",
					valueProvider = StreamNameValueProvider.class) String name) throws OperationNotSupportedException {
		TablesInfo result = new TablesInfo();
		final StreamAppStatusResource stream = streamOperations().validateStreamDefinition(name);
		TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
		modelBuilder.addRow().addValue("Stream Name").addValue("Stream Definition");
		modelBuilder.addRow().addValue(stream.getAppName())
				.addValue(stream.getDsl());
		TableBuilder builder = DataFlowTables.applyStyle(new TableBuilder(modelBuilder.build()));
		result.addTable(builder.build());

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
			result.addFooter(String.format("\n%s is a valid stream.", stream.getAppName()));
		}
		else {
			result.addFooter(String.format("\n%s is an invalid stream.", stream.getAppName()));
		}
		result.addTable(builder.build());
		return result;
	}

	protected StreamOperations streamOperations() {
		return dataFlowShell.getDataFlowOperations().streamOperations();
	}
}
