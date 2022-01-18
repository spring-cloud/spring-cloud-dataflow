/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.cloud.skipper.server.service;

import java.util.Optional;

import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.server.repository.jpa.ReleaseRepository;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Service used to access an actuator endpoint for a deployed app instance.
 *
 * @author David Turanski
 */
public class ActuatorService {

	private final ReleaseService releaseService;

	private final DeployerRepository deployerRepository;

	private final ReleaseRepository releaseRepository;

	public ActuatorService(ReleaseService releaseService, DeployerRepository deployerRepository,
			ReleaseRepository releaseRepository) {
		Assert.notNull(releaseService, "'releaseService' is required");
		this.releaseService = releaseService;
		Assert.notNull(deployerRepository, "'deployerRepository' is required");
		this.deployerRepository = deployerRepository;
		Assert.notNull(releaseRepository, "'releaseRepository' is required");
		this.releaseRepository = releaseRepository;
	}

	/**
	 *
	 * @param releaseName the release name
	 * @param appName the deployment ID for the app
	 * @param appId the deployer assigned guid of the app instance
	 * @param endpoint the relative actuator resource path, e.g., {@code /info}, preceding /
	 *     is optional
	 * @param authorization pass through of authorization header if present.
	 * @return JSON content as UTF-8 encoded string
	 */
	public String getFromActuator(String releaseName, String appName, String appId, String endpoint,
			Optional<String> authorization) {

		return actuatorOperations(releaseName).getFromActuator(deploymentId(releaseName, appName), appId, endpoint,
				String.class, authHeader(authorization));
	}

	/**
	 *
	 * @param releaseName the release name
	 * @param appName the deployment ID for the app
	 * @param appId the deployer assigned guid of the app instance
	 * @param postRequest the request parameters {@link ActuatorPostRequest}
	 * @param authorization pass through of authorization header if present.
	 * @return the response object, if any
	 */
	public Object postToActuator(String releaseName, String appName, String appId, ActuatorPostRequest postRequest,
			Optional<String> authorization) {
		return actuatorOperations(releaseName)
				.postToActuator(deploymentId(releaseName, appName), appId, postRequest.getEndpoint(),
				postRequest.getBody(), Object.class, authHeader(authorization));
	}

	private String deploymentId(String releaseName, String appName) {
		return this.releaseService.status(releaseName).getStatus().getAppStatusList().stream()
				.filter(as -> appName.equals(as.getDeploymentId()))
				.map(as -> as.getDeploymentId())
				.findFirst().orElseThrow(() -> new IllegalArgumentException(
						String.format("app %s is not found in release %s", appName, releaseName)));
	}

	private Optional<HttpHeaders> authHeader(Optional<String> authorization) {
		return authorization.map(auth -> {
			HttpHeaders headers = new HttpHeaders();
			headers.set(HttpHeaders.AUTHORIZATION, auth);
			return headers;
		});
	}

	private ActuatorOperations actuatorOperations(String releaseName) {
		Release release = this.releaseRepository.findTopByNameOrderByVersionDesc(releaseName);
		return this.deployerRepository.findByNameRequired(release.getPlatformName())
				.getActuatorOperations();
	}
}
