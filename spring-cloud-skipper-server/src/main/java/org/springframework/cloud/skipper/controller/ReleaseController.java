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
import org.springframework.cloud.skipper.domain.UpdateRequest;
import org.springframework.cloud.skipper.service.ReleaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Release related operations such as un-deploy/rollback/status etc.
 *
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/release")
public class ReleaseController {

	private final ReleaseService releaseService;

	@Autowired
	public ReleaseController(ReleaseService releaseService) {
		this.releaseService = releaseService;
	}

	@RequestMapping(path = "/status/{name}/{version}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Release status(@PathVariable("name") String name, @PathVariable("version") int version) {
		return this.releaseService.status(name, version);
	}

	@RequestMapping(path = "/update", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release update(@RequestBody UpdateRequest updateRequest) {
		return this.releaseService.update(updateRequest);
	}

	@RequestMapping(path = "/undeploy/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release undeploy(@PathVariable("name") String releaseName) {
		return this.releaseService.undeploy(releaseName);
	}

	@RequestMapping(path = "/rollback/{name}/{version}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release rollback(@PathVariable("name") String releaseName,
			@PathVariable("version") int rollbackVersion) {
		return this.releaseService.rollback(releaseName, rollbackVersion);
	}
}
