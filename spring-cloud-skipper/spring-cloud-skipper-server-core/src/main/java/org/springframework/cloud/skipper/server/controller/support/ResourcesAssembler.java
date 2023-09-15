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

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

/**
 * Analogous to {@link RepresentationModelAssembler} but for resource collections.
 *
 * @author Greg Turnquist
 */
public interface ResourcesAssembler<T, D extends RepresentationModel<D>> {

	/**
	 * Converts all given entities into resources and wraps the collection as a resource as well.
	 *
	 * @see RepresentationModelAssembler#toModel(Object)
	 * @param entities must not be {@literal null}.
	 * @return {@link CollectionModel} containing {@link RepresentationModel} of {@code T}.
	 */
	CollectionModel<D> toCollectionModel(Iterable<? extends T> entities);
}
