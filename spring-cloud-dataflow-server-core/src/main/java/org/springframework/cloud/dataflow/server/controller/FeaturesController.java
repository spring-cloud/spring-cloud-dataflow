/*
 * Copyright 2016-2017 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.resource.FeaturesInfoResource;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that provides features that are enabled/disabled on the dataflow
 * server.
 *
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @deprecated Functionality now provided by {@link AboutController}
 */
@RestController
@RequestMapping("/features")
@ExposesResourceFor(FeaturesInfoResource.class)
public class FeaturesController {

	private final FeaturesProperties featuresProperties;

	public FeaturesController(FeaturesProperties featuresProperties) {
		this.featuresProperties = featuresProperties;
	}

	/**
	 * @return the features that are enabled/disabled on the dataflow server.
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public FeaturesInfoResource getSecurityInfo() {
		FeaturesInfoResource featuresInfoResource = new FeaturesInfoResource();
		featuresInfoResource.setAnalyticsEnabled(featuresProperties.isAnalyticsEnabled());
		featuresInfoResource.setStreamsEnabled(featuresProperties.isStreamsEnabled());
		featuresInfoResource.setTasksEnabled(featuresProperties.isTasksEnabled());
		return featuresInfoResource;
	}
}
