/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.CancelRequest;
import org.springframework.cloud.skipper.domain.CancelResponse;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.shell.command.support.DeploymentStateDisplay;
import org.springframework.cloud.skipper.shell.command.support.TableUtils;
import org.springframework.cloud.skipper.shell.command.support.YmlUtils;
import org.springframework.cloud.skipper.support.DurationUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The main skipper commands that deal with releases.
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 */
@ShellComponent
public class ReleaseCommands extends AbstractSkipperCommand {

	private static final Logger logger = LoggerFactory.getLogger(ReleaseCommands.class);

	public ReleaseCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	/**
	 * Aggregate the set of app states into a single state for a stream.
	 *
	 * @param states set of states for apps of a stream
	 * @return the stream state based on app states
	 */
	public static DeploymentState aggregateState(List<DeploymentState> states) {
		if (states.size() == 1) {
			DeploymentState state = states.iterator().next();
			logger.debug("aggregateState: Deployment State Set Size = 1.  Deployment State " + state);
			// a stream which is known to the stream definition repository
			// but unknown to deployers is undeployed
			if (state == DeploymentState.unknown) {
				logger.debug("aggregateState: Returning " + DeploymentState.undeployed);
				return DeploymentState.undeployed;
			}
			else {
				logger.debug("aggregateState: Returning " + state);
				return state;
			}
		}
		if (states.isEmpty() || states.contains(DeploymentState.error)) {
			logger.debug("aggregateState: Returning " + DeploymentState.error);
			return DeploymentState.error;
		}
		if (states.contains(DeploymentState.failed)) {
			logger.debug("aggregateState: Returning " + DeploymentState.failed);
			return DeploymentState.failed;
		}
		if (states.contains(DeploymentState.deploying)) {
			logger.debug("aggregateState: Returning " + DeploymentState.deploying);
			return DeploymentState.deploying;
		}

		if (allAppsDeployed(states)) {
			return DeploymentState.deployed;
		}

		logger.debug("aggregateState: Returning " + DeploymentState.partial);
		return DeploymentState.partial;
	}

	private static boolean allAppsDeployed(List<DeploymentState> deploymentStateList) {
		boolean allDeployed = true;
		for (DeploymentState deploymentState : deploymentStateList) {
			if (deploymentState != DeploymentState.deployed) {
				allDeployed = false;
				break;
			}
		}
		return allDeployed;
	}

	@ShellMethod(key = "release upgrade", value = "Upgrade a release.")
	public Object upgrade(
			@ShellOption(help = "the name of the release to upgrade") String releaseName,
			@ShellOption(help = "the name of the package to use for the upgrade") String packageName,
			@ShellOption(help = "the version of the package to use for the upgrade, if not specified latest version will be used", defaultValue = ShellOption.NULL) String packageVersion,
			@ShellOption(help = "specify values in a YAML file", defaultValue = ShellOption.NULL) File file,
			@ShellOption(help = "the expression for upgrade timeout", defaultValue = ShellOption.NULL) String timeoutExpression,
			@ShellOption(help = "the comma separated set of properties to override during upgrade", defaultValue = ShellOption.NULL) String properties,
			@ShellOption(help = "force upgrade") boolean force,
			@ShellOption(help = "application names to force upgrade. If no specific list is provided, all the apps in the packages are force upgraded",
					defaultValue = ShellOption.NULL) String appNames)
			throws IOException {
		// Commented out until https://github.com/spring-cloud/spring-cloud-skipper/issues/263 is
		// addressed
		// assertMutuallyExclusiveFileAndProperties(file, properties);
		if (StringUtils.hasText(appNames)) {
			Assert.isTrue(force, "App names can be used only when the stream update is forced.");
		}
		Release release = skipperClient
				.upgrade(getUpgradeRequest(releaseName, packageName, packageVersion, file, properties, timeoutExpression, force, appNames));
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been upgraded.  Now at version v" + release.getVersion() + ".");
		return sb.toString();
	}

	private void updateStatus(StringBuilder sb, Release release) {
		sb.append("Release Status: " + release.getInfo().getStatus().getStatusCode() + "\n");
		if (StringUtils.hasText(release.getInfo().getStatus().getPlatformStatus())) {
			sb.append("Platform Status: " + release.getInfo().getStatus().getPlatformStatusPrettyPrint());
		}
		else {
			sb.append("Platform Status: unknown");
		}
	}

	private void assertMutuallyExclusiveFileAndProperties(File yamlFile, String properties) {
		Assert.isTrue(!(yamlFile != null && properties != null), "The options 'file' and 'properties' options "
				+ "are mutually exclusive.");
		if (yamlFile != null) {
			String extension = FilenameUtils.getExtension(yamlFile.getName());
			Assert.isTrue((extension.equalsIgnoreCase("yml") || extension.equalsIgnoreCase("yaml")),
					"The file should be YAML file");
		}
	}

	private UpgradeRequest getUpgradeRequest(String releaseName, String packageName, String packageVersion,
			File propertiesFile, String propertiesToOverride, String timeoutExpression, boolean forceUpgrade, String appNames) throws IOException {
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setForce(forceUpgrade);
		upgradeRequest.setAppNames(new ArrayList<>(StringUtils.commaDelimitedListToSet(appNames)));
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		String configValuesYML = YmlUtils.getYamlConfigValues(propertiesFile, propertiesToOverride);
		if (StringUtils.hasText(configValuesYML)) {
			ConfigValues configValues = new ConfigValues();
			configValues.setRaw(configValuesYML);
			upgradeProperties.setConfigValues(configValues);
		}
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		Duration duration = DurationUtils.convert(timeoutExpression);
		if (duration != null) {
			upgradeRequest.setTimeout(duration.toMillis());
		}
		return upgradeRequest;
	}

	@ShellMethod(key = "release rollback", value = "Rollback the release to a previous or a specific release.")
	public String rollback(
			@ShellOption(help = "the name of the release to rollback") String releaseName,
			@ShellOption(help = "the specific release version to rollback to. " +
					"Not specifying the value rolls back to the previous release.", defaultValue = "0") int releaseVersion,
			@ShellOption(help = "the expression for rollback timeout", defaultValue = ShellOption.NULL) String timeoutExpression) {

		RollbackRequest rollbackRequest = new RollbackRequest(releaseName, releaseVersion);
		Duration duration = DurationUtils.convert(timeoutExpression);
		if (duration != null) {
			rollbackRequest.setTimeout(duration.toMillis());
		}

		Release release = skipperClient.rollback(rollbackRequest);
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been rolled back.  Now at version v" + release.getVersion() + ".");
		return sb.toString();
	}

	@ShellMethod(key = "release delete", value = "Delete the release.")
	public String delete(
			@ShellOption(help = "the name of the release to delete") String releaseName,
			@ShellOption(help = "delete the release package", defaultValue = "false") boolean deletePackage) {
		this.skipperClient.delete(releaseName, deletePackage);
		StringBuilder sb = new StringBuilder();
		sb.append(releaseName + " has been deleted.");
		return sb.toString();
	}

	@ShellMethod(key = "release cancel", value = "Request a cancellation of current release operation.")
	public String cancel(
			@ShellOption(help = "the name of the release to cancel") String releaseName) {
		CancelResponse cancelResponse = this.skipperClient.cancel(new CancelRequest(releaseName));
		if (cancelResponse != null && cancelResponse.getAccepted() != null && cancelResponse.getAccepted()) {
			return "Cancel request for release " + releaseName + " sent";
		}
		throw new SkipperException("Cancel request for release " + releaseName + " not accepted");
	}

	@ShellMethod(key = "release list", value = "List the latest version of releases with status of deployed or failed.")
	public Table list(
			@ShellOption(help = "wildcard expression to search by release name", defaultValue = ShellOption.NULL) String releaseName) {
		List<Release> releases = this.skipperClient.list(releaseName);
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Name");
		headers.put("version", "Version");
		headers.put("info.lastDeployed", "Last updated");
		headers.put("info.status.statusCode", "Status");
		headers.put("pkg.metadata.name", "Package Name");
		headers.put("pkg.metadata.version", "Package Version");
		headers.put("platformName", "Platform Name");
		headers.put("info.status.platformStatusPrettyPrint", "Platform Status");
		TableModel model = new BeanListTableModel<>(releases, headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		TableUtils.applyStyle(tableBuilder);
		return tableBuilder.build();
	}

	@ShellMethod(key = "release history", value = "List the history of versions for a given release.")
	public Table history(
			@ShellOption(help = "wildcard expression to search by release name") @NotNull String releaseName) {
		Collection<Release> releases;
		releases = this.skipperClient.history(releaseName);
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("version", "Version");
		headers.put("info.lastDeployed", "Last updated");
		headers.put("info.status.statusCode", "Status");
		headers.put("pkg.metadata.name", "Package Name");
		headers.put("pkg.metadata.version", "Package Version");
		headers.put("info.description", "Description");
		TableModel model = new BeanListTableModel<>(releases, headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		TableUtils.applyStyle(tableBuilder);
		return tableBuilder.build();
	}

	@ShellMethod(key = "release status", value = "Status for a last known release version.")
	public Object status(
			@ShellOption(help = "release name") @NotNull String releaseName,
			@ShellOption(help = "the specific release version.", defaultValue = ShellOption.NULL) Integer releaseVersion) {
		Info info;
		try {
			if (releaseVersion == null) {
				info = this.skipperClient.status(releaseName);
			}
			else {
				info = this.skipperClient.status(releaseName, releaseVersion);
			}
		}
		catch (ReleaseNotFoundException e) {
			return "Release with name '" + e.getReleaseName() + "' not found";
		}
		Object[][] data = new Object[3][];
		data[0] = new Object[] { "Last Deployed", info.getFirstDeployed() };
		data[1] = new Object[] { "Status", info.getStatus().getStatusCode().toString() };

		DeploymentState aggregateState = aggregateState(info.getStatus().getDeploymentStateList());
		StringBuilder sb = new StringBuilder();
		sb.append(DeploymentStateDisplay.fromKey(aggregateState.name()).getDescription() + "\n");
		sb.append(info.getStatus().getPlatformStatusPrettyPrint());
		data[2] = new Object[] { "Platform Status", sb.toString() };
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		TableUtils.applyStyleNoHeader(tableBuilder);
		return tableBuilder.build();
	}

	private void assertMaxIsIntegerAndGreaterThanZero(String max) {
		try {
			int maxInt = Integer.parseInt(max);
			Assert.isTrue(maxInt > 0, "The maximum number of revisions should be greater than zero.");
		}
		catch (NumberFormatException e) {
			throw new NumberFormatException("The maximum number of revisions is not an integer. Input string = " + max);
		}
	}

}
