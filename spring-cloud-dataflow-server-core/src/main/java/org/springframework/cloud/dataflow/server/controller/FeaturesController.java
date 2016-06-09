package org.springframework.cloud.dataflow.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.resource.FeaturesInfoResource;
import org.springframework.cloud.dataflow.rest.resource.security.SecurityInfoResource;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that provides features that are enabled/disabled on the dataflow server.
 *
 * @author Ilayaperumal Gopinathan
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
	 * Return features that are enabled/disabled on the dataflow server.
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
