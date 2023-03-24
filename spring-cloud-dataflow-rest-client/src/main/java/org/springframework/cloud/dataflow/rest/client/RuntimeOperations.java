/*
 * Copyright 2015-2022 the original author or authors.
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

import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamStatusResource;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;

/**
 * Defines operations available for obtaining information about deployed apps.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Chris Bono
 * @author Corneil du Plessis
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

	/**
	 * @param streamNames deployed stream names
	 * @return the runtime information about the deployed streams their apps and instances.
	 */
	PagedModel<StreamStatusResource> streamStatus(String... streamNames);

	/**
	 * Access an HTTP GET exposed actuator resource for a deployed app instance.
	 *
	 * @param appId      the application id
	 * @param instanceId the application instance id
	 * @param endpoint   the relative actuator path, e.g., {@code /info}
	 * @return the contents as JSON text
	 */
	String getFromActuator(String appId, String instanceId, String endpoint);

	/**
	 * Access an HTTP POST exposed actuator resource for a deployed app instance.
	 *
	 * @param appId      the application id
	 * @param instanceId the application instance id
	 * @param endpoint   the relative actuator path, e.g., {@code /info}
	 * @param data       map representing the data to post on request body
	 * @return response from actuator
	 */
	Object postToActuator(String appId, String instanceId, String endpoint, Map<String, Object> data);

	/**
	 * Provides for POST to application HTTP endpoint exposed via url property.
	 *
	 * @param appId      the application id
	 * @param instanceId the application instance id
	 * @param data       data to send to url. The mimetype should be in the Content-Type header if important.
	 * @param headers    post request headers.
	 *                   This method will return an exception
	 */
	void postToUrl(String appId, String instanceId, byte[] data, HttpHeaders headers);
}
