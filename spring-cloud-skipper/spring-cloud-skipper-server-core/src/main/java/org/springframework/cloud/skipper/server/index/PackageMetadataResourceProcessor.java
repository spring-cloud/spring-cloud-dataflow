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

import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.server.controller.PackageController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@Component
public class PackageMetadataResourceProcessor implements RepresentationModelProcessor<EntityModel<PackageMetadata>> {

	@Override
	public EntityModel<PackageMetadata> process(EntityModel<PackageMetadata> packageMetadataResource) {
		Link installLink = linkTo(
				methodOn(PackageController.class).install(packageMetadataResource.getContent().getId(), null))
				.withRel("install");
		packageMetadataResource.add(installLink);
		return packageMetadataResource;
	}
}
