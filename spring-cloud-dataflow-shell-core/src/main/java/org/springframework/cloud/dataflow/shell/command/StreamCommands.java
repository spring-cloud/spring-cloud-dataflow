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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.dataflow.rest.client.StreamOperations;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;
import org.springframework.cloud.dataflow.shell.command.support.YmlUtils;
import org.springframework.cloud.dataflow.shell.config.DataFlowShell;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.dataflow.rest.SkipperStream.SKIPPER_ENABLED_PROPERTY_KEY;
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
 */
@Component
// todo: reenable optionContext attributes
public class StreamCommands implements CommandMarker {

	private static final String LIST_STREAM = "stream list";

	private static final String CREATE_STREAM = "stream create";

	private static final String DEPLOY_STREAM = "stream deploy";

	private static final String STREAM_SKIPPER_DEPLOY = "stream skipper deploy";

	private static final String STREAM_SKIPPER_UPDATE = "stream skipper update";

	private static final String UNDEPLOY_STREAM = "stream undeploy";

	private static final String UNDEPLOY_STREAM_ALL = "stream all undeploy";

	private static final String DESTROY_STREAM = "stream destroy";

	private static final String DESTROY_STREAM_ALL = "stream all destroy";

	private static final String PROPERTIES_OPTION = "properties";

	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";

	private static final String YAML_OPTION = "yaml";

	private static final String YAML_FILE_OPTION = "yamlFile";

	@Autowired
	private DataFlowShell dataFlowShell;

	@Autowired
	private UserInput userInput;

	@CliAvailabilityIndicator({ LIST_STREAM })
	public boolean availableWithViewRole() {
		return dataFlowShell.hasAccess(RoleType.VIEW, OpsType.STREAM);
	}

	@CliAvailabilityIndicator({ CREATE_STREAM, DEPLOY_STREAM, STREAM_SKIPPER_DEPLOY, STREAM_SKIPPER_UPDATE,
			UNDEPLOY_STREAM, UNDEPLOY_STREAM_ALL, DESTROY_STREAM, DESTROY_STREAM_ALL })
	public boolean availableWithCreateRole() {
		return dataFlowShell.hasAccess(RoleType.CREATE, OpsType.STREAM);
	}

	@CliCommand(value = LIST_STREAM, help = "List created streams")
	public Table listStreams() {
		final PagedResources<StreamDefinitionResource> streams = streamOperations().list();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Stream Name");
		headers.put("dslText", "Stream Definition");
		headers.put("statusDescription", "Status");
		BeanListTableModel<StreamDefinitionResource> model = new BeanListTableModel<>(streams, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model)).build();
	}

	@CliCommand(value = CREATE_STREAM, help = "Create a new stream definition")
	public String createStream(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the stream") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a stream definition, using the DSL (e.g. "
					+ "\"http --port=9000 | hdfs\")", optionContext = "disable-string-converter completion-stream") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the stream immediately", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean deploy) {
		streamOperations().createStream(name, dsl, deploy);
		String message = String.format("Created new stream '%s'", name);
		if (deploy) {
			message += "\nDeployment request has been sent";
		}
		return message;
	}

	@CliCommand(value = STREAM_SKIPPER_DEPLOY, help = "Deploy a previously created stream using Skipper")
	public String deployStreamUsingSkipper(
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
		Map<String, String> propertiesToUse = getDeploymentProperties(deploymentProperties, propertiesFile, which);
		propertiesToUse.put(SKIPPER_ENABLED_PROPERTY_KEY, "true");
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

	private Map<String, String> getDeploymentProperties(@CliOption(key = {
			PROPERTIES_OPTION }, help = "the properties for this deployment") String deploymentProperties,
			@CliOption(key = {
					PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)") File propertiesFile,
			int which) throws IOException {
		Map<String, String> propertiesToUse;
		switch (which) {
		case 0:
			propertiesToUse = DeploymentPropertiesUtils.parse(deploymentProperties);
			break;
		case 1:
			String extension = FilenameUtils.getExtension(propertiesFile.getName());
			Properties props = null;
			if (extension.equals("yaml") || extension.equals("yml")) {
				YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
				yamlPropertiesFactoryBean.setResources(new FileSystemResource(propertiesFile));
				yamlPropertiesFactoryBean.afterPropertiesSet();
				props = yamlPropertiesFactoryBean.getObject();
			}
			else {
				props = new Properties();
				try (FileInputStream fis = new FileInputStream(propertiesFile)) {
					props.load(fis);
				}
			}
			propertiesToUse = DeploymentPropertiesUtils.convert(props);
			break;
		case -1: // Neither option specified
			propertiesToUse = new HashMap<>(1);
			break;
		default:
			throw new AssertionError();
		}
		return propertiesToUse;
	}

	@CliCommand(value = DEPLOY_STREAM, help = "Deploy a previously created stream")
	public String deployStream(
			@CliOption(key = { "",
					"name" }, help = "the name of the stream to deploy", mandatory = true, optionContext = "existing-stream disable-string-converter") String name,
			@CliOption(key = {
					PROPERTIES_OPTION }, help = "the properties for this deployment") String deploymentProperties,
			@CliOption(key = {
					PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)") File propertiesFile)
			throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, deploymentProperties, PROPERTIES_FILE_OPTION,
				propertiesFile);
		Map<String, String> propertiesToUse = getDeploymentProperties(deploymentProperties, propertiesFile, which);
		streamOperations().deploy(name, propertiesToUse);
		return String.format("Deployment request has been sent for stream '%s'", name);
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
		Map<String, String> propertiesToUse = getDeploymentProperties(properties, propertiesFile, which);


		//assertMutuallyExclusiveFileAndProperties(propertiesFile, properties);
		//String yamlConfigValues = getYamlConfigValues(propertiesFile, properties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(name);
		if (StringUtils.hasText(packageVersion)) {
			packageIdentifier.setPackageVersion(packageVersion);
		}
		if (StringUtils.hasText(repoName)) {
			packageIdentifier.setRepositoryName(repoName);
		}
		streamOperations().updateStream(name, name, packageIdentifier, propertiesToUse);
		return String.format("Update request has been sent for stream '%s'", name);
	}

	private void assertMutuallyExclusiveFileAndProperties(File yamlFile, String propertyString) {
		Assert.isTrue(!(yamlFile != null && propertyString != null),
				"The options " + YAML_FILE_OPTION + " and " + YAML_OPTION + "are mutually exclusive.");
		if (yamlFile != null) {
			String extension = FilenameUtils.getExtension(yamlFile.getName());
			Assert.isTrue((extension.equalsIgnoreCase("yml") || extension.equalsIgnoreCase("yaml")),
					"The YAML file should have a yml or yaml as the file extension.");
		}
	}

	private String getYamlConfigValues(File yamlFile, String yamlString) throws IOException {
		String configValuesYML = null;
		if (yamlFile != null) {
			Yaml yaml = new Yaml();
			// Validate it is yaml formatted.
			configValuesYML = yaml.dump(yaml.load(new FileInputStream(yamlFile)));
		}
		else if (StringUtils.hasText(yamlString)) {
			configValuesYML = YmlUtils.convertFromCsvToYaml(yamlString);
		}
		return configValuesYML;
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

	StreamOperations streamOperations() {
		return dataFlowShell.getDataFlowOperations().streamOperations();
	}
}
