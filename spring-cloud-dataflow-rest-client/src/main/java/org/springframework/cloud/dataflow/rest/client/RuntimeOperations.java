/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import org.springframework.cloud.dataflow.rest.resource.ModuleStatusResource;
import org.springframework.hateoas.PagedResources;

/**
 * Defines operations available for obtaining information about deployed modules.
 *
 * @author Eric Bottard
 */
public interface RuntimeOperations {

	/**
	 * Return runtime information about all deployed modules.
	 */
	PagedResources<ModuleStatusResource> status();

	/**
	 * Return runtime information about a single module deployment.
	 */
	ModuleStatusResource status(String moduleDeploymentId);
}
