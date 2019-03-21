/*
 * Copyright 2019 the original author or authors.
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

import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for task launching platform related operations.
 *
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/tasks/platforms")
@ExposesResourceFor(LauncherResource.class)
public class TaskPlatformController {

	private final LauncherRepository launcherRepository;

	private final Assembler launcherAssembler = new Assembler();

	public TaskPlatformController(LauncherRepository LauncherRepository) {
		this.launcherRepository = LauncherRepository;
	}

	/**
	 * Returns the list of platform accounts available for launching tasks.
	 * @param pageable the Pageable request
	 * @param assembler the paged resource assembler for Launcher
	 * @return the paged resources of type {@link LauncherResource}
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<LauncherResource> list(Pageable pageable,
			PagedResourcesAssembler<Launcher> assembler) {
		return assembler.toResource(this.launcherRepository.findAll(pageable), this.launcherAssembler);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation that converts
	 * {@link Launcher}s to {@link LauncherResource}s.
	 */
	private static class Assembler extends ResourceAssemblerSupport<Launcher, LauncherResource> {

		public Assembler() {
			super(TaskPlatformController.class, LauncherResource.class);
		}

		@Override
		public LauncherResource toResource(Launcher launcher) {
			return createResourceWithId(launcher.getId(), launcher);
		}

		@Override
		public LauncherResource instantiateResource(Launcher launcher) {
			return new LauncherResource(launcher);
		}
	}
}
