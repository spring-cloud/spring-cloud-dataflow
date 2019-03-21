/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.dataflow.server.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.rest.resource.about.Dependency;
import org.springframework.cloud.dataflow.rest.resource.about.FeatureInfo;
import org.springframework.cloud.dataflow.rest.resource.about.RuntimeEnvironment;
import org.springframework.cloud.dataflow.rest.resource.about.RuntimeEnvironmentDetails;
import org.springframework.cloud.dataflow.rest.resource.about.SecurityInfo;
import org.springframework.cloud.dataflow.rest.resource.about.VersionInfo;
import org.springframework.cloud.dataflow.server.config.VersionInfoProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.config.security.support.SecurityStateBean;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that provides meta information regarding the dataflow server and its
 * deployers.
 *
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/about")
@ExposesResourceFor(AboutResource.class)
public class AboutController {

	private final FeaturesProperties featuresProperties;

	private final VersionInfoProperties versionInfoProperties;

	private final SecurityStateBean securityStateBean;

	@Value("${security.oauth2.client.client-id:#{null}}")
	private String oauthClientId;

	@Value("${info.app.name:#{null}}")
	private String implementationName;

	@Value("${info.app.version:#{null}}")
	private String implementationVersion;

	private AppDeployer appDeployer;

	private TaskLauncher taskLauncher;

	public AboutController(AppDeployer appDeployer, TaskLauncher taskLauncher, FeaturesProperties featuresProperties,
			VersionInfoProperties versionInfoProperties, SecurityStateBean securityStateBean) {
		this.appDeployer = appDeployer;
		this.taskLauncher = taskLauncher;
		this.featuresProperties = featuresProperties;
		this.versionInfoProperties = versionInfoProperties;
		this.securityStateBean = securityStateBean;
	}

	/**
	 * Return meta information about the dataflow server.
	 *
	 * @return Detailed information about the enabled features, versions of implementation
	 * libraries, and security configuration
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public AboutResource getAboutResource() {
		final AboutResource aboutResource = new AboutResource();
		final FeatureInfo featureInfo = new FeatureInfo();
		featureInfo.setAnalyticsEnabled(featuresProperties.isAnalyticsEnabled());
		featureInfo.setStreamsEnabled(featuresProperties.isStreamsEnabled());
		featureInfo.setTasksEnabled(featuresProperties.isTasksEnabled());

		final VersionInfo versionInfo = new VersionInfo();

		versionInfo.setImplementation(new Dependency(this.implementationName, this.implementationVersion));
		versionInfo
				.setCore(new Dependency("Spring Cloud Data Flow Core", versionInfoProperties.getDataflowCoreVersion()));
		versionInfo.setDashboard(
				new Dependency("Spring Cloud Dataflow UI", versionInfoProperties.getDataflowDashboardVersion()));

		aboutResource.setFeatureInfo(featureInfo);
		aboutResource.setVersionInfo(versionInfo);

		final boolean authenticationEnabled = securityStateBean.isAuthenticationEnabled();
		final boolean authorizationEnabled = securityStateBean.isAuthorizationEnabled();

		final SecurityInfo securityInfo = new SecurityInfo();
		securityInfo.setAuthenticationEnabled(authenticationEnabled);
		securityInfo.setAuthorizationEnabled(authorizationEnabled);

		if (authenticationEnabled && SecurityContextHolder.getContext() != null) {
			final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!(authentication instanceof AnonymousAuthenticationToken)) {
				securityInfo.setAuthenticated(authentication.isAuthenticated());
				securityInfo.setUsername(authentication.getName());

				if (authorizationEnabled) {
					for (GrantedAuthority authority : authentication.getAuthorities()) {
						securityInfo.addRole(authority.getAuthority());
					}
				}
				if (this.oauthClientId == null) {
					securityInfo.setFormLogin(true);
				}
				else {
					securityInfo.setFormLogin(false);
				}
			}
		}

		aboutResource.setSecurityInfo(securityInfo);

		final RuntimeEnvironment runtimeEnvironment = new RuntimeEnvironment();

		if (this.appDeployer != null) {
			final RuntimeEnvironmentInfo deployerEnvironmentInfo = this.appDeployer.environmentInfo();
			final RuntimeEnvironmentDetails deployerInfo = new RuntimeEnvironmentDetails();

			deployerInfo.setDeployerImplementationVersion(deployerEnvironmentInfo.getImplementationVersion());
			deployerInfo.setDeployerName(deployerEnvironmentInfo.getImplementationName());
			deployerInfo.setDeployerSpiVersion(deployerEnvironmentInfo.getSpiVersion());
			deployerInfo.setJavaVersion(deployerEnvironmentInfo.getJavaVersion());
			deployerInfo.setPlatformApiVersion(deployerEnvironmentInfo.getPlatformApiVersion());
			deployerInfo.setPlatformClientVersion(deployerEnvironmentInfo.getPlatformClientVersion());
			deployerInfo.setPlatformHostVersion(deployerEnvironmentInfo.getPlatformHostVersion());
			deployerInfo.setPlatformSpecificInfo(deployerEnvironmentInfo.getPlatformSpecificInfo());
			deployerInfo.setPlatformHostVersion(deployerEnvironmentInfo.getPlatformHostVersion());
			deployerInfo.setPlatformType(deployerEnvironmentInfo.getPlatformType());
			deployerInfo.setSpringBootVersion(deployerEnvironmentInfo.getSpringBootVersion());
			deployerInfo.setSpringVersion(deployerEnvironmentInfo.getSpringVersion());

			runtimeEnvironment.setAppDeployer(deployerInfo);
		}

		if (this.taskLauncher != null) {
			final RuntimeEnvironmentInfo taskLauncherEnvironmentInfo = this.taskLauncher.environmentInfo();
			final RuntimeEnvironmentDetails taskLauncherInfo = new RuntimeEnvironmentDetails();

			taskLauncherInfo.setDeployerImplementationVersion(taskLauncherEnvironmentInfo.getImplementationVersion());
			taskLauncherInfo.setDeployerName(taskLauncherEnvironmentInfo.getImplementationName());
			taskLauncherInfo.setDeployerSpiVersion(taskLauncherEnvironmentInfo.getSpiVersion());
			taskLauncherInfo.setJavaVersion(taskLauncherEnvironmentInfo.getJavaVersion());
			taskLauncherInfo.setPlatformApiVersion(taskLauncherEnvironmentInfo.getPlatformApiVersion());
			taskLauncherInfo.setPlatformClientVersion(taskLauncherEnvironmentInfo.getPlatformClientVersion());
			taskLauncherInfo.setPlatformHostVersion(taskLauncherEnvironmentInfo.getPlatformHostVersion());
			taskLauncherInfo.setPlatformSpecificInfo(taskLauncherEnvironmentInfo.getPlatformSpecificInfo());
			taskLauncherInfo.setPlatformHostVersion(taskLauncherEnvironmentInfo.getPlatformHostVersion());
			taskLauncherInfo.setPlatformType(taskLauncherEnvironmentInfo.getPlatformType());
			taskLauncherInfo.setSpringBootVersion(taskLauncherEnvironmentInfo.getSpringBootVersion());
			taskLauncherInfo.setSpringVersion(taskLauncherEnvironmentInfo.getSpringVersion());

			runtimeEnvironment.setTaskLauncher(taskLauncherInfo);
		}

		aboutResource.setRuntimeEnvironment(runtimeEnvironment);
		aboutResource.add(ControllerLinkBuilder.linkTo(AboutController.class).withSelfRel());

		return aboutResource;
	}
}
