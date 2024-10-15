/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationHealthCheck;
import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppInstanceStatus;
import org.springframework.cloud.skipper.domain.CloudFoundryApplicationManifestReader;
import org.springframework.cloud.skipper.domain.CloudFoundryApplicationSkipperManifest;
import org.springframework.cloud.skipper.domain.CloudFoundryApplicationSpec;
import org.springframework.cloud.skipper.domain.CloudFoundryApplicationSpec.Manifest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * The helper class that handles the deployment related operation for CF manifest based application deployment.
 *
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
public class CloudFoundryManifestApplicationDeployer {

	public static final Duration STAGING_TIMEOUT = Duration.ofMinutes(15L);

	public static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(5L);

	public static final Duration PUSH_REQUEST_TIMEOUT = Duration.ofMinutes(360L);

	public static final Duration DELETE_REQUEST_TIMEOUT = Duration.ofSeconds(30L);

	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryManifestApplicationDeployer.class);

	private final CloudFoundryApplicationManifestReader cfApplicationManifestReader;

	private final PlatformCloudFoundryOperations platformCloudFoundryOperations;

	private final DelegatingResourceLoader delegatingResourceLoader;

	public CloudFoundryManifestApplicationDeployer(CloudFoundryApplicationManifestReader cfApplicationManifestReader,
			PlatformCloudFoundryOperations platformCloudFoundryOperations,
			DelegatingResourceLoader delegatingResourceLoader) {
		this.cfApplicationManifestReader = cfApplicationManifestReader;
		this.platformCloudFoundryOperations = platformCloudFoundryOperations;
		this.delegatingResourceLoader = delegatingResourceLoader;
	}

	public ApplicationManifest getCFApplicationManifest(Release release) {
		ApplicationManifest cfApplicationManifest = CloudFoundryApplicationManifestUtils.updateApplicationName(release);
		cfApplicationManifest = ApplicationManifest.builder()
				.from(cfApplicationManifest)
				.build();

		List<? extends CloudFoundryApplicationSkipperManifest> cfApplicationManifestList = this.cfApplicationManifestReader
				.read(release.getManifest().getData());
		for (CloudFoundryApplicationSkipperManifest cfApplicationSkipperManifest : cfApplicationManifestList) {
			CloudFoundryApplicationSpec spec = cfApplicationSkipperManifest.getSpec();
			String resource = spec.getResource();
			String version = spec.getVersion();

			Manifest manifest = spec.getManifest();
			cfApplicationManifest = ApplicationManifest.builder()
					.from(cfApplicationManifest)
					.buildpack(manifest.getBuildpack())
					.command(manifest.getCommand())
					.disk(CloudFoundryApplicationManifestUtils.memoryInteger(manifest.getDiskQuota()))
					.domains(manifest.getDomains() != null && !manifest.getDomains().isEmpty() ? manifest.getDomains()
							: null)
					.environmentVariables(manifest.getEnv() != null ? manifest.getEnv() : null)
					.healthCheckHttpEndpoint(manifest.getHealthCheckHttpEndpoint())
					.healthCheckType(manifest.getHealthCheckType() != null
							? ApplicationHealthCheck.from(manifest.getHealthCheckType().name())
							: null)
					.hosts(manifest.getHosts() != null && !manifest.getHosts().isEmpty() ? manifest.getHosts() : null)
					.instances(manifest.getInstances())
					.memory(CloudFoundryApplicationManifestUtils.memoryInteger(manifest.getMemory()))
					.noHostname(manifest.getNoHostname())
					.noRoute(manifest.getNoRoute())
					.randomRoute(manifest.getRandomRoute())
					.services(manifest.getServices())
					.stack(manifest.getStack())
					.timeout(manifest.getTimeout())
					.build();


			Resource application = this.delegatingResourceLoader.getResource(getResourceLocation(resource, version));
			cfApplicationManifest = CloudFoundryApplicationManifestUtils.updateApplicationPath(cfApplicationManifest, application);

		}

		return cfApplicationManifest;
	}

	public static String getResourceLocation(String specResource, String specVersion) {
		Assert.hasText(specResource, "Spec resource must not be empty");
		if (specVersion != null) {
			if ((specResource.startsWith("maven") || specResource.startsWith("docker"))) {
				if (specResource.endsWith(":" + specVersion)) {
					return specResource;
				}
				else {
					return String.format("%s:%s", specResource, specVersion);
				}
			}
			// When it is neither maven nor docker, the version is expected to have been embedded into resource value.
			else {
				return specResource;
			}
		}
		else {
			return specResource;
		}
	}

	public Mono<AppStatus> getStatus(String applicationName, String platformName) {
		GetApplicationRequest getApplicationRequest = GetApplicationRequest.builder().name(applicationName).build();
		return this.platformCloudFoundryOperations.getCloudFoundryOperations(platformName)
				.applications().get(getApplicationRequest)
				.map(applicationDetail -> createAppStatus(applicationDetail, applicationName))
				.onErrorResume(IllegalArgumentException.class, t -> {
					logger.debug("Application for {} does not exist.", applicationName);
					return Mono.just(createEmptyAppStatus(applicationName));
				})
				.onErrorResume(Throwable.class, t -> {
					logger.error("Error: " + t);
					return Mono.just(createErrorAppStatus(applicationName));
				});
	}

	private AppStatus createAppStatus(ApplicationDetail applicationDetail, String deploymentId) {
		logger.trace("Gathering instances for " + applicationDetail);
		logger.trace("InstanceDetails: " + applicationDetail.getInstanceDetails());

		AppStatus.Builder builder = AppStatus.of(deploymentId);

		int i = 0;
		for (InstanceDetail instanceDetail : applicationDetail.getInstanceDetails()) {
			builder.with(new CloudFoundryAppInstanceStatus(applicationDetail, instanceDetail, i++));
		}
		for (; i < applicationDetail.getInstances(); i++) {
			builder.with(new CloudFoundryAppInstanceStatus(applicationDetail, null, i));
		}

		return builder.build();
	}

	private AppStatus createEmptyAppStatus(String deploymentId) {
		return AppStatus.of(deploymentId)
				.build();
	}

	private AppStatus createErrorAppStatus(String deploymentId) {
		return AppStatus.of(deploymentId)
				.generalState(DeploymentState.error)
				.build();
	}

	public AppStatus status(Release release) {
		logger.info("Checking application status for the release: " + release.getName());
		ApplicationManifest applicationManifest = CloudFoundryApplicationManifestUtils.updateApplicationName(release);
		String applicationName = applicationManifest.getName();
		AppStatus appStatus = null;
		try {
			appStatus = getStatus(applicationName, release.getPlatformName())
					.doOnSuccess(v -> logger.info("Successfully computed status [{}] for {}", v,
							applicationName))
					.doOnError(e -> logger.error("Failed to compute status for {}", applicationName))
					.block();
		}
		catch (Exception timeoutDueToBlock) {
			logger.error("Caught exception while querying for status of {}", applicationName, timeoutDueToBlock);
			appStatus = createErrorAppStatus(applicationName);
		}
		return appStatus;
	}

	public Release delete(Release release) {
		ApplicationManifest applicationManifest = CloudFoundryApplicationManifestUtils.updateApplicationName(release);
		String applicationName = applicationManifest.getName();
		DeleteApplicationRequest deleteApplicationRequest = DeleteApplicationRequest.builder().name(applicationName)
				.build();
		this.platformCloudFoundryOperations.getCloudFoundryOperations(release.getPlatformName()).applications()
				.delete(deleteApplicationRequest)
				.timeout(DELETE_REQUEST_TIMEOUT)
				.doOnSuccess(v -> logger.info("Successfully undeployed app {}", applicationName))
				.doOnError(e -> logger.error("Failed to undeploy app %s", applicationName))
				.block();
		Status deletedStatus = new Status();
		deletedStatus.setStatusCode(StatusCode.DELETED);
		release.getInfo().setStatus(deletedStatus);
		release.getInfo().setDescription("Delete complete");
		return release;
	}

	public static Predicate<Throwable> isNotFoundError() {
		return t -> t instanceof AbstractCloudFoundryException acfe
				&& acfe.getStatusCode() == HttpStatus.NOT_FOUND.value();
	}

}
