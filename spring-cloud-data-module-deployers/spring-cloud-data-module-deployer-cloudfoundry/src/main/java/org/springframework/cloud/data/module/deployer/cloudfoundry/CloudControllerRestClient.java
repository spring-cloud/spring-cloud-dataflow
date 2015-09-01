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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

/**
 * Interface to cloud controller functions which wrap the REST interface required by the {@link CloudFoundryModuleDeployer}.
 *
 * @author Steve Powell
 */
interface CloudControllerRestClient {

	/**
	 * Check that a given route exists.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	ListRoutesResponse listRoutes(ListRoutesRequest request);

	/**
	 * Creates an application definition and returns its id.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	CreateApplicationResponse createApplication(CreateApplicationRequest request);

	/**
	 * Create a new route in the given space and domain.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	CreateRouteResponse createRoute(CreateRouteRequest request);

	/**
	 * Creates a service binding to an application (given its id).
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	CreateServiceBindingResponse createServiceBinding(CreateServiceBindingRequest request);

	/**
	 * Deletes an application given its id. The application may be in any state.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	DeleteApplicationResponse deleteApplication(DeleteApplicationRequest request);

	/**
	 * Deletes a route given its id.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	void deleteRoute(DeleteRouteRequest request);

	/**
	 * Obtains application statistics for every instance of an application given its id.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	GetApplicationStatisticsResponse getApplicationStatistics(GetApplicationStatisticsRequest request);

	/**
	 * Lists applications (with their ids) in a given space(id) and optionally matching a name.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	ListApplicationsResponse listApplications(ListApplicationsRequest request);

	/**
	 * Lists services bindings for a given application.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	ListServiceBindingsResponse listServiceBindings(ListServiceBindingsRequest request);

	/**
	 * Lists all the known organizations (with their ids).
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	ListOrganizationsResponse listOrganizations(ListOrganizationsRequest request);

	/**
	 * Lists all the known service instances (with their ids) in a given space (by id), optionally matching a name.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	ListServiceInstancesResponse listServiceInstances(ListServiceInstancesRequest request);

	/**
	 * Lists all shared domains.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	ListSharedDomainsResponse listSharedDomains(ListSharedDomainsRequest request);

	/**
	 * Lists all the known spaces (with their ids) in a given organization (given by id).
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	ListSpacesResponse listSpaces(ListSpacesRequest request);

	/**
	 * Map an existing route to an application.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	RouteMappingResponse mapRoute(RouteMappingRequest request);

	/**
	 * Remove a service binding from a particular application.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	RemoveServiceBindingResponse removeServiceBinding(RemoveServiceBindingRequest request);

	/**
	 * Uploads the bits required for an application (identified by its id) to run.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	UploadBitsResponse uploadBits(UploadBitsRequest request);

	/**
	 * Updates the state of an application (given by its id), for example, to start it.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	UpdateApplicationResponse updateApplication(UpdateApplicationRequest request);

	/**
	 * Un-map an existing route from an application.
	 * @param request the structure carrying all necessary parameters
	 * @return a Response instance carrying all the response values expected
	 * @throws org.springframework.web.client.RestClientException in the event of failure
	 */
	void unmapRoute(RouteMappingRequest request);

}
