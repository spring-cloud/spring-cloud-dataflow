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
package org.springframework.cloud.dataflow.server.controller;

import org.springframework.cloud.dataflow.rest.resource.AppStarter;
import org.springframework.cloud.dataflow.rest.resource.AppStartersInfoResource;
import org.springframework.cloud.dataflow.server.config.features.AppStartersProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that provides a list of app starter uris.
 *
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/app-starters")
@ExposesResourceFor(AppStartersInfoResource.class)
public class AppStartersController {

	private final AppStartersProperties appStartersProperties;
	private final FeaturesProperties featuresProperties;

	public AppStartersController(AppStartersProperties appStartersProperties, FeaturesProperties featuresProperties) {
		this.appStartersProperties = appStartersProperties;
		this.featuresProperties = featuresProperties;
	}

	/**
	 * Returns an {@link AppStartersInfoResource} containing a list of {@link AppStarter}.
	 * Depending on {@link FeaturesProperties}, Stream App Starters and/or Task App Starters
	 * will be returned.
	 *
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public AppStartersInfoResource getAppStarters() {
		final AppStartersInfoResource appStarterInfoResource = new AppStartersInfoResource();

		if (featuresProperties.isTasksEnabled()) {
			appStarterInfoResource.setTaskAppStarters(appStartersProperties.getTask());
		}

		if (featuresProperties.isStreamsEnabled()) {
			appStarterInfoResource.setStreamAppStarters(appStartersProperties.getStream());
		}

		return appStarterInfoResource;
	}
}
