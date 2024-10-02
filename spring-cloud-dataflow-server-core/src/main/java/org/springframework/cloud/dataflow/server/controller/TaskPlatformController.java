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
import org.springframework.cloud.dataflow.server.service.LauncherService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	private final LauncherService launcherService;

	private final Assembler launcherAssembler = new Assembler();

	public TaskPlatformController(LauncherService launcherService) {
		this.launcherService = launcherService;
	}

	/**
	 * Returns the list of platform accounts available for launching tasks.
	 * @param pageable the Pageable request
	 * @param schedulesEnabled optional criteria to indicate enabled schedules.
	 * @param assembler the paged resource assembler for Launcher*
	 * @return the paged resources of type {@link LauncherResource}
	 */
	@GetMapping("")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<LauncherResource> list(
			Pageable pageable,
			@RequestParam(required = false) String schedulesEnabled,
			PagedResourcesAssembler<Launcher> assembler
	) {
		PagedModel<LauncherResource> result;
		if(StringUtils.hasText(schedulesEnabled) && schedulesEnabled.toLowerCase().equals("true")) {
			result = assembler.toModel(this.launcherService.getLaunchersWithSchedules(pageable), this.launcherAssembler);
		} else {
			result = assembler.toModel(this.launcherService.getAllLaunchers(pageable), this.launcherAssembler);
		}
		return result;
	}

	/**
	 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that converts
	 * {@link Launcher}s to {@link LauncherResource}s.
	 */
	private static class Assembler extends RepresentationModelAssemblerSupport<Launcher, LauncherResource> {

		public Assembler() {
			super(TaskPlatformController.class, LauncherResource.class);
		}

		@Override
		public LauncherResource toModel(Launcher launcher) {
			return createModelWithId(launcher.getId(), launcher);
		}

		@Override
		public LauncherResource instantiateModel(Launcher launcher) {
			return new LauncherResource(launcher);
		}
	}
}
