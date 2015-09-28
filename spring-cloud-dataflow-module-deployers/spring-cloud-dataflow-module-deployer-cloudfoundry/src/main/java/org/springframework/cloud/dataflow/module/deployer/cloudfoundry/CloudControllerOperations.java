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

package org.springframework.cloud.dataflow.module.deployer.cloudfoundry;

/**
 * Interface to cloud controller functions which wrap the REST interface required by the {@link ApplicationModuleDeployer}.
 *
 * @author Steve Powell
 */
interface CloudControllerOperations {

	/**
	 * Lists all the routes for the given domain and host.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.ListRoutes listRoutes(Requests.ListRoutes request);

	/**
	 * Creates an application definition and returns its id.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.CreateApplication createApplication(Requests.CreateApplication request);

	/**
	 * Create a new route in the given space and domain.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.CreateRoute createRoute(Requests.CreateRoute request);

	/**
	 * Creates a service binding to an application (given its id).
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.CreateServiceBinding createServiceBinding(Requests.CreateServiceBinding request);

	/**
	 * Deletes an application given its id. The application may be in any state.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.DeleteApplication deleteApplication(Requests.DeleteApplication request);

	/**
	 * Deletes a route given its id.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.DeleteRoute deleteRoute(Requests.DeleteRoute request);

	/**
	 * Obtains environment for an application given its id.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.GetApplicationEnvironment getApplicationEnvironment(Requests.GetApplicationEnvironment request);

	/**
	 * Obtains application statistics for every instance of an application given its id.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.GetApplicationStatistics getApplicationStatistics(Requests.GetApplicationStatistics request);

	/**
	 * Lists applications (with their ids) in a given space(id) and optionally matching a name.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.ListApplications listApplications(Requests.ListApplications request);

	/**
	 * Lists services bindings for a given application.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.ListServiceBindings listServiceBindings(Requests.ListServiceBindings request);

	/**
	 * Lists all the known organizations (with their ids).
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.ListOrganizations listOrganizations(Requests.ListOrganizations request);

	/**
	 * Lists all the known service instances (with their ids) in a given space (by id), optionally matching a name.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.ListServiceInstances listServiceInstances(Requests.ListServiceInstances request);

	/**
	 * Lists all shared domains.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.ListSharedDomains listSharedDomains(Requests.ListSharedDomains request);

	/**
	 * Lists all the known spaces (with their ids) in a given organization (given by id).
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.ListSpaces listSpaces(Requests.ListSpaces request);

	/**
	 * Map an existing route to an application.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.RouteMapping mapRoute(Requests.RouteMapping request);

	/**
	 * Remove a service binding from a particular application.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.RemoveServiceBinding removeServiceBinding(Requests.RemoveServiceBinding request);

	/**
	 * Uploads the bits required for an application (identified by its id) to run.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.UploadBits uploadBits(Requests.UploadBits request);

	/**
	 * Updates the state of an application (given by its id), for example, to start it.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.UpdateApplication updateApplication(Requests.UpdateApplication request);

	/**
	 * Un-map an existing route from an application.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	Responses.RouteMapping unmapRoute(Requests.RouteMapping request);

}
