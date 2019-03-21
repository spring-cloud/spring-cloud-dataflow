/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.server.index;

import org.springframework.cloud.skipper.server.controller.AboutController;
import org.springframework.cloud.skipper.server.controller.PackageController;
import org.springframework.cloud.skipper.server.controller.ReleaseController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
@Component
public class SkipperLinksResourceProcessor implements ResourceProcessor<RepositoryLinksResource> {


	@Override
	public RepositoryLinksResource process(RepositoryLinksResource resource) {
		resource.add(ControllerLinkBuilder.linkTo(methodOn(AboutController.class).getAboutResource()).withRel("about"));
		resource.add(ControllerLinkBuilder.linkTo(ReleaseController.class).withRel("release"));
		resource.add(ControllerLinkBuilder.linkTo(PackageController.class).withRel("package"));
		return resource;
	}
}
