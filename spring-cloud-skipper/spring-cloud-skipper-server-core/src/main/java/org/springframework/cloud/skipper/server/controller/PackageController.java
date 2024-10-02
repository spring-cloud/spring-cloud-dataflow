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
package org.springframework.cloud.skipper.server.controller;

import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.server.controller.support.PackageMetadataResourceAssembler;
import org.springframework.cloud.skipper.server.controller.support.ReleaseResourceAssembler;
import org.springframework.cloud.skipper.server.service.PackageMetadataService;
import org.springframework.cloud.skipper.server.service.PackageService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for Skipper package related operations.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/api/package")
public class PackageController {

	private final SkipperStateMachineService skipperStateMachineService;

	private final PackageService packageService;

	private final PackageMetadataService packageMetadataService;

	private PackageMetadataResourceAssembler packageMetadataResourceAssembler = new PackageMetadataResourceAssembler();

	private ReleaseResourceAssembler releaseResourceAssembler = new ReleaseResourceAssembler();

	public PackageController(PackageService packageService, PackageMetadataService packageMetadataService,
			SkipperStateMachineService skipperStateMachineService) {
		this.packageService = packageService;
		this.packageMetadataService = packageMetadataService;
		this.skipperStateMachineService = skipperStateMachineService;
	}

	@GetMapping
	public PackageControllerLinksResource resourceLinks() {
		PackageControllerLinksResource resource = new PackageControllerLinksResource();
		resource.add(
				WebMvcLinkBuilder.linkTo(methodOn(PackageController.class).upload(null))
						.withRel("upload"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(PackageController.class).install(null))
				.withRel("install"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(PackageController.class).install(null, null))
				.withRel("install/id"));
		return resource;
	}

	@PostMapping("/upload")
	@ResponseStatus(HttpStatus.CREATED)
	public EntityModel<PackageMetadata> upload(@RequestBody UploadRequest uploadRequest) {
		return this.packageMetadataResourceAssembler.toModel(this.packageService.upload(uploadRequest));
	}

	@PostMapping("/install")
	@ResponseStatus(HttpStatus.CREATED)
	public EntityModel<Release> install(@RequestBody InstallRequest installRequest) {
		return this.releaseResourceAssembler.toModel(this.skipperStateMachineService.installRelease(installRequest));
	}

	@PostMapping("/install/{id}")
	@ResponseStatus(HttpStatus.CREATED)
	public EntityModel<Release> install(@PathVariable Long id, @RequestBody InstallProperties installProperties) {
		return this.releaseResourceAssembler.toModel(this.skipperStateMachineService.installRelease(id, installProperties));
	}

	@DeleteMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public void packageDelete(@PathVariable String name) {
		this.packageMetadataService.deleteIfAllReleasesDeleted(name, PackageMetadataService.DEFAULT_RELEASE_ACTIVITY_CHECK);
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Release not found")
	@ExceptionHandler(ReleaseNotFoundException.class)
	public void handleReleaseNotFoundException() {
		// needed for server not to log 500 errors
	}

	@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Package deletion error")
	@ExceptionHandler(PackageDeleteException.class)
	public void handlePackageDeleteException() {
		// needed for server not to log 500 errors
	}

	@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Skipper server exception")
	@ExceptionHandler(SkipperException.class)
	public void handleSkipperException() {
		// needed for server not to log 500 errors
	}

	public static class PackageControllerLinksResource extends RepresentationModel {

		public PackageControllerLinksResource() {
		}
	}

}
