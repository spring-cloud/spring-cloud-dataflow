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

import org.springframework.cloud.skipper.server.controller.PackageController;
import org.springframework.cloud.skipper.server.domain.PackageSummary;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Mark Pollack
 */
@Component
public class PackageSummaryResourceProcessor implements RepresentationModelProcessor<EntityModel<PackageSummary>> {

	@Override
	public EntityModel<PackageSummary> process(EntityModel<PackageSummary> packageSummaryResource) {
		Link link = linkTo(
				methodOn(PackageController.class).install(Long.valueOf(packageSummaryResource.getContent().getId()),
						null))
				.withRel("install");
		packageSummaryResource.add(link);
		return packageSummaryResource;
	}
}
