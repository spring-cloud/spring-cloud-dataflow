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
import java.util.function.BiFunction;
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
import org.springframework.cloud.deployer.resource.docker.DockerResource;
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

/**
 * Delegates to Skipper to deploy the stream.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Soby Chacko
 * @author Glenn Renfro
 */
public class SkipperStreamDeployer implements StreamDeployer {

	public static final String SKIPPER_KEY_PREFIX = "spring.cloud.dataflow.skipper";

	public static final String SKIPPER_PACKAGE_NAME = SKIPPER_KEY_PREFIX + ".packageName";

	public static final String SKIPPER_PACKAGE_VERSION = SKIPPER_KEY_PREFIX + ".packageVersion";

	public static final String SKIPPER_PACKAGE_REPO_NAME = SKIPPER_KEY_PREFIX + ".repoName";

	public static final String SKIPPER_PLATFORM_NAME = SKIPPER_KEY_PREFIX + ".platformName";

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

	/**
	 * Extracts the version from the resource.
	 * @param resource to be used.
	 * @return version the resource.
	 */
	public static String getResourceVersion(Resource resource) {
		Assert.notNull(resource, "resource must not be null");
		if (resource instanceof MavenResource) {
			MavenResource mavenResource = (MavenResource) resource;
			return mavenResource.getVersion();
		}
		else if (resource instanceof DockerResource) {
			DockerResource dockerResource = (DockerResource) resource;
			return formatDockerResource(dockerResource, (s, i) -> s.substring(i + 1, s.length()));
		}
		return extractGenericVersion(resource);
	}

	private static String extractGenericVersion(Resource resource)  {
		try {
			String uri = resource.getURI().toString();
			return uri.substring(getVersionIndex(uri) + 1, uri.lastIndexOf("."));
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	/**
	 * Extracts the string representing the resource with the version number extracted.
	 * @param resource to be used.
	 * @return String representation of the resource.
	 */
	public static String getResourceWithoutVersion(Resource resource) {
		Assert.notNull(resource, "resource must not be null");
		if (resource instanceof MavenResource) {
			MavenResource mavenResource = (MavenResource) resource;
			return String.format("maven://%s:%s",
					mavenResource.getGroupId(),
					mavenResource.getArtifactId());
		}
		else if (resource instanceof DockerResource) {
			DockerResource dockerResource = (DockerResource) resource;
			return formatDockerResource(dockerResource, (s, i) -> s.substring(0, i));
		}
		return extractGenericResourceWithoutVersion(resource);
	}

	private static String formatDockerResource(DockerResource dockerResource, BiFunction<String, Integer, String> function) {
		try {
			String dockerResourceUri = dockerResource.getURI().toString();
			Assert.isTrue(StringUtils.countOccurrencesOf(dockerResourceUri, ":") == 2,
					"Invalid docker resource URI: " + dockerResourceUri);
			int indexOfVersionSeparator = dockerResourceUri.lastIndexOf(":");
			return function.apply(dockerResourceUri, indexOfVersionSeparator);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String extractGenericResourceWithoutVersion(Resource resource) {
		try {
			String uri = resource.getURI().toString();
			return String.format("%s.%s", uri.substring(0, getVersionIndex(uri)),
					StringUtils.getFilenameExtension(uri));
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	private static int getVersionIndex(String uri) {
		int indexOfSnapshot = uri.lastIndexOf(".BUILD-SNAPSHOT");
		return (indexOfSnapshot > -1) ?
				uri.substring(0, indexOfSnapshot).lastIndexOf('-') :
				uri.lastIndexOf('-');
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
		Map<String, String> streamDeployerProperties = streamDeploymentRequest.getStreamDeployerProperties();
		String repoName = streamDeployerProperties.get(SKIPPER_PACKAGE_REPO_NAME);
		repoName = (StringUtils.hasText(repoName)) ? (repoName) : "local";
		String platformName = streamDeployerProperties.get(SKIPPER_PLATFORM_NAME);
		platformName = (StringUtils.hasText(platformName)) ? platformName : "default";
		String packageName = streamDeployerProperties.get(SKIPPER_PACKAGE_NAME);
		packageName = (StringUtils.hasText(packageName)) ? packageName : streamDeploymentRequest.getStreamName();
		String packageVersion = streamDeployerProperties.get(SKIPPER_PACKAGE_VERSION);
		packageVersion = (StringUtils.hasText(packageVersion)) ? packageVersion : "1.0.0";
		// Create the package .zip file to upload
		File packageFile = createPackageForStream(packageName, packageVersion, streamDeploymentRequest);
		// Upload the package
		UploadRequest uploadRequest = new UploadRequest();
		uploadRequest.setName(streamDeployerProperties.get(SKIPPER_PACKAGE_NAME));
		uploadRequest.setVersion(packageVersion);
		uploadRequest.setExtension("zip");
		uploadRequest.setRepoName(repoName); // TODO use from skipperDeploymentProperties if set.
		try {
			uploadRequest.setPackageFileAsBytes(Files.readAllBytes(packageFile.toPath()));
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't read packageFile " + packageFile, e);
		}
		skipperClient.upload(uploadRequest);
		// Install the package
		String streamName = streamDeploymentRequest.getStreamName();
		InstallRequest installRequest = new InstallRequest();
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		packageIdentifier.setRepositoryName(repoName);
		installRequest.setPackageIdentifier(packageIdentifier);
		InstallProperties installProperties = new InstallProperties();
		installProperties.setPlatformName(platformName);
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

	private File createPackageForStream(String packageName, String packageVersion, StreamDeploymentRequest
			streamDeploymentRequest) {
		PackageWriter packageWriter = new DefaultPackageWriter();
		Package pkgtoWrite = createPackage(packageName, packageVersion, streamDeploymentRequest);
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

	private Package createPackage(String packageName, String packageVersion, StreamDeploymentRequest
			streamDeploymentRequest) {
		Package pkg = new Package();
		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setApiVersion("skipper/v1");
		packageMetadata.setKind("SpringBootApp");
		packageMetadata.setName(packageName);
		packageMetadata.setVersion(packageVersion);
		packageMetadata.setMaintainer("dataflow");
		packageMetadata.setDescription(streamDeploymentRequest.getDslText());
		pkg.setMetadata(packageMetadata);
		pkg.setDependencies(createDependentPackages(packageVersion, streamDeploymentRequest));
		return pkg;
	}

	private List<Package> createDependentPackages(String packageVersion, StreamDeploymentRequest
			streamDeploymentRequest) {
		List<Package> packageList = new ArrayList<>();
		for (AppDeploymentRequest appDeploymentRequest : streamDeploymentRequest.getAppDeploymentRequests()) {
			packageList.add(createDependentPackage(packageVersion, appDeploymentRequest));
		}
		return packageList;
	}

	private Package createDependentPackage(String packageVersion, AppDeploymentRequest
			appDeploymentRequest) {
		Package pkg = new Package();
		String packageName = appDeploymentRequest.getDefinition().getName();

		PackageMetadata packageMetadata = new PackageMetadata();
		packageMetadata.setApiVersion("skipper/v1");
		packageMetadata.setKind("SpringBootApp");
		packageMetadata.setName(packageName);
		packageMetadata.setVersion(packageVersion);
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
		metadataMap.put("name", packageName);

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

	@Override
	public void undeployStream(String streamName) {
		String releaseName = "my" + streamName;
		this.skipperClient.delete(releaseName);
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
