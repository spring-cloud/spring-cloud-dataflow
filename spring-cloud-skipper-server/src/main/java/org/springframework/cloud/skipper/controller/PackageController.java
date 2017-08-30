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
package org.springframework.cloud.skipper.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.skipperpackage.Deployproperties;
import org.springframework.cloud.skipper.domain.skipperpackage.UndeployProperties;
import org.springframework.cloud.skipper.domain.skipperpackage.UpdateProperties;
import org.springframework.cloud.skipper.service.ReleaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/package")
public class PackageController {

	private ReleaseService releaseService;

	@Autowired
	public PackageController(ReleaseService releaseService) {
		this.releaseService = releaseService;
	}

	@RequestMapping(path = "/{id}/deploy", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release deploy(@PathVariable("id") String id, @RequestBody Deployproperties deployproperties) {
		return this.releaseService.deploy(id, deployproperties);
	}

	@RequestMapping(path = "/undeploy", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release undeploy(@RequestBody UndeployProperties undeployProperties) {
		return this.releaseService.undeploy(undeployProperties);
	}

	@RequestMapping(path = "/update", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release update(@RequestBody UpdateProperties updateProperties) {
		return this.releaseService.update(updateProperties);
	}
}
