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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.domain.AboutInfo;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.server.service.PackageService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Skipper server related operations such as install, upgrade, delete,
 * and rollback.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/api")
public class SkipperController {

	private final ReleaseService releaseService;

	private final PackageService packageService;

	@Value("${info.app.name:#{null}}")
	private String appName;

	@Value("${info.app.version:#{null}}")
	private String appVersion;

	@Autowired
	public SkipperController(ReleaseService releaseService, PackageService packageService) {
		this.releaseService = releaseService;
		this.packageService = packageService;
	}

	@RequestMapping(path = "/about", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public AboutInfo getAboutInfo() {
		return new AboutInfo(appName, appVersion);
	}

	@RequestMapping(path = "/upload", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public PackageMetadata upload(@RequestBody UploadRequest uploadRequest) {
		return this.packageService.upload(uploadRequest);
	}

	@RequestMapping(path = "/install", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release install(@RequestBody InstallRequest installRequest) {
		return this.releaseService.install(installRequest);
	}

	@RequestMapping(path = "/install/{id}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release install(@PathVariable("id") String id, @RequestBody InstallProperties installProperties) {
		return this.releaseService.install(id, installProperties);
	}

	@RequestMapping(path = "/status/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Info status(@PathVariable("name") String name) {
		return this.releaseService.status(name);
	}

	@RequestMapping(path = "/status/{name}/{version}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Info status(@PathVariable("name") String name, @PathVariable("version") int version) {
		return this.releaseService.status(name, version);
	}

	@RequestMapping(path = "/manifest/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public String manifest(@PathVariable("name") String name) {
		return this.releaseService.manifest(name);
	}

	@RequestMapping(path = "/manifest/{name}/{version}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public String manifest(@PathVariable("name") String name, @PathVariable("version") int version) {
		return this.releaseService.manifest(name, version);
	}

	@RequestMapping(path = "/upgrade", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release upgrade(@RequestBody UpgradeRequest upgradeRequest) {
		return this.releaseService.upgrade(upgradeRequest);
	}

	@RequestMapping(path = "/rollback/{name}/{version}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release rollback(@PathVariable("name") String releaseName,
			@PathVariable("version") int rollbackVersion) {
		return this.releaseService.rollback(releaseName, rollbackVersion);
	}

	@RequestMapping(path = "/delete/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release delete(@PathVariable("name") String releaseName) {
		return this.releaseService.delete(releaseName);
	}

	@RequestMapping(path = "/history/{name}/{max}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public List<Release> history(@PathVariable("name") String releaseName,
			@PathVariable("max") int maxRevisions) {
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

	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Release doesn't exist")
	@ExceptionHandler(ReleaseNotFoundException.class)
	public void releaseNotExist() {
		// handle ReleaseNotFoundException by returning 404
	}
}
