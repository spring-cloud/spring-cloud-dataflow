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
package org.springframework.cloud.skipper.server.controller.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.util.Assert;

/**
 * A {@link ResourceAssembler}/{@link ResourcesAssembler} that focuses purely on the domain type,
 * returning back {@link Resource} and {@link Resources} for that type instead of
 * {@link org.springframework.hateoas.ResourceSupport}.
 *
 * @author Greg Turnquist
 */
public class SimpleResourceAssembler<T> implements RepresentationModelAssembler<T, EntityModel<T>>, ResourcesAssembler<T, EntityModel<T>> {

	/**
	 * Converts the given entity into a {@link Resource}.
	 *
	 * @param entity the entity
	 * @return a resource for the entity.
	 */
	@Override
	public EntityModel<T> toModel(T entity) {

		EntityModel<T> resource = EntityModel.of(entity);

		addLinks(resource);

		return resource;
	}

	/**
	 * Converts all given entities into resources and wraps the collection as a resource as well.
	 *
	 * @see #toModel(Object)
	 * @param entities must not be {@literal null}.
	 * @return {@link Resources} containing {@link Resource} of {@code T}.
	 */
	public CollectionModel<EntityModel<T>> toCollectionModel(Iterable<? extends T> entities) {

		Assert.notNull(entities, "Entities must not be null!");
		List<EntityModel<T>> result = new ArrayList<EntityModel<T>>();

		for (T entity : entities) {
			result.add(toModel(entity));
		}

		CollectionModel<EntityModel<T>> resources = CollectionModel.of(result);

		addLinks(resources);

		return resources;
	}

	/**
	 * Define links to add to every individual {@link Resource}.
	 *
	 * @param resource
	 */
	protected void addLinks(EntityModel<T> resource) {
		// Default adds no links
	}

	/**
	 * Define links to add to the {@link Resources} collection.
	 *
	 * @param resources
	 */
	protected void addLinks(CollectionModel<EntityModel<T>> resources) {
		// Default adds no links.
	}
}
