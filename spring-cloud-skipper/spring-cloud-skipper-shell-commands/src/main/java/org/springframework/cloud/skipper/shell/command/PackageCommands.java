/*
 * Copyright 2018-2020 the original author or authors.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.shell.command.support.TableUtils;
import org.springframework.cloud.skipper.shell.command.support.YmlUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Commands related to packages.
 * @author Mark Pollack
 */
@ShellComponent
public class PackageCommands extends AbstractSkipperCommand {

	private static final Logger logger = LoggerFactory.getLogger(ReleaseCommands.class);

	public PackageCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = {"package search", "package list"}, value = "Search for packages.")
	public Object search(
			@ShellOption(help = "wildcard expression to search for the package name", defaultValue = ShellOption.NULL) String name,
			@ShellOption(help = "boolean to set for more detailed package metadata") boolean details)
			throws Exception {
		Collection<PackageMetadata> resources = skipperClient.search(name, details);
		if (!details) {
			LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
			headers.put("name", "Name");
			headers.put("version", "Version");
			headers.put("description", "Description");
			TableModel model = new BeanListTableModel<>(resources, headers);
			TableBuilder tableBuilder = new TableBuilder(model);
			TableUtils.applyStyle(tableBuilder);
			return tableBuilder.build();
		}
		else {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			PackageMetadata[] packageMetadataResources = resources.toArray(new PackageMetadata[0]);
			List<Table> tableList = new ArrayList<>();
			for (int i = 0; i < resources.size(); i++) {
				String json = mapper.writeValueAsString(packageMetadataResources[i]);
				Map<String, String> map = mapper.readValue(json, new TypeReference<Map<String, String>>() {
				});
				map.remove("id");
				LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
				headers.put("key", "Name");
				headers.put("value", "Value");
				TableModel model = new BeanListTableModel<>(map.entrySet(), headers);
				TableBuilder tableBuilder = new TableBuilder(model);
				TableUtils.applyStyle(tableBuilder);
				tableList.add(tableBuilder.build());
			}
			return tableList;
		}
	}

	@ShellMethod(key = "package upload", value = "Upload a package.")
	public String upload(@ShellOption(help = "the package to be uploaded") String path,
			@ShellOption(help = "the local repository name to upload to", defaultValue = ShellOption.NULL) String repoName) {
		UploadRequest uploadRequest = new UploadRequest();
		try {
			File file = ResourceUtils.getFile(path);
			String zipFileName = file.getName();
			String fileName = zipFileName.substring(0, zipFileName.lastIndexOf("-"));
			String versionAndExtension = zipFileName.substring(fileName.length() + 1);
			String extension = versionAndExtension.substring(versionAndExtension.lastIndexOf(".") + 1);
			String version = versionAndExtension.replaceAll("." + extension, "");
			uploadRequest.setName(fileName);
			uploadRequest.setVersion(version);
			uploadRequest.setExtension(extension);
			uploadRequest.setRepoName(StringUtils.hasText(repoName) ? repoName : "local");
			uploadRequest.setPackageFileAsBytes(Files.readAllBytes(file.toPath()));
		}
		catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File Not found: " + e.getMessage());
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		PackageMetadata packageMetadata = skipperClient.upload(uploadRequest);
		return "Package uploaded successfully:[" + packageMetadata.getName() + ":" + packageMetadata.getVersion() + "]";
	}

	@ShellMethod(key = "package install", value = "Install a package.")
	public String install(
			@ShellOption(help = "name of the package to install") String packageName,
			@ShellOption(help = "version of the package to install, if not specified latest version will be used", defaultValue = ShellOption.NULL) String packageVersion,
			@ShellOption(help = "specify values in a YAML file", defaultValue = ShellOption.NULL) File file,
			@ShellOption(help = "the comma separated set of properties to override during install", defaultValue = ShellOption.NULL) String properties,
			@ShellOption(help = "the release name to use") String releaseName,
			@ShellOption(help = "the platform name to use", defaultValue = "default") String platformName)
			throws IOException {
		// Commented out until https://github.com/spring-cloud/spring-cloud-skipper/issues/263 is
		// addressed
		// assertMutuallyExclusiveFileAndProperties(file, properties);
		Release release = skipperClient
				.install(getInstallRequest(packageName, packageVersion, file, properties, releaseName, platformName));
		return "Released " + release.getName() + ". Now at version v" + release.getVersion() + ".";
	}

	@ShellMethod(key = "package delete", value = "Delete a package.")
	public String packageDelete(@ShellOption(help = "the package name to be deleted") String packageName) {
		try {
			this.skipperClient.packageDelete(packageName);
		}
		catch (PackageDeleteException e) {
			return e.getMessage();
		}
		return String.format("Deleted Package '%s'", packageName);
	}

	private InstallRequest getInstallRequest(String packageName, String packageVersion, File yamlFile,
			String properties, String releaseName, String platformName) throws IOException {
		InstallProperties installProperties = getInstallProperties(releaseName, platformName, yamlFile,
				properties);
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(installProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		installRequest.setPackageIdentifier(packageIdentifier);
		return installRequest;
	}

	private InstallProperties getInstallProperties(String releaseName, String platformName, File yamlFile,
			String propertiesToOverride) throws IOException {
		InstallProperties installProperties = new InstallProperties();
		if (StringUtils.hasText(releaseName)) {
			installProperties.setReleaseName(releaseName);
		}
		// There is a 'default' value for platformName
		installProperties.setPlatformName(platformName);
		String configValuesYML = YmlUtils.getYamlConfigValues(yamlFile, propertiesToOverride);
		if (StringUtils.hasText(configValuesYML)) {
			ConfigValues configValues = new ConfigValues();
			configValues.setRaw(configValuesYML);
			installProperties.setConfigValues(configValues);
		}
		return installProperties;
	}

}
