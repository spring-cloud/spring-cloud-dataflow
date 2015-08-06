/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.data.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.data.rest.resource.TaskDefinitionResource;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the root resource of the admin server.
 *
 * @author Patrick Peralta
 */
@RestController
public class AdminController {

	/**
	 * Contains links pointing to controllers backing an entity type
	 * (such as streams).
	 */
	private final EntityLinks entityLinks;

	/**
	 * Construct an {@code AdminController}.
	 *
	 * @param entityLinks holder of links to controllers and their associated
	 *                    entity types
	 */
	@Autowired
	public AdminController(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	/**
	 * Return a {@link ResourceSupport} object containing the resources
	 * served by the admin server.
	 *
	 * @return {@code ResourceSupport} object containing the admin server's resources
	 */
	@RequestMapping
	public ResourceSupport info() {
		ResourceSupport resourceSupport = new ResourceSupport();
		resourceSupport.add(entityLinks.linkFor(StreamDefinitionResource.class).withRel("streams"));
		resourceSupport.add(entityLinks.linkFor(TaskDefinitionResource.class).withRel("tasks"));
		return resourceSupport;
	}

}
