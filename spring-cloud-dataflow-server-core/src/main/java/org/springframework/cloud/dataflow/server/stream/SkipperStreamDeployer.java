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

package org.springframework.cloud.dataflow.server.stream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.DataFlowPropertyKeys;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.core.StreamDeployment;
import org.springframework.cloud.dataflow.core.StreamRuntimePropertyKeys;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.SkipperStream;
import org.springframework.cloud.dataflow.server.controller.NoSuchAppException;
import org.springframework.cloud.dataflow.server.controller.NoSuchAppInstanceException;
import org.springframework.cloud.dataflow.server.controller.support.InvalidStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchStreamDefinitionException;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.LogInfo;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.ScaleRequest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifest;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationManifestReader;
import org.springframework.cloud.skipper.domain.SpringCloudDeployerApplicationSpec;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.io.DefaultPackageWriter;
import org.springframework.cloud.skipper.io.PackageWriter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Delegates to Skipper to deploy the stream.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Soby Chacko
 * @author Glenn Renfro
 * @author Christian Tzolov
 * @author Chris Bono
 */
public class SkipperStreamDeployer implements StreamDeployer {

    private static final Logger logger = LoggerFactory.getLogger(SkipperStreamDeployer.class);

    //Assume version suffix added by skipper is 5 chars.
    private static final int MAX_APPNAME_LENGTH = 63 - 5;

    private final SkipperClient skipperClient;

    private final StreamDefinitionRepository streamDefinitionRepository;

    private final AppRegistryService appRegistryService;

    private final ForkJoinPool forkJoinPool;

    private final StreamDefinitionService streamDefinitionService;

    public SkipperStreamDeployer(SkipperClient skipperClient, StreamDefinitionRepository streamDefinitionRepository,
                                 AppRegistryService appRegistryService, ForkJoinPool forkJoinPool,
                                 StreamDefinitionService streamDefinitionService) {
        Assert.notNull(skipperClient, "SkipperClient can not be null");
        Assert.notNull(streamDefinitionRepository, "StreamDefinitionRepository can not be null");
        Assert.notNull(appRegistryService, "StreamDefinitionRepository can not be null");
        Assert.notNull(forkJoinPool, "ForkJoinPool can not be null");
        Assert.notNull(streamDefinitionService, "StreamDefinitionService can not be null");
        this.skipperClient = skipperClient;
        this.streamDefinitionRepository = streamDefinitionRepository;
        this.appRegistryService = appRegistryService;
        this.forkJoinPool = forkJoinPool;
        this.streamDefinitionService = streamDefinitionService;
    }

    public static List<AppStatus> deserializeAppStatus(String platformStatus) {
        try {
            if (platformStatus != null) {
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
                return mapper.readValue(platformStatus, typeRef);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Could not parse Skipper Platform Status JSON [" + platformStatus + "]. " +
                    "Exception message = " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public DeploymentState streamState(String streamName) {
        return getStreamDeploymentState(streamName);
    }

    @Override
    public Map<StreamDefinition, DeploymentState> streamsStates(List<StreamDefinition> streamDefinitions) {
        Map<String, StreamDefinition> nameToDefinition = new HashMap<>();
        Map<StreamDefinition, DeploymentState> states = new HashMap<>();
        List<String> streamNamesList = new ArrayList<>();
        streamDefinitions.stream().forEach(sd -> {
            streamNamesList.add(sd.getName());
            nameToDefinition.put(sd.getName(), sd);
        });
        String[] streamNames = streamNamesList.toArray(new String[0]);
        Map<String, Map<String, DeploymentState>> statuses = this.skipperClient.states(streamNames);
        for (Map.Entry<String, StreamDefinition> entry : nameToDefinition.entrySet()) {
            String streamName = entry.getKey();
            if (statuses != null && statuses.containsKey(streamName) && !statuses.get(streamName).isEmpty()) {
                states.put(nameToDefinition.get(streamName),
                        StreamDeployerUtil.aggregateState(new HashSet<>(statuses.get(streamName).values())));
            } else {
                states.put(nameToDefinition.get(streamName), DeploymentState.undeployed);
            }
        }
        return states;
    }

    private DeploymentState getStreamDeploymentState(String streamName) {
        DeploymentState state = null;
        try {
            Info info = this.skipperClient.status(streamName);
            if (info.getStatus().getPlatformStatus() == null) {
                return getDeploymentStateFromStatusInfo(info);
            }
            List<AppStatus> appStatusList = deserializeAppStatus(info.getStatus().getPlatformStatus());
            Set<DeploymentState> deploymentStateList = appStatusList.stream().map(AppStatus::getState)
                    .collect(Collectors.toSet());
            state = StreamDeployerUtil.aggregateState(deploymentStateList);
        } catch (ReleaseNotFoundException e) {
            // a defined stream but unknown to skipper is considered to be in an undeployed state
            if (streamDefinitionExists(streamName)) {
                state = DeploymentState.undeployed;
            }
        }
        return state;
    }

    private DeploymentState getDeploymentStateFromStatusInfo(Info info) {
        DeploymentState result = DeploymentState.unknown;
        switch (info.getStatus().getStatusCode()) {
            case FAILED:
                result = DeploymentState.failed;
                break;
            case DELETED:
                result = DeploymentState.undeployed;
                break;
            case DEPLOYED:
                result = DeploymentState.deployed;
        }
        return result;
    }

    private boolean streamDefinitionExists(String streamName) {
        return this.streamDefinitionRepository.findById(streamName).isPresent();
    }

    @Override
    public void scale(String streamName, String appName, int count, Map<String, String> properties) {
        this.skipperClient.scale(streamName, ScaleRequest.of(appName, count, properties));
    }

    public Release deployStream(StreamDeploymentRequest streamDeploymentRequest) {
        validateStreamDeploymentRequest(streamDeploymentRequest);
        Map<String, String> streamDeployerProperties = streamDeploymentRequest.getStreamDeployerProperties();
        String packageVersion = streamDeployerProperties.get(SkipperStream.SKIPPER_PACKAGE_VERSION);
        Assert.isTrue(StringUtils.hasText(packageVersion), "Package Version must be set");
        logger.info("Deploying Stream " + streamDeploymentRequest.getStreamName() + " using skipper.");
        String repoName = streamDeployerProperties.get(SkipperStream.SKIPPER_REPO_NAME);
        repoName = (StringUtils.hasText(repoName)) ? (repoName) : "local";
        String platformName = streamDeployerProperties.get(SkipperStream.SKIPPER_PLATFORM_NAME);
        platformName = determinePlatformName(platformName);
        String packageName = streamDeployerProperties.get(SkipperStream.SKIPPER_PACKAGE_NAME);
        packageName = (StringUtils.hasText(packageName)) ? packageName : streamDeploymentRequest.getStreamName();
        // Create the package .zip file to upload
        File packageFile = createPackageForStream(packageName, packageVersion, streamDeploymentRequest);
        // Upload the package
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setName(packageName);
        uploadRequest.setVersion(packageVersion);
        uploadRequest.setExtension("zip");
        uploadRequest.setRepoName(repoName); // TODO use from skipperDeploymentProperties if set.
        try {
            uploadRequest.setPackageFileAsBytes(Files.readAllBytes(packageFile.toPath()));
        } catch (IOException e) {
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
        installProperties.setReleaseName(streamName);
        installProperties.setConfigValues(new ConfigValues());
        installRequest.setInstallProperties(installProperties);
        Release release = null;
        try {
            release = this.skipperClient.install(installRequest);
        } catch (Exception e) {
            logger.error("Skipper install failed. Deleting the package: " + packageName);
            try {
                this.skipperClient.packageDelete(packageName);
            } catch (Exception e1) {
                logger.error("Package delete threw exception: " + e1.getMessage());
            }
            throw new SkipperException(e.getMessage());
        }
        // TODO store releasename in deploymentIdRepository...
        return release;
    }

    private String determinePlatformName(final String platformName) {
        Collection<Deployer> deployers = skipperClient.listDeployers();
        if (StringUtils.hasText(platformName)) {
            List<Deployer> filteredDeployers = deployers.stream()
                    .filter(d -> d.getName().equals(platformName))
                    .collect(Collectors.toList());
            if (filteredDeployers.size() == 0) {
                throw new IllegalArgumentException("No platform named '" + platformName + "'");
            } else {
                return platformName;
            }
        } else {
            if (deployers.size() == 0) {
                throw new IllegalArgumentException("No platforms configured");
            } else {
                String platformNameToUse = deployers.stream().findFirst().get().getName();
                logger.info("Using platform '" + platformNameToUse + "'");
                return platformNameToUse;
            }
        }
    }

    private void validateStreamDeploymentRequest(StreamDeploymentRequest streamDeploymentRequest) {
        if (streamDeploymentRequest.getAppDeploymentRequests() == null
                || streamDeploymentRequest.getAppDeploymentRequests().isEmpty()) {
            // nothing to validate.
            return;
        }
        String streamName = streamDeploymentRequest.getStreamName();
        // throw as at this point we should have definition
        StreamDefinition streamDefinition = this.streamDefinitionRepository
                .findById(streamName)
                .orElseThrow(() -> new NoSuchStreamDefinitionException(streamDeploymentRequest.getStreamName()));

        for (AppDeploymentRequest adr : streamDeploymentRequest.getAppDeploymentRequests()) {
            String registeredAppName = getRegisteredName(streamDefinition, adr.getDefinition().getName());
            String appName = String.format("%s-%s-v", streamName, registeredAppName);
            if (appName.length() > 40) {
                logger.warn("The stream name plus application name [" + appName + "] is longer than 40 characters." +
                        "  This can not exceed " + MAX_APPNAME_LENGTH + " in length.");
            }
            if (appName.length() > MAX_APPNAME_LENGTH) {
                throw new InvalidStreamDefinitionException(
                        String.format("The runtime application name for the app %s in the stream %s "
                                + "should not exceed %s in length. The runtime application name is: %s", registeredAppName, streamName, MAX_APPNAME_LENGTH, appName));
            }
            String version = this.appRegistryService.getResourceVersion(adr.getResource());
            validateAppVersionIsRegistered(registeredAppName, adr, version);
        }
    }

    private String getRegisteredName(StreamDefinition streamDefinition, String adrAppName) {
        for (StreamAppDefinition appDefinition : this.streamDefinitionService.getAppDefinitions(streamDefinition)) {
            if (appDefinition.getName().equals(adrAppName)) {
                return appDefinition.getRegisteredAppName();
            }
        }
        return adrAppName;
    }

    public void validateAppVersionIsRegistered(StreamDefinition streamDefinition, AppDeploymentRequest appDeploymentRequest, String appVersion) {
        String registeredAppName = getRegisteredName(streamDefinition, appDeploymentRequest.getDefinition().getName());
        this.validateAppVersionIsRegistered(registeredAppName, appDeploymentRequest, appVersion);
    }

    private void validateAppVersionIsRegistered(String registeredAppName, AppDeploymentRequest appDeploymentRequest, String appVersion) {
        String appTypeString = appDeploymentRequest.getDefinition().getProperties()
                .get(DataFlowPropertyKeys.STREAM_APP_TYPE);
        ApplicationType applicationType = ApplicationType.valueOf(appTypeString);
        if (!this.appRegistryService.appExist(registeredAppName, applicationType, appVersion)) {
            throw new IllegalStateException(String.format("The %s:%s:%s app is not registered!",
                    registeredAppName, appTypeString, appVersion));
        }
    }

    private File createPackageForStream(String packageName, String packageVersion,
                                        StreamDeploymentRequest streamDeploymentRequest) {
        PackageWriter packageWriter = new DefaultPackageWriter();
        Package pkgtoWrite = createPackage(packageName, packageVersion, streamDeploymentRequest);
        Path tempPath;
        try {
            tempPath = Files.createTempDirectory("streampackages");
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't create temp diroectory");
        }
        File outputDirectory = tempPath.toFile();

        File zipFile = packageWriter.write(pkgtoWrite, outputDirectory);
        return zipFile;
    }

    private Package createPackage(String packageName, String packageVersion,
                                  StreamDeploymentRequest streamDeploymentRequest) {
        Package pkg = new Package();
        PackageMetadata packageMetadata = new PackageMetadata();
        packageMetadata.setApiVersion(SkipperStream.SKIPPER_DEFAULT_API_VERSION);
        packageMetadata.setKind(SkipperStream.SKIPPER_DEFAULT_KIND);
        packageMetadata.setName(packageName);
        packageMetadata.setVersion(packageVersion);
        packageMetadata.setMaintainer(SkipperStream.SKIPPER_DEFAULT_MAINTAINER);
        packageMetadata.setDescription(streamDeploymentRequest.getDslText());
        pkg.setMetadata(packageMetadata);
        pkg.setDependencies(createDependentPackages(packageVersion, streamDeploymentRequest));
        return pkg;
    }

    private List<Package> createDependentPackages(String packageVersion,
                                                  StreamDeploymentRequest streamDeploymentRequest) {
        List<Package> packageList = new ArrayList<>();
        for (AppDeploymentRequest appDeploymentRequest : streamDeploymentRequest.getAppDeploymentRequests()) {
            packageList.add(createDependentPackage(packageVersion, appDeploymentRequest));
        }
        return packageList;
    }

    private Package createDependentPackage(String packageVersion, AppDeploymentRequest appDeploymentRequest) {
        Package pkg = new Package();
        String packageName = appDeploymentRequest.getDefinition().getName();

        PackageMetadata packageMetadata = new PackageMetadata();
        packageMetadata.setApiVersion(SkipperStream.SKIPPER_DEFAULT_API_VERSION);
        packageMetadata.setKind(SkipperStream.SKIPPER_DEFAULT_KIND);
        packageMetadata.setName(packageName);
        packageMetadata.setVersion(packageVersion);
        packageMetadata.setMaintainer(SkipperStream.SKIPPER_DEFAULT_MAINTAINER);

        pkg.setMetadata(packageMetadata);

        ConfigValues configValues = new ConfigValues();
        Map<String, Object> configValueMap = new HashMap<>();
        Map<String, Object> metadataMap = new HashMap<>();
        Map<String, Object> specMap = new HashMap<>();

        // Add metadata
        metadataMap.put("name", packageName);

        // Add spec
        String resourceWithoutVersion = this.appRegistryService.getResourceWithoutVersion(appDeploymentRequest.getResource());
        specMap.put("resource", resourceWithoutVersion);
        specMap.put("applicationProperties", appDeploymentRequest.getDefinition().getProperties());
        specMap.put("deploymentProperties", appDeploymentRequest.getDeploymentProperties());
        String version = this.appRegistryService.getResourceVersion(appDeploymentRequest.getResource());
        // Add version, including possible override via deploymentProperties - hack to store version in cmdline args
        if (appDeploymentRequest.getCommandlineArguments().size() == 1) {
            specMap.put("version", appDeploymentRequest.getCommandlineArguments().get(0));
        } else {
            specMap.put("version", version);
        }
        // Add metadata and spec to top level map
        configValueMap.put("metadata", metadataMap);
        configValueMap.put("spec", specMap);

		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
		dumperOptions.setPrettyFlow(false);
		dumperOptions.setSplitLines(false);
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()), new Representer(dumperOptions), dumperOptions);
		configValues.setRaw(yaml.dump(configValueMap));

        pkg.setConfigValues(configValues);
        pkg.setTemplates(createGenericTemplate());
        return pkg;

    }

    private List<Template> createGenericTemplate() {
        Template template = this.skipperClient.getSpringCloudDeployerApplicationTemplate();
        List<Template> templateList = new ArrayList<>();
        templateList.add(template);
        return templateList;
    }

    public void undeployStream(String streamName) {
        Collection<PackageMetadata> packages = this.skipperClient.search(streamName, false);
        boolean packageExists = packages.stream().map(PackageMetadata::getName).anyMatch(s -> s.equalsIgnoreCase(streamName));
        if (packageExists) {
            try {
                this.skipperClient.delete(streamName, true);
            } catch (ReleaseNotFoundException e) {
                logger.info("Release not found for {}. Deleting the package {}", streamName, streamName);
                this.skipperClient.packageDelete(streamName);
            };
        } else {
            logger.info("Can not find package named '{}' - bypassing Skipper delete.", streamName);
        }
    }

    @Override
    public Page<AppStatus> getAppStatuses(Pageable pageable) {
        List<String> streamNames = new ArrayList<>();
        Page<StreamDefinition> streamDefinitions = this.streamDefinitionRepository.findAll(pageable);
        for (Map.Entry<StreamDefinition, DeploymentState> entry : this.streamsStates(streamDefinitions.getContent()).entrySet()) {
            if (entry.getValue() != null && (entry.getValue().equals(DeploymentState.deployed) ||
                    entry.getValue().equals(DeploymentState.partial))) {
                streamNames.add(entry.getKey().getName());
            }
        }
        return new PageImpl<>(getStreamsStatuses(streamNames), pageable, streamNames.size());
    }

    @Override
    public AppStatus getAppStatus(String appDeploymentId) {
        // iteration through a real platform statuses one by one
        // is too expensive instead we rely on what skipper
        // already knows about it.
        AppStatus appStatus = this.skipperClient.list(null)
                .stream()
                .flatMap(r -> {
                    Info info = r.getInfo();
                    if (info != null) {
                        Status status = info.getStatus();
                        if (status != null) {
                            return status.getAppStatusList().stream();
                        }
                    }
                    return Stream.empty();
                })
                .filter(as -> ObjectUtils.nullSafeEquals(appDeploymentId, as.getDeploymentId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchAppException(appDeploymentId));
        if (logger.isDebugEnabled()) {
            for (AppInstanceStatus status : appStatus.getInstances().values()) {
                logger.debug("getAppStatus:{}:{}:{}", status.getId(), status.getState(), status.getAttributes());
            }
        }
        return appStatus;
    }

    @Override
    public List<AppStatus> getStreamStatuses(String streamName) {
        return skipperStatus(streamName);
    }

    @Override
    public Map<String, List<AppStatus>> getStreamStatuses(String[] streamNames) {
        Map<String, Info> statuses = this.skipperClient.statuses(streamNames);
        Map<String, List<AppStatus>> appStatuses = new HashMap<>();
        statuses.entrySet().stream().forEach(e -> {
            appStatuses.put(e.getKey(), e.getValue().getStatus().getAppStatusList());
        });
        return appStatuses;
    }

    @Override
    public LogInfo getLog(String streamName) {
        return this.skipperClient.getLog(streamName);
    }

    @Override
    public LogInfo getLog(String streamName, String appName) {
        return this.skipperClient.getLog(streamName, appName);
    }

    private List<AppStatus> getStreamsStatuses(List<String> streamNames) {
        Map<String, Info> statuses = this.skipperClient.statuses(streamNames.toArray(new String[0]));
        List<AppStatus> appStatusList = new ArrayList<>();
        statuses.entrySet().stream().forEach(e -> {
            appStatusList.addAll(e.getValue().getStatus().getAppStatusList());
        });
        return appStatusList;
    }

    @Override
    public RuntimeEnvironmentInfo environmentInfo() {
        AboutResource skipperInfo = skipperClient.info();
        Collection<Deployer> deployers = skipperClient.listDeployers();
        RuntimeEnvironmentInfo.Builder builder = new RuntimeEnvironmentInfo.Builder()
                .implementationName(skipperInfo.getVersionInfo().getServer().getName())
                .implementationVersion(skipperInfo.getVersionInfo().getServer().getVersion())
                .platformApiVersion("")
                .platformClientVersion("")
                .platformHostVersion("")
                .platformType("Skipper Managed")
                .spiClass(SkipperClient.class);
        for (Deployer d : deployers) {
            builder.addPlatformSpecificInfo(d.getName(), d.getType());
        }
        return builder.build();
    }

    @Override
    public StreamDeployment getStreamInfo(String streamName) {
        try {
            String manifest = this.manifest(streamName);
            if (StringUtils.hasText(manifest)) {
                List<SpringCloudDeployerApplicationManifest> appManifests =
                        new SpringCloudDeployerApplicationManifestReader().read(manifest);
                Map<String, Map<String, String>> streamPropertiesMap = new HashMap<>();
                for (SpringCloudDeployerApplicationManifest applicationManifest : appManifests) {
                    Map<String, String> versionAndDeploymentProperties = new HashMap<>();
                    SpringCloudDeployerApplicationSpec spec = applicationManifest.getSpec();
                    String applicationName = applicationManifest.getApplicationName();
                    versionAndDeploymentProperties.putAll(spec.getDeploymentProperties());
                    versionAndDeploymentProperties.put(SkipperStream.SKIPPER_SPEC_RESOURCE, spec.getResource());
                    versionAndDeploymentProperties.put(SkipperStream.SKIPPER_SPEC_VERSION, spec.getVersion());
                    streamPropertiesMap.put(applicationName, versionAndDeploymentProperties);
                }
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String json = objectMapper.writeValueAsString(streamPropertiesMap);
                    return new StreamDeployment(streamName, json);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to serializer streamPropertiesMap", e);
                }
            }
        } catch (ReleaseNotFoundException e) {
            return new StreamDeployment(streamName);
        }
        return new StreamDeployment(streamName);
    }

    @Override
    public List<String> getStreams() {
        return this.skipperClient.list("").stream().map(Release::getName).collect(Collectors.toList());
    }

    @Override
    public String getFromActuator(String appId, String instanceId, String endpoint) {
        String releaseName = determineReleaseName(appId, instanceId);
        return skipperClient.getFromActuator(releaseName, appId, instanceId, endpoint);
    }

    @Override
    public Object postToActuator(String appId, String instanceId, ActuatorPostRequest actuatorPostRequest) {
        String releaseName = determineReleaseName(appId, instanceId);
        return skipperClient.postToActuator(releaseName, appId, instanceId, actuatorPostRequest);
    }

    private String determineReleaseName(String appId, String instanceId) {
        AppStatus status = this.getAppStatus(appId);
        if (status.getState().equals(DeploymentState.unknown)) {
            throw new NoSuchAppException(appId);
        }
        AppInstanceStatus appInstanceStatus = status.getInstances().get(instanceId);
        if (appInstanceStatus == null) {
            throw new NoSuchAppInstanceException(instanceId);
        }
        String releaseName = appInstanceStatus.getAttributes().get(StreamRuntimePropertyKeys.ATTRIBUTE_SKIPPER_RELEASE_NAME);
        if (releaseName == null) {
            throw new RuntimeException(String.format("Could not determine release name for %s / %s", appId, instanceId));
        }
        return releaseName;
    }

    private List<AppStatus> skipperStatus(String streamName) {
        List<AppStatus> appStatuses = new ArrayList<>();
        try {
            Info info = this.skipperClient.status(streamName);
            appStatuses.addAll(info.getStatus().getAppStatusList());
        } catch (Exception e) {
            // ignore as we query status for all the streams.
        }
        return appStatuses;
    }

    /**
     * Update the stream identified by the PackageIdentifier and runtime configuration values.
     *
     * @param streamName        the name of the stream to upgrade
     * @param packageIdentifier the name of the package in skipper
     * @param configYml         the YML formatted configuration values to use when upgrading
     * @param force             the flag to indicate if the stream update is forced even if there are no differences from the existing stream
     * @param appNames          the app names to update
     * @return release the upgraded release
     */
    public Release upgradeStream(String streamName, PackageIdentifier packageIdentifier, String configYml,
                                 boolean force, List<String> appNames) {
        UpgradeRequest upgradeRequest = new UpgradeRequest();
        upgradeRequest.setPackageIdentifier(packageIdentifier);
        UpgradeProperties upgradeProperties = new UpgradeProperties();
        ConfigValues configValues = new ConfigValues();
        configValues.setRaw(configYml);
        upgradeProperties.setConfigValues(configValues);
        upgradeProperties.setReleaseName(streamName);
        upgradeRequest.setUpgradeProperties(upgradeProperties);
        upgradeRequest.setForce(force);
        upgradeRequest.setAppNames(appNames);
        return this.skipperClient.upgrade(upgradeRequest);
    }

    /**
     * Rollback the stream to a specific version
     *
     * @param streamName     the name of the stream to rollback
     * @param releaseVersion the version of the stream to rollback to
     * @return instance of {@link Release}
     */
    public Release rollbackStream(String streamName, int releaseVersion) {
        RollbackRequest rollbackRequest = new RollbackRequest();
        rollbackRequest.setReleaseName(streamName);
        rollbackRequest.setVersion(releaseVersion);
        return this.skipperClient.rollback(rollbackRequest);
    }

    public String manifest(String name, int version) {
        if (version > 0) {
            return this.skipperClient.manifest(name, version);
        } else {
            return this.skipperClient.manifest(name);
        }
    }

    public String manifest(String name) {
        return this.skipperClient.manifest(name);
    }

    public Collection<Release> history(String releaseName) {
        return this.skipperClient.history(releaseName);
    }

    public Collection<Deployer> platformList() {
        return this.skipperClient.listDeployers();
    }
}
