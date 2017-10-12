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
package org.springframework.cloud.dataflow.server.stream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.server.controller.StreamDefinitionController;
import org.springframework.cloud.dataflow.server.repository.StreamDeploymentRepository;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.io.DefaultPackageWriter;
import org.springframework.cloud.skipper.io.PackageWriter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;

import static org.springframework.cloud.deployer.spi.app.AppDeployer.COUNT_PROPERTY_KEY;

/**
 * Delegates to Skipper to deploy the stream.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class SkipperStreamDeployer implements StreamDeployer {

	public static final String SKIPPER_KEY_PREFIX = "spring.cloud.dataflow.skipper";

	public static final String SKIPPER_ENABLED_PROPERTY_KEY = SKIPPER_KEY_PREFIX + ".enabled";

	private static Log logger = LogFactory.getLog(SkipperStreamDeployer.class);

	private final SkipperClient skipperClient;

	private final StreamDeploymentRepository streamDeploymentRepository;

	public SkipperStreamDeployer(SkipperClient skipperClient, StreamDeploymentRepository streamDeploymentRepository) {
		Assert.notNull(skipperClient, "SkipperClient can not be null");
		Assert.notNull(streamDeploymentRepository, "StreamDeploymentRepository can not be null");
		this.skipperClient = skipperClient;
		this.streamDeploymentRepository = streamDeploymentRepository;
	}

	public static String getResourceVersion(Resource resource) {
		if (resource instanceof MavenResource) {
			MavenResource mavenResource = (MavenResource) resource;
			return mavenResource.getVersion();
		}
		else {
			// TODO handle docker and http resource
			throw new IllegalArgumentException("Can't extract version from resource " + resource.getDescription());
		}
	}

	public static String getResourceWithoutVersion(Resource resource) {
		if (resource instanceof MavenResource) {
			MavenResource mavenResource = (MavenResource) resource;
			return String.format("maven://%s:%s",
					mavenResource.getGroupId(),
					mavenResource.getArtifactId());
		}
		else {
			// TODO handle docker and http resource
			throw new IllegalArgumentException("Can't extract version from resource " + resource.getDescription());
		}
	}

	@Override
	public String calculateStreamState(String streamName) {
		// TODO call out to skipper for stream state.
		return DeploymentState.unknown.toString();
	}

	@Override
	public Map<StreamDefinition, DeploymentState> state(List<StreamDefinition> streamDefinitions) {
		Map<StreamDefinition, DeploymentState> states = new HashMap<>();
		for (StreamDefinition streamDefinition : streamDefinitions) {
			try {
				//todo: Better naming for the release name
				Info info = this.skipperClient.status("my" + streamDefinition.getName());
				List<AppStatus> appStatusList = deserializeAppStatus(info.getStatus().getPlatformStatus());
				Set<DeploymentState> deploymentStateList = appStatusList.stream().map(appStatus -> appStatus.getState())
						.collect(Collectors.toSet());
				DeploymentState aggregateState = StreamDefinitionController.aggregateState(deploymentStateList);
				states.put(streamDefinition, aggregateState);
			}
			// todo: Handle ReleaseNotFoundException at the server side
			catch (HttpStatusCodeException e) {
				if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
					// ignore
				}
				else {
					throw new RuntimeException(e);
				}
			}
		}
		return states;
	}

	public List<AppStatus> deserializeAppStatus(String platformStatus) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.addMixIn(AppStatus.class, AppStatusMixin.class);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			SimpleModule module = new SimpleModule("CustomModel", Version.unknownVersion());
			SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
			resolver.addMapping(AppInstanceStatus.class, AppInstanceStatusImpl.class);
			module.setAbstractTypes(resolver);
			mapper.registerModule(module);
			TypeReference<List<AppStatus>> typeRef = new TypeReference<List<AppStatus>>() {
			};
			List<AppStatus> result = mapper.readValue(platformStatus, typeRef);
			return result;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Could not parse Skipper Platfrom Status JSON:" + platformStatus, e);
		}
	}

	@Override
	public void deployStream(StreamDeploymentRequest streamDeploymentRequest) {
		logger.info("Deploying Stream " + streamDeploymentRequest.getStreamName() + " using skipper.");
		// Create the package .zip file to upload
		File packageFile = createPackageForStream(streamDeploymentRequest);

		// Upload the package
		UploadRequest uploadRequest = new UploadRequest();
		uploadRequest.setName(streamDeploymentRequest.getStreamName());
		uploadRequest.setVersion("1.0.0"); // TODO use from skipperDeploymentProperties if set.
		uploadRequest.setExtension("zip");
		uploadRequest.setRepoName("local"); // TODO use from skipperDeploymentProperties if set.
		try {
			uploadRequest.setPackageFileAsBytes(Files.readAllBytes(packageFile.toPath()));
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't read packageFile " + packageFile, e);
		}
		skipperClient.upload(uploadRequest);

		// Install the package
		InstallRequest installRequest = new InstallRequest();
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		String streamName = streamDeploymentRequest.getStreamName();
		packageIdentifier.setPackageName(streamName);
		packageIdentifier.setPackageVersion("1.0.0");
		String repoName = "local";
		packageIdentifier.setRepositoryName(repoName);
		installRequest.setPackageIdentifier(packageIdentifier);
		InstallProperties installProperties = new InstallProperties();
		installProperties.setPlatformName("default");
		//todo: Better naming for the release name
		String releaseName = "my" + streamName;
		installProperties.setReleaseName(releaseName);
		installProperties.setConfigValues(new ConfigValues());
		installRequest.setInstallProperties(installProperties);
		StreamDeployment streamDeployment = new StreamDeployment(streamName, StreamDeployers.skipper.name(),  streamName,
				releaseName, repoName);
		skipperClient.install(installRequest);
		this.streamDeploymentRepository.save(streamDeployment);
		// TODO store releasename in deploymentIdRepository...
		// this.deploymentIdRepository.save(DeploymentKey.forStreamAppDefinition(streamAppDefinition),
		// id);

	}

	private File createPackageForStream(StreamDeploymentRequest streamDeploymentRequest) {
		PackageWriter packageWriter = new DefaultPackageWriter();
		Package pkgtoWrite = createPackage(streamDeploymentRequest);
		Path tempPath;
		try {
			tempPath = Files.createTempDirectory("streampackages");
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't create temp diroectory");
		}
		File outputDirectory = tempPath.toFile();

		File zipFile = packageWriter.write(pkgtoWrite, outputDirectory);
		return zipFile;
	}

	private Package createPackage(StreamDeploymentRequest streamDeploymentRequest) {
		Package pkg = new Package();
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName(streamDeploymentRequest.getStreamName());
		packageMetadata.setVersion("1.0.0");
		packageMetadata.setMaintainer("dataflow");
		packageMetadata.setDescription(streamDeploymentRequest.getDslText());
		pkg.setMetadata(packageMetadata);

		pkg.setDependencies(createDependentPackages(streamDeploymentRequest));

		return pkg;
	}

	private List<Package> createDependentPackages(StreamDeploymentRequest streamDeploymentRequest) {
		List<Package> packageList = new ArrayList<>();
		for (AppDeploymentRequest appDeploymentRequest : streamDeploymentRequest.getAppDeploymentRequests()) {
			packageList.add(createDependentPackage(streamDeploymentRequest.getStreamName(), appDeploymentRequest));
		}
		return packageList;
	}

	private Package createDependentPackage(String streamName, AppDeploymentRequest appDeploymentRequest) {
		Package pkg = new Package();

		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setName(appDeploymentRequest.getDefinition().getName());
		packageMetadata.setVersion("1.0.0");
		packageMetadata.setMaintainer("dataflow");

		pkg.setMetadata(packageMetadata);

		ConfigValues configValues = new ConfigValues();
		Map<String, Object> configValueMap = new HashMap<>();
		Map<String, Object> metadataMap = new HashMap<>();
		Map<String, Object> specMap = new HashMap<>();

		// Add version
		String version = getResourceVersion(appDeploymentRequest.getResource());
		configValueMap.put("version", version);

		// Add metadata
		String countProperty = appDeploymentRequest.getDeploymentProperties().get(COUNT_PROPERTY_KEY);
		int count = (StringUtils.hasText(countProperty)) ? Integer.parseInt(countProperty) : 1;
		metadataMap.put("count", Integer.toString(count));
		metadataMap.put("name", appDeploymentRequest.getDefinition().getName());

		// Add spec
		String resourceWithoutVersion = getResourceWithoutVersion(appDeploymentRequest.getResource());
		specMap.put("resource", resourceWithoutVersion);
		specMap.put("applicationProperties", appDeploymentRequest.getDefinition().getProperties());
		specMap.put("deploymentProperties", appDeploymentRequest.getDeploymentProperties());

		// Add metadata and spec to top level map
		configValueMap.put("metadata", metadataMap);
		configValueMap.put("spec", specMap);

		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		Yaml yaml = new Yaml(dumperOptions);
		configValues.setRaw(yaml.dump(configValueMap));

		pkg.setConfigValues(configValues);
		pkg.setTemplates(createGenericTemplate());
		return pkg;

	}

	private List<Template> createGenericTemplate() {
		Template template = this.skipperClient.getSpringBootAppTemplate();
		List<Template> templateList = new ArrayList<>();
		templateList.add(template);
		return templateList;
	}

	public void upgradeStream(String name, String releaseName, PackageIdentifier packageIdentifier, String yaml) {
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		ConfigValues configValues = new ConfigValues();
		configValues.setRaw(yaml);
		upgradeProperties.setConfigValues(configValues);
		upgradeProperties.setReleaseName(releaseName);
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		this.skipperClient.upgrade(upgradeRequest);
	}
}
