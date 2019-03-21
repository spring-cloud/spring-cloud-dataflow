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

package org.springframework.cloud.dataflow.server.controller;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.SkipperStreamService;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for deployment operations on {@link StreamDefinition}s. Extends the
 * {@link StreamDeploymentController} adding support for stream update, rollback, and
 * update history by delegating to {@link SkipperStreamService}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 * @author Christian Tzolov
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/streams/deployments")
@ExposesResourceFor(StreamDeploymentResource.class)
public class SkipperStreamDeploymentController extends StreamDeploymentController {

	private static final Logger logger = LoggerFactory.getLogger(SkipperStreamDeploymentController.class);

	private final SkipperStreamService skipperStreamService;

	/**
	 * Construct a new UpdatableStreamDeploymentController, given a
	 * {@link StreamDeploymentController} and {@link SkipperStreamService}
	 *
	 * @param repository the repository this controller will use for stream CRUD operations
	 * @param skipperStreamService the underlying UpdatableStreamService to deploy the stream
	 */
	public SkipperStreamDeploymentController(StreamDefinitionRepository repository,
			SkipperStreamService skipperStreamService) {
		super(repository, skipperStreamService);
		this.skipperStreamService = skipperStreamService;
	}

	@RequestMapping(value = "/update/{name}", method = RequestMethod.POST)
	public ResponseEntity<Void> update(@PathVariable("name") String name, @RequestBody UpdateStreamRequest updateStreamRequest) {
		this.skipperStreamService.updateStream(name, updateStreamRequest);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@RequestMapping(value = "/rollback/{name}/{version}", method = RequestMethod.POST)
	public ResponseEntity<Void> rollback(@PathVariable("name") String name, @PathVariable("version") Integer version) {
		this.skipperStreamService.rollbackStream(name, version);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@RequestMapping(value = "/manifest/{name}/{version}", method = RequestMethod.GET)
	public ResponseEntity<String> manifest(@PathVariable("name") String name, @PathVariable("version") Integer version) {
		return new ResponseEntity<String>(this.skipperStreamService.manifest(name, version), HttpStatus.OK);
	}

	@RequestMapping(path = "/history/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<Release> history(@PathVariable("name") String releaseName) {
		return this.skipperStreamService.history(releaseName);
	}

	@RequestMapping(path = "/platform/list", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<Deployer> platformList() {
		return this.skipperStreamService.platformList();
	}
}
