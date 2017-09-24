/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.DeployProperties;
import org.springframework.cloud.skipper.domain.DeployRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpdateProperties;
import org.springframework.cloud.skipper.domain.UpdateRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.shell.command.support.TableUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.hateoas.Resources;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import static org.springframework.shell.standard.ShellOption.NULL;

/**
 * @author Ilayaperumal Gopinathan
 */
@ShellComponent
public class PackageCommands extends AbstractSkipperCommand {

	@Autowired
	public PackageCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = "package search", value = "Search for the packages")
	public Object searchPackage(
			@ShellOption(help = "wildcard expression to search for the package name", defaultValue = NULL) String name,
			@ShellOption(help = "boolean to set for more detailed package metadata") boolean details)
			throws Exception {
		Resources<PackageMetadata> resources = skipperClient.getPackageMetadata(name, details);
		if (!details) {
			LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
			headers.put("name", "Name");
			headers.put("version", "Version");
			headers.put("description", "Description");
			TableModel model = new BeanListTableModel<>(resources.getContent(), headers);
			TableBuilder tableBuilder = new TableBuilder(model);
			TableUtils.applyStyle(tableBuilder);
			return tableBuilder.build();
		}
		else {
			ObjectMapper mapper = new ObjectMapper();
			PackageMetadata[] packageMetadataResources = resources.getContent().toArray(new PackageMetadata[0]);
			List<Table> tableList = new ArrayList<>();
			for (int i = 0; i < resources.getContent().size(); i++) {
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

	@ShellMethod(key = "package deploy", value = "Deploy a package")
	public String deploy(
			@ShellOption(help = "name of the package to deploy") String name,
			@ShellOption(help = "version of the package to deploy", defaultValue = NULL) String version,
			// TODO specify a specific package repository
			@ShellOption(help = "the properties file to use to deploy", defaultValue = NULL) File propertiesFile,
			// TODO support generation of a release name
			@ShellOption(help = "the release name to use") String releaseName,
			@ShellOption(help = "the platform name to use", defaultValue = "default") String platformName)
			throws IOException {
		Release release = skipperClient
				.deploy(getDeployRequest(name, version, propertiesFile, releaseName, platformName));
		return "Released " + release.getName();
	}

	@ShellMethod(key = "package update", value = "Update a package")
	public String update(
			@ShellOption(help = "the name of the release to update") String releaseName,
			@ShellOption(help = "the name of the package to use for the update") String packageName,
			@ShellOption(help = "the version of the package to use for the update") String packageVersion,
			@ShellOption(help = "the properties file to use to deploy", defaultValue = NULL) File propertiesFile)
			throws IOException {
		Release release = skipperClient
				.update(getUpdateRequest(releaseName, packageName, packageVersion, propertiesFile));
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been updated.\n");
		sb.append("Last Deployed: " + release.getInfo().getLastDeployed() + "\n");
		sb.append("Status: " + release.getInfo().getStatus().getPlatformStatus() + "\n");
		return sb.toString();
	}

	@ShellMethod(key = "package rollback", value = "Rollback the package to a previous release")
	public String rollback(
			@ShellOption(help = "the name of the release to rollback") String releaseName,
			@ShellOption(help = "the specific release version to rollback to. " +
					"Not specifying the value rolls back to the previous release.", defaultValue = "0") int releaseVersion) {
		Release release = skipperClient.rollback(releaseName, releaseVersion);
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been rolled back.\n");
		sb.append("Last Deployed: " + release.getInfo().getLastDeployed() + "\n");
		sb.append("Status: " + release.getInfo().getStatus().getPlatformStatus() + "\n");
		return sb.toString();
	}

	@ShellMethod(key = "package undeploy", value = "Undeploy the package")
	public String undeploy(
			@ShellOption(help = "the name of the release to undeploy") String releaseName) {
		Release release = skipperClient.undeploy(releaseName);
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been undeployed.\n");
		return sb.toString();
	}

	private UpdateRequest getUpdateRequest(String releaseName, String packageName, String packageVersion,
			File propertiesFile) throws IOException {
		UpdateRequest updateRequest = new UpdateRequest();
		UpdateProperties updateProperties = new UpdateProperties();
		updateProperties.setReleaseName(releaseName);
		// TODO support config values from propertiesFile.
		// updateProperties.setConfigValues();
		updateRequest.setUpdateProperties(updateProperties);

		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		updateRequest.setPackageIdentifier(packageIdentifier);
		updateRequest.setPackageIdentifier(packageIdentifier);
		return updateRequest;
	}

	private DeployRequest getDeployRequest(String packageName, String packageVersion, File propertiesFile,
			String releaseName, String platformName) throws IOException {
		DeployProperties deployProperties = getDeployProperties(releaseName, platformName, propertiesFile);
		DeployRequest deployRequest = new DeployRequest();
		deployRequest.setDeployProperties(deployProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		deployRequest.setPackageIdentifier(packageIdentifier);
		return deployRequest;
	}

	@ShellMethod(key = "package upload", value = "Upload a package")
	public String upload(@ShellOption(help = "the package to be uploaded") String path,
			@ShellOption(help = "the local repository name to upload to", defaultValue = NULL) String repoName) {
		UploadRequest properties = new UploadRequest();
		try {
			File file = ResourceUtils.getFile(path);
			StringTokenizer tokenizer = new StringTokenizer(file.getName(), "-");
			String fileName = (String) tokenizer.nextElement();
			String versionAndExtension = (String) tokenizer.nextElement();
			String extension = versionAndExtension.substring(versionAndExtension.lastIndexOf("."));
			String version = versionAndExtension.replaceAll(extension, "");
			properties.setName(fileName);
			properties.setVersion(version);
			properties.setExtension(extension);
			properties.setRepoName(StringUtils.hasText(repoName) ? repoName : "local");
			properties.setPackageFileAsBytes(Files.readAllBytes(file.toPath()));
		}
		catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File Not found: " + e.getMessage());
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		PackageMetadata packageMetadata = skipperClient.upload(properties);
		return "Package uploaded successfully:[" + packageMetadata.getName() + ":" + packageMetadata.getVersion() + "]";
	}

	private DeployProperties getDeployProperties(String releaseName, String platformName, File propertiesFile)
			throws IOException {
		DeployProperties deployProperties = new DeployProperties();
		if (StringUtils.hasText(releaseName)) {
			deployProperties.setReleaseName(releaseName);
			deployProperties.setPlatformName(platformName);
		}
		else {
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
			if (props != null) {
				Assert.notNull(props.getProperty("release-name"), "Release name must not be null");
				// TODO why set these here?
				deployProperties.setReleaseName(props.getProperty("release-name"));
				deployProperties.setPlatformName(props.getProperty("platform-name", platformName));
				// TODO support config values from propertiesFile
				// deployProperties.setConfigValues();
			}
		}
		return deployProperties;
	}

}
