/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.hateoas.PagedModel;

/**
 * Defines operations available for obtaining information about deployed apps.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public interface RuntimeOperations {

	/**
	 * @return the runtime information about all deployed apps.
	 */
	PagedModel<AppStatusResource> status();

	/**
	 * @param deploymentId the deployment id
	 * @return the runtime information about a single app deployment.
	 */
	AppStatusResource status(String deploymentId);
}
