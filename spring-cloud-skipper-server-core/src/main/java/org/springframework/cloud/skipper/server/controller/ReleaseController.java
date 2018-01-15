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
package org.springframework.cloud.skipper.server.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.domain.DeleteProperties;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * REST controller for Skipper release related operations.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/api/release")
public class ReleaseController {

	@Value("${info.app.name:#{null}}")
	private String appName;

	@Value("${info.app.version:#{null}}")
	private String appVersion;

	private final ReleaseService releaseService;

	private final SkipperStateMachineService skipperStateMachineService;

	public ReleaseController(ReleaseService releaseService,
			SkipperStateMachineService skipperStateMachineService) {
		this.releaseService = releaseService;
		this.skipperStateMachineService = skipperStateMachineService;
	}

	@RequestMapping(method = RequestMethod.GET)
	public ReleaseControllerLinksResource resourceLinks() {
		ReleaseControllerLinksResource resource = new ReleaseControllerLinksResource();
		resource.add(ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).status(null))
							.withRel("status/name"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).status(null, null))
							.withRel("status/name/version"));
		resource.add(
				ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).manifest(null))
						.withRel("manifest"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).manifest(null, null))
							.withRel("manifest/name/version"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).upgrade(null))
							.withRel("upgrade"));
		resource.add(
				ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).rollback(null, null))
						.withRel("rollback"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).delete(null, null))
							.withRel("delete"));
		resource.add(
				ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).history(null, null))
						.withRel("history"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).list())
							.withRel("list"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(ReleaseController.class).list(null))
							.withRel("list/name"));
		return resource;
	}

	// Release commands

	@RequestMapping(path = "/status/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Info status(@PathVariable("name") String name) {
		return this.releaseService.status(name);
	}

	@RequestMapping(path = "/status/{name}/{version}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Info status(@PathVariable("name") String name, @PathVariable("version") Integer version) {
		return this.releaseService.status(name, version);
	}

	@RequestMapping(path = "/manifest/{name}", method = RequestMethod.GET)
	public ResponseEntity<String> manifest(@PathVariable("name") String name) {
		return new ResponseEntity<String>(this.releaseService.manifest(name), HttpStatus.OK);
	}

	@RequestMapping(path = "/manifest/{name}/{version}", method = RequestMethod.GET)
	public ResponseEntity<String> manifest(@PathVariable("name") String name,
			@PathVariable("version") Integer version) {
		return new ResponseEntity<String>(this.releaseService.manifest(name, version), HttpStatus.OK);
	}

	@RequestMapping(path = "/upgrade", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release upgrade(@RequestBody UpgradeRequest upgradeRequest) {
		return this.skipperStateMachineService.upgradeRelease(upgradeRequest);
	}

	@RequestMapping(path = "/rollback/{name}/{version}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release rollback(@PathVariable("name") String releaseName,
			@PathVariable("version") Integer rollbackVersion) {
		return this.skipperStateMachineService.rollbackRelease(releaseName, rollbackVersion);
	}

	@RequestMapping(path = "/delete/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release delete(@PathVariable("name") String releaseName,
			@RequestBody DeleteProperties deleteProperties) {
		return this.skipperStateMachineService.deleteRelease(releaseName, deleteProperties);
	}

	@RequestMapping(path = "/history/{name}/{max}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public List<Release> history(@PathVariable("name") String releaseName,
			@PathVariable("max") Integer maxRevisions) {
		return this.releaseService.history(releaseName, maxRevisions);
	}

	@RequestMapping(path = "/list", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public List<Release> list() {
		return this.releaseService.list();
	}

	@RequestMapping(path = "/list/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public List<Release> list(@PathVariable("name") String releaseName) {
		return this.releaseService.list(releaseName);
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Release not found")
	@ExceptionHandler(ReleaseNotFoundException.class)
	public void handleReleaseNotFoundException() {
		// needed for server not to log 500 errors
	}

	/**
	 * @author Mark Pollack
	 */
	public static class ReleaseControllerLinksResource extends ResourceSupport {

		public ReleaseControllerLinksResource() {
		}
	}

	@ExceptionHandler(PackageDeleteException.class)
	public ResponseEntity<String> handlePackageDeleteException(PackageDeleteException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
	}
}
