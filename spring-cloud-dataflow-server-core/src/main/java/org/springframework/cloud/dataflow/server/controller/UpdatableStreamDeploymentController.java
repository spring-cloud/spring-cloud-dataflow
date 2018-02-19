/*
 * Copyright 2018 the original author or authors.
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

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.UpdatableStreamService;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for deployment operations on {@link StreamDefinition}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Janne Valkealahti
 * @author Christian Tzolov
 */
@RestController
@RequestMapping("/streams/deployments")
@ExposesResourceFor(StreamDeploymentResource.class)
public class UpdatableStreamDeploymentController extends StreamDeploymentController {

	private static final Logger logger = LoggerFactory.getLogger(UpdatableStreamDeploymentController.class);

	private final UpdatableStreamService streamService;

	/**
	 * Extends the {@code StreamDeploymentController} with support for stream update, rollback, update history.
	 * <ul>
	 * <li>CRUD operations to the provided {@link StreamDefinitionRepository}</li>
	 * <li>deployment operations to the provided SkipperStreamDeployer via {@link UpdatableStreamService}</li>
	 * </ul>
	 *
	 * @param repository the repository this controller will use for stream CRUD operations
	 * @param updatableStreamService the underlying UpdatableStreamService to deploy the stream
	 */
	public UpdatableStreamDeploymentController(StreamDefinitionRepository repository,
			UpdatableStreamService updatableStreamService) {
		super(repository, updatableStreamService);
		this.streamService = updatableStreamService;
	}

	@RequestMapping(value = "/update/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void update(@PathVariable("name") String name, @RequestBody UpdateStreamRequest updateStreamRequest) {
		this.streamService.updateStream(name, updateStreamRequest);
	}

	@RequestMapping(value = "/rollback/{name}/{version}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void rollback(@PathVariable("name") String name, @PathVariable("version") int version) {
		this.streamService.rollbackStream(name, version);
	}

	@RequestMapping(value = "/manifest/{name}/{version}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public String manifest(@PathVariable("name") String name, @PathVariable("version") int version) {
		return this.streamService.manifest(name, version);
	}

	@RequestMapping(path = "/history/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<Release> history(@PathVariable("name") String releaseName) {
		return this.streamService.history(releaseName);
	}

	@RequestMapping(path = "/platform/list", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<Deployer> platformList() {
		return this.streamService.platformList();
	}
}
