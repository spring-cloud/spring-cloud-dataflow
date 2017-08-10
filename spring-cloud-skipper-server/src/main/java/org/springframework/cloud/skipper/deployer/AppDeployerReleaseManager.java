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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
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
 * A ReleaseManager implementation that uses the AppDeployer
 * @author Mark Pollack
 */
@Service
public class AppDeployerReleaseManager implements ReleaseManager {

	private ReleaseRepository releaseRepository;

	private ManifestStore manifestStore;

	private DelegatingResourceLoader delegatingResourceLoader;

	private AppDeployerDataRepository appDeployerDataRepository;

	private DeployerRepository deployerRepository;

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

	public void deploy(Release release) {
		// TODO review transaction semantics
		releaseRepository.save(release);

		// TODO how to rollback file system/git in case of DB errors later in method execution
		manifestStore.store(release);

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

		appDeployerDataRepository.save(appDeployerData);

		// Update Status in DB
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		release.getInfo().setStatus(status);
		release.getInfo().setDescription("Install complete");

		// Store updated state in in DB
		releaseRepository.save(release);
		updateStatus(release);

	}

	public void undeploy(Release release) {
		// List<String> deploymentIds = Arrays
		// .asList(StringUtils.commaDelimitedListToStringArray(release.getDeploymentId()));
		//
		// for (String deploymentId : deploymentIds) {
		// appDeployer.undeploy(deploymentId);
		// }
		// Status status = new Status();
		// status.setStatusCode(StatusCode.SUPERSEDED);
		// release.getInfo().setStatus(status);

		releaseRepository.save(release);
	}

	@Override
	public void updateStatus(Release release) {
		//
		// boolean allClear = true;
		// Map<String, AppInstanceStatus> instances = new HashMap<String, AppInstanceStatus>();
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
		deploymentProperties.put(AppDeployer.COUNT_PROPERTY_KEY, String.valueOf(deployment.getCount()));
		deploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, releaseName + "-v" + version);

		AppDeploymentRequest appDeploymentRequest = new AppDeploymentRequest(appDefinition, resource,
				deploymentProperties);
		return appDeploymentRequest;
	}

}
