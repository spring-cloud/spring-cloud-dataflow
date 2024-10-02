/*
 * Copyright 2017-2022 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.ReleaseUpgradeException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.cloud.skipper.domain.CancelRequest;
import org.springframework.cloud.skipper.domain.CancelResponse;
import org.springframework.cloud.skipper.domain.DeleteProperties;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.LogInfo;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.ScaleRequest;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.controller.support.InfoResourceAssembler;
import org.springframework.cloud.skipper.server.controller.support.ManifestResourceAssembler;
import org.springframework.cloud.skipper.server.controller.support.ReleaseResourceAssembler;
import org.springframework.cloud.skipper.server.controller.support.SimpleResourceAssembler;
import org.springframework.cloud.skipper.server.service.ActuatorService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for Skipper release related operations.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author David Turanski
 */
@RestController
@RequestMapping("/api/release")
public class ReleaseController {

	private final ReleaseService releaseService;

	private final SkipperStateMachineService skipperStateMachineService;

	private final ReleaseResourceAssembler releaseResourceAssembler = new ReleaseResourceAssembler();

	private final ManifestResourceAssembler manifestResourceAssembler = new ManifestResourceAssembler();

	private final InfoResourceAssembler infoResourceAssembler = new InfoResourceAssembler();

	private final ActuatorService actuatorService;

	@Value("${info.app.name:#{null}}")
	private String appName;

	@Value("${info.app.version:#{null}}")
	private String appVersion;

	public ReleaseController(ReleaseService releaseService,
			SkipperStateMachineService skipperStateMachineService, ActuatorService actuatorService) {
		this.releaseService = releaseService;
		this.skipperStateMachineService = skipperStateMachineService;
		this.actuatorService = actuatorService;
	}

	@GetMapping
	public ReleaseControllerLinksResource resourceLinks() {
		ReleaseControllerLinksResource resource = new ReleaseControllerLinksResource();
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class)
				.getFromActuator(null, null, null, null, null))
				.withRel("actuator/name/app/id"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class)
						.postToActuator(null, null, null, null, null))
				.withRel("actuator/name/app/id"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).log(null))
				.withRel("logs/name"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).status(null))
				.withRel("status/name"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).status(null, null))
				.withRel("status/name/version"));
		resource.add(
				WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).manifest(null))
						.withRel("manifest"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).manifest(null, null))
				.withRel("manifest/name/version"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).upgrade(null))
				.withRel("upgrade"));
		resource.add(
				WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).rollbackWithNamedVersion(null, 123))
						.withRel("rollback"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).list())
				.withRel("list"));
		resource.add(WebMvcLinkBuilder.linkTo(methodOn(ReleaseController.class).list(null))
				.withRel("list/name"));
		return resource;
	}

	// Release commands

	@GetMapping("/statuses")
	@ResponseStatus(HttpStatus.OK)
	public Mono<Map<String, Info>> statuses(@RequestParam String[] names) {
		return this.releaseService.statusReactive(names);
	}

	@GetMapping("/states")
	@ResponseStatus(HttpStatus.OK)
	public Mono<Map<String, Map<String, DeploymentState>>> states(@RequestParam String[] names) {
		return this.releaseService.states(names);
	}

	@GetMapping("/status/{name}")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Info> status(@PathVariable String name) {
		return this.infoResourceAssembler.toModel(this.releaseService.status(name));
	}

	@GetMapping("/status/{name}/{version}")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Info> status(@PathVariable String name, @PathVariable Integer version) {
		return this.infoResourceAssembler.toModel(this.releaseService.status(name, version));
	}

	@GetMapping("/logs/{name}")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<LogInfo> log(@PathVariable String name) {
		return new SimpleResourceAssembler<LogInfo>().toModel(this.releaseService.getLog(name));
	}

	@GetMapping("/logs/{name}/{appName}")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<LogInfo> log(@PathVariable String name, @PathVariable String appName) {
		return new SimpleResourceAssembler<LogInfo>().toModel(this.releaseService.getLog(name, appName));
	}

	@GetMapping("/manifest/{name}")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Manifest> manifest(@PathVariable String name) {
		return this.manifestResourceAssembler.toModel(this.releaseService.manifest(name));
	}

	@GetMapping("/manifest/{name}/{version}")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Manifest> manifest(@PathVariable String name,
			@PathVariable Integer version) {
		return this.manifestResourceAssembler.toModel(this.releaseService.manifest(name, version));
	}

	@PostMapping("/scale/{name}")
	@ResponseStatus(HttpStatus.CREATED)
	public EntityModel<Release> scale(@PathVariable String name, @RequestBody ScaleRequest scaleRequest) {
		Release release = this.skipperStateMachineService.scaleRelease(name, scaleRequest);
		return this.releaseResourceAssembler.toModel(release);
	}

	@PostMapping("/upgrade")
	@ResponseStatus(HttpStatus.CREATED)
	public EntityModel<Release> upgrade(@RequestBody UpgradeRequest upgradeRequest) {
		Release release = this.skipperStateMachineService.upgradeRelease(upgradeRequest);
		return this.releaseResourceAssembler.toModel(release);
	}

	@PostMapping("/rollback")
	@ResponseStatus(HttpStatus.CREATED)
	public EntityModel<Release> rollback(@RequestBody RollbackRequest rollbackRequest) {
		Release release = this.skipperStateMachineService.rollbackRelease(rollbackRequest);
		return this.releaseResourceAssembler.toModel(release);
	}

	@PostMapping("/rollback/{name}/{version}")
	@ResponseStatus(HttpStatus.CREATED)
	@Deprecated
	public EntityModel<Release> rollbackWithNamedVersion(@PathVariable("name") String releaseName,
			@PathVariable("version") int rollbackVersion) {
		Release release = this.skipperStateMachineService
				.rollbackRelease(new RollbackRequest(releaseName, rollbackVersion));
		return this.releaseResourceAssembler.toModel(release);
	}

	@DeleteMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Release> delete(@PathVariable("name") String releaseName) {
		return deleteRelease(releaseName, false);
	}

	@DeleteMapping("/{name}/package")
	@ResponseStatus(HttpStatus.OK)
	public EntityModel<Release> deleteWithPackage(@PathVariable("name") String releaseName) {
		return deleteRelease(releaseName, true);
	}

	private EntityModel<Release> deleteRelease(String releaseName, boolean canDeletePackage) {
		DeleteProperties deleteProperties = new DeleteProperties();
		deleteProperties.setDeletePackage(canDeletePackage);
		Release release = this.skipperStateMachineService.deleteRelease(releaseName, deleteProperties);
		return this.releaseResourceAssembler.toModel(release);
	}

	@PostMapping("/cancel")
	@ResponseStatus(HttpStatus.OK)
	public CancelResponse cancel(@RequestBody CancelRequest cancelRequest) {
		boolean accepted = this.skipperStateMachineService.cancelRelease(cancelRequest.getReleaseName());
		return new CancelResponse(accepted);
	}

	@GetMapping("/list")
	@ResponseStatus(HttpStatus.OK)
	public CollectionModel<EntityModel<Release>> list() {
		List<Release> releaseList = this.releaseService.list();
		CollectionModel<EntityModel<Release>> resources = this.releaseResourceAssembler.toCollectionModel(releaseList);
		return resources;
	}

	@GetMapping("/list/{name}")
	@ResponseStatus(HttpStatus.OK)
	public CollectionModel<EntityModel<Release>> list(@PathVariable("name") String releaseName) {
		List<Release> releaseList = this.releaseService.list(releaseName);
		CollectionModel<EntityModel<Release>> resources = this.releaseResourceAssembler.toCollectionModel(releaseList);
		return resources;
	}

	@GetMapping("/actuator/{name}/{app}/{id}")
	public ResponseEntity<String> getFromActuator(
			@PathVariable("name") String releaseName,
			@PathVariable("app") String appName,
			@PathVariable("id") String appId,
			@RequestParam String endpoint,
			@Nullable @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
		return new ResponseEntity<>(this.actuatorService.getFromActuator(
				releaseName, appName, appId, endpoint,
				Optional.ofNullable(auth)), HttpStatus.OK);
	}

	@PostMapping("/actuator/{name}/{app}/{id}")
	public ResponseEntity<?> postToActuator(
			@PathVariable("name") String releaseName,
			@PathVariable("app") String appName,
			@PathVariable("id") String appId,
			@RequestBody ActuatorPostRequest postRequest,
			@Nullable @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {

		return new ResponseEntity<>(
				this.actuatorService.postToActuator(releaseName, appName, appId, postRequest,
						Optional.ofNullable(auth)),
				HttpStatus.OK);
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

	@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Release upgrade exception")
	@ExceptionHandler(ReleaseUpgradeException.class)
	public void handleReleaseUpgradeException() {
		// needed for server not to log 500 errors
	}

	@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Skipper server exception")
	@ExceptionHandler(SkipperException.class)
	public void handleSkipperException() {
		// needed for server not to log 500 errors
	}

	/**
	 * @author Mark Pollack
	 */
	public static class ReleaseControllerLinksResource extends RepresentationModel {

		public ReleaseControllerLinksResource() {
		}
	}
}
