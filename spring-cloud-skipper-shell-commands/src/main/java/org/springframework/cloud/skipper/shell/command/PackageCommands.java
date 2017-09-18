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
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
import org.springframework.cloud.skipper.shell.command.support.SkipperClientUpdatedEvent;
import org.springframework.cloud.skipper.shell.command.support.TableUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.hateoas.Resources;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.Assert;

import static org.springframework.shell.standard.ShellOption.NULL;

/**
 * @author Ilayaperumal Gopinathan
 */
@ShellComponent
public class PackageCommands {

	private SkipperClient skipperClient;

	@Autowired
	public PackageCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = "package search", value = "Search for the packages")
	public String searchPackage(
			@ShellOption(help = "wildcard expression to search for the package name", defaultValue = NULL) String name,
			@ShellOption(help = "boolean to set for more detailed package metadata") boolean details)
			throws Exception {
		Resources<PackageMetadata> resources = skipperClient.getPackageMetadata(name, details);
		TableBuilder tableBuilder = null;
		if (!details) {
			LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
			headers.put("name", "Name");
			headers.put("version", "Version");
			headers.put("description", "Description");
			TableModel model = new BeanListTableModel<>(resources.getContent(), headers);
			tableBuilder = new TableBuilder(model);
			TableUtils.applyStyle(tableBuilder);
			// TODO is there a better way to get the current terminal width?
			return tableBuilder.build().render(132);
		}
		else {
			ObjectMapper mapper = new ObjectMapper();
			String[][] data = new String[resources.getContent().size()][1];
			PackageMetadata[] packageMetadataResources = resources.getContent().toArray(new PackageMetadata[0]);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < resources.getContent().size(); i++) {
				sb.append("---\n");
				String json = mapper.writeValueAsString(packageMetadataResources[i]);
				Map<String, Object> map = mapper.readValue(json, new TypeReference<Map<String, String>>() {
				});
				DumperOptions dumperOptions = new DumperOptions();
				dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
				dumperOptions.setPrettyFlow(true);
				Yaml yaml = new Yaml(dumperOptions);
				map.remove("id");
				String yamlString = yaml.dump(map);
				sb.append(yamlString);
			}
			sb.append("---\n");
			return sb.toString();
		}

	}

	@ShellMethod(key = "package deploy", value = "Deploy the package metadata")
	public String deploy(
			@ShellOption(help = "packageId of the package metadata to deploy", defaultValue = NULL) String packageId,
			@ShellOption(help = "the properties file to use to deploy", defaultValue = NULL) File propertiesFile,
			@ShellOption(help = "the release name to use", defaultValue = NULL) String releaseName,
			@ShellOption(help = "the platform name to use", defaultValue = "default") String platformName)
			throws IOException {
		// todo: Make releaseName & propertiesFile mutually exclusive
		Assert.notNull(packageId, "Package Id must not be null");
		return skipperClient.deploy(packageId, getDeployProperties(releaseName, platformName, propertiesFile));
	}

	@ShellMethod(key = "package update", value = "Update a specific release")
	public String update(
			@ShellOption(help = "the package id to update", defaultValue = NULL) String packageId,
			@ShellOption(help = "the properties file to use to deploy", defaultValue = NULL) File propertiesFile,
			@ShellOption(help = "the release name to use", defaultValue = NULL) String releaseName,
			@ShellOption(help = "the platform name to use", defaultValue = "default") String platformName)
			throws IOException {
		Assert.notNull(packageId, "Package Id must not be null");
		return skipperClient.update(packageId, getDeployProperties(releaseName, platformName, propertiesFile));
	}

	private DeployProperties getDeployProperties(String releaseName, String platformName, File propertiesFile)
			throws IOException {
		DeployProperties deployProperties = new DeployProperties();
		if (releaseName != null) {
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
				deployProperties.setReleaseName(props.getProperty("release-name"));
				deployProperties.setPlatformName(props.getProperty("platform-name", platformName));
				// todo: support config values
				// deployProperties.setConfigValues();
			}
		}
		return deployProperties;
	}

	@EventListener
	void handle(SkipperClientUpdatedEvent event) {
		this.skipperClient = event.getSkipperClient();
	}

}
