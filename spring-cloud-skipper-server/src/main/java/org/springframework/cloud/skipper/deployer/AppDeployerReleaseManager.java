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
package org.springframework.cloud.skipper.deployer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.*;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.cloud.skipper.service.ManifestStore;
import org.springframework.cloud.skipper.service.ReleaseManager;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * A ReleaseManager implementation that uses the AppDeployer.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@Service
public class AppDeployerReleaseManager implements ReleaseManager {

	private final ReleaseRepository releaseRepository;

	private final ManifestStore manifestStore;

	private final DelegatingResourceLoader delegatingResourceLoader;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final DeployerRepository deployerRepository;

	@Autowired
	public AppDeployerReleaseManager(ReleaseRepository releaseRepository,
			ManifestStore manifestStore,
			AppDeployerDataRepository appDeployerDataRepository,
			DelegatingResourceLoader delegatingResourceLoader,
			DeployerRepository deployerRepository) {
		this.releaseRepository = releaseRepository;
		this.manifestStore = manifestStore;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.delegatingResourceLoader = delegatingResourceLoader;
		this.deployerRepository = deployerRepository;
	}

	public Release deploy(Release release) {
		// TODO review transaction semantics
		this.releaseRepository.save(release);

		// TODO how to rollback file system/git in case of DB errors later in method
		// execution
		this.manifestStore.store(release);

		// Deploy the application
		List<Deployment> appDeployments = unmarshallDeployments(release.getManifest());

		AppDeployer appDeployer = deployerRepository.findByName(release.getPlatformName()).getDeployer();
		List<String> deploymentIds = new ArrayList<>();
		for (Deployment appDeployment : appDeployments) {
			deploymentIds.add(appDeployer.deploy(
					createAppDeploymentRequest(appDeployment, release.getName(),
							String.valueOf(release.getVersion()))));
		}
		AppDeployerData appDeployerData = new AppDeployerData();
		appDeployerData.setReleaseName(release.getName());
		appDeployerData.setReleaseVersion(release.getVersion());
		appDeployerData.setDeploymentData(StringUtils.collectionToCommaDelimitedString(deploymentIds));

		this.appDeployerDataRepository.save(appDeployerData);

		// Update Status in DB
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		release.getInfo().setStatus(status);
		release.getInfo().setDescription("Install complete");

		// Store updated state in in DB
		this.releaseRepository.save(release);
		updateStatus(release);
		return release;
	}

	public Release status(Release release) {
		AppDeployer appDeployer = this.deployerRepository.findByName(release.getPlatformName()).getDeployer();
		Set<String> deploymentIds = new HashSet<>();
		AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
		deploymentIds.addAll(StringUtils.commaDelimitedListToSet(appDeployerData.getDeploymentData()));
		if (!deploymentIds.isEmpty()) {
			boolean allDeployed = true;
			StringBuffer releaseStatusMsg = new StringBuffer();
			for (String deploymentId : deploymentIds) {
				AppStatus appStatus = appDeployer.status(deploymentId);
				if (appStatus.getState() != DeploymentState.deployed) {
					StringBuffer statusMsg = new StringBuffer(deploymentId + "=[");
					allDeployed = false;
					for (AppInstanceStatus appInstanceStatus : appStatus.getInstances().values()) {
						statusMsg.append(appInstanceStatus.getId() + "=" + appInstanceStatus.getState());
					}
					statusMsg.append("]");
					releaseStatusMsg.append(statusMsg);
				}
			}
			if (allDeployed) {
				release.getInfo().getStatus().setPlatformStatus("All the applications are deployed successfully.");
			}
			else {
				release.getInfo().getStatus().setPlatformStatus(
						"Not all the applications are deployed successfully. " + releaseStatusMsg.toString());
			}
		}
		return release;
	}

	public Release undeploy(Release release) {
		AppDeployer appDeployer = deployerRepository.findByName(release.getPlatformName()).getDeployer();
		Set<String> deploymentIds = new HashSet<>();
		AppDeployerData appDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersion(release.getName(), release.getVersion());
		deploymentIds.addAll(StringUtils.commaDelimitedListToSet(appDeployerData.getDeploymentData()));
		if (!deploymentIds.isEmpty()) {
			Status deletingStatus = new Status();
			deletingStatus.setStatusCode(StatusCode.DELETING);
			release.getInfo().setStatus(deletingStatus);
			this.releaseRepository.save(release);
			for (String deploymentId : deploymentIds) {
				appDeployer.undeploy(deploymentId);
			}
			Status deletedStatus = new Status();
			deletedStatus.setStatusCode(StatusCode.DELETED);
			release.getInfo().setStatus(deletedStatus);
			release.getInfo().setDescription("Undeployment complete");
			this.releaseRepository.save(release);
		}
		return release;
	}

	@Override
	public void updateStatus(Release release) {
		//
		// boolean allClear = true;
		// Map<String, AppInstanceStatus> instances = new HashMap<String,
		// AppInstanceStatus>();
		// List<String> deploymentIds = Arrays
		// .asList(StringUtils.commaDelimitedListToStringArray(release.getDeploymentId()));
		//
		// for (String deploymentId : deploymentIds) {
		// AppStatus status = appDeployer.status(deploymentId);
		// for (AppInstanceStatus appInstanceStatus : status.getInstances().values()) {
		// if (appInstanceStatus.getState() != DeploymentState.deployed) {
		// allClear = false;
		// }
		// }
		// }
		// if (allClear) {
		// release.getInfo().getStatus().setPlatformStatus("All Applications deployed
		// successfully");
		// releaseRepository.save(release);
		// }
		// else {
		// StringBuffer stringBuffer = new StringBuffer();
		// stringBuffer.append("Not all applications deployed successfully. ");
		// for (String deploymentId : deploymentIds) {
		// AppStatus status = appDeployer.status(deploymentId);
		// for (AppInstanceStatus appInstanceStatus : status.getInstances().values()) {
		// stringBuffer.append(appInstanceStatus.getId()).append("=").append(appInstanceStatus.getState())
		// .append(", ");
		// }
		// }
		// String platformStatus = stringBuffer.toString();
		// platformStatus = platformStatus.replaceAll(", $", "");
		// release.getInfo().getStatus().setPlatformStatus(platformStatus);
		// releaseRepository.save(release);
		// }

	}

	private List<Deployment> unmarshallDeployments(String manifests) {

		List<AppDeploymentKind> deploymentKindList = new ArrayList<>();
		YAMLMapper mapper = new YAMLMapper();
		try {
			MappingIterator<AppDeploymentKind> it = mapper.readerFor(AppDeploymentKind.class).readValues(manifests);
			while (it.hasNextValue()) {
				AppDeploymentKind deploymentKind = it.next();
				deploymentKindList.add(deploymentKind);
			}

		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't parse Package's manifest YAML", e);
		}

		List<Deployment> deploymentList = deploymentKindList.stream().map(AppDeploymentKind::getDeployment)
				.collect(Collectors.toList());
		return deploymentList;
	}

	private AppDeploymentRequest createAppDeploymentRequest(Deployment deployment, String releaseName,
			String version) {

		AppDefinition appDefinition = new AppDefinition(deployment.getName(), deployment.getApplicationProperties());
		Resource resource = delegatingResourceLoader.getResource(deployment.getResource());

		Map<String, String> deploymentProperties = deployment.getDeploymentProperties();
		if (deployment.getCount() != 0) {
			deploymentProperties.put(AppDeployer.COUNT_PROPERTY_KEY, String.valueOf(deployment.getCount()));
		}
		deploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, releaseName + "-v" + version);

		AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource,
				deploymentProperties);
		return appDeploymentRequest;
	}

}
