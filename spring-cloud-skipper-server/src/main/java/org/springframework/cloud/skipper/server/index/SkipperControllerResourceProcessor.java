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
package org.springframework.cloud.skipper.server.index;

import org.springframework.cloud.skipper.server.controller.SkipperController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * @author Gunnar Hillert
 */
@Component
public class SkipperControllerResourceProcessor implements ResourceProcessor<RepositoryLinksResource> {

	@Override
	public RepositoryLinksResource process(RepositoryLinksResource resource) {
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).getAboutInfo()).withRel("about"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).upload(null)).withRel("upload"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).install(null)).withRel("install"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).install(null, null)).withRel("install-with-package-id"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).status(null)).withRel("status"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).status(null, 123)).withRel("status-by-name-and-version"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).manifest(null)).withRel("manifest"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).manifest(null, 123)).withRel("manifest-by-name-and-version"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).upgrade(null)).withRel("upgrade"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).rollback(null, -1)).withRel("rollback"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).delete(null)).withRel("delete"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).history(null, 123)).withRel("history"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).list()).withRel("list"));
		resource.add(ControllerLinkBuilder.linkTo(methodOn(SkipperController.class).list(null)).withRel("list-by-name"));
		return resource;
	}
}
