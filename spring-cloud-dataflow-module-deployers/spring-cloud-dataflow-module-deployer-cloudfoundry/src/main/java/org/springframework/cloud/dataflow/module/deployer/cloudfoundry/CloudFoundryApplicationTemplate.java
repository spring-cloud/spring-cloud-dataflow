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

import java.util.List;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClientException;

/**
 * Implementation of high-level operations on applications, limited to those
 * operations required by the {@link ApplicationModuleDeployer}.
 *
 * @author Steve Powell
 * @author Eric Bottard
 * @author Paul Harris
 */
class CloudFoundryApplicationTemplate implements CloudFoundryApplicationOperations {

	private static final String DEFAULT_BUILDPACK = "https://github.com/cloudfoundry/java-buildpack.git#69abec6d2726f73a22339caa6ae7739f060002e4";

	private static final int DEFAULT_MEMORY = 1024; // megabytes

	private final CloudControllerOperations client;

	private final String spaceId;

	private final String domainId;

	CloudFoundryApplicationTemplate(CloudControllerOperations client, String organizationName, String spaceName, String domain) {
		this.client = client;
		this.spaceId = getSpaceId(client, organizationName, spaceName);
		this.domainId = getDomainId(client, domain);
	}

	@Override
	public DeleteApplicationResults deleteApplication(DeleteApplicationParameters parameters) {
		String appName = parameters.getName();

		String appId = findApplicationId(appName);
		if (appId == null) {
			deleteOldRoutes(appName);
			return new DeleteApplicationResults().withFound(false);
		}

		deleteBoundRoutes(appName, appId);

		unbindServices(appId);

		return new DeleteApplicationResults().withFound(true).withDeleted(deleteBaseApplication(appId));
	}

	@Override
	public GetApplicationsStatusResults getApplicationsStatus(GetApplicationsStatusParameters parameters) {
		GetApplicationsStatusResults response = new GetApplicationsStatusResults();

		ListApplicationsRequest listRequest = new ListApplicationsRequest()
				.withSpaceId(this.spaceId);

		if (parameters.getName() != null) {
			listRequest.withName(parameters.getName());
		}

		List<ApplicationResourceResponse> applications;
		try {
			applications = this.client.listApplications(listRequest).getResources();
			if (applications.isEmpty()) {
				return response;
			}
		}
		catch (RestClientException rce) {
			return response;
		}

		for (ApplicationResourceResponse application : applications) {
			String applicationId = application.getMetadata().getId();
			String applicationName = application.getEntity().getName();

			try {
				GetApplicationStatisticsRequest statsRequest = new GetApplicationStatisticsRequest()
						.withId(applicationId);

				GetApplicationStatisticsResponse statsResponse = this.client.getApplicationStatistics(statsRequest);
				response.withApplication(applicationName, new ApplicationStatus()
						.withId(applicationId)
						.withInstances(statsResponse));
			}
			catch (RestClientException rce) {
				response.withApplication(applicationName, new ApplicationStatus());
			}
		}

		return response;
	}

	@Override
	public PushBindAndStartApplicationResults pushBindAndStartApplication(PushBindAndStartApplicationParameters parameters) {
		PushBindAndStartApplicationResults pushResults = new PushBindAndStartApplicationResults();

		String appId = createBaseApplication(parameters);

		String appName = parameters.getName();

		if (appId == null) {
			return pushResults.withCreateSucceeded(false);
		}

		if (!deleteOldRoutes(appName)) {
			deleteBaseApplication(appId);
			return pushResults.withCreateSucceeded(false);
		}

		if (!bindServiceInstances(appId, parameters.getServiceInstanceNames())) {
			deleteBaseApplication(appId);
			return pushResults.withCreateSucceeded(false);
		}

		if (!createAndMapRoute(appName, appId)) {
			unbindServices(appId);
			deleteBaseApplication(appId);
			return pushResults.withCreateSucceeded(false);
		}

		if (!uploadBits(appId, parameters.getResource())) {
			deleteBoundRoutes(appName, appId);
			unbindServices(appId);
			deleteBaseApplication(appId);
			return pushResults.withCreateSucceeded(false);
		}

		if (!startApplication(appId)) {
			deleteBoundRoutes(appName, appId);
			unbindServices(appId);
			deleteBaseApplication(appId);
			return pushResults.withCreateSucceeded(false);
		}

		return pushResults.withCreateSucceeded(true);
	}

	private boolean bindServiceInstances(String appId, Set<String> serviceInstanceNames) {
		for (String serviceInstanceName : serviceInstanceNames) {
			ListServiceInstancesRequest listServiceInstancesRequest = new ListServiceInstancesRequest()
					.withName(serviceInstanceName)
					.withSpaceId(this.spaceId);
			try {
				List<NamedResourceResponse> listServiceInstances = this.client.listServiceInstances(listServiceInstancesRequest).getResources();
				for (NamedResourceResponse serviceInstanceResource : listServiceInstances) {
					CreateServiceBindingRequest createServiceBindingRequest = new CreateServiceBindingRequest()
							.withAppId(appId)
							.withServiceInstanceId(serviceInstanceResource.getMetadata().getId());
					this.client.createServiceBinding(createServiceBindingRequest);
				}
			}
			catch (RestClientException rce) {
				return false;
			}
		}
		return true;
	}

	private boolean createAndMapRoute(String appName, String appId) {
		CreateRouteRequest createRouteRequest = new CreateRouteRequest()
				.withDomainId(this.domainId)
				.withSpaceId(this.spaceId)
				.withHost(appName.replaceAll("[^A-Za-z0-9]", "-"));
		try {
			CreateRouteResponse createRouteResponse = this.client.createRoute(createRouteRequest);

			RouteMappingRequest mapRouteRequest = new RouteMappingRequest()
					.withRouteId(createRouteResponse.getMetadata().getId())
					.withAppId(appId);
			this.client.mapRoute(mapRouteRequest);
		}
		catch (RestClientException rce) {
			return false;
		}
		return true;
	}

	private String createBaseApplication(PushBindAndStartApplicationParameters parameters) {
		CreateApplicationRequest createRequest = new CreateApplicationRequest()
				.withSpaceId(this.spaceId)
				.withName(parameters.getName())
				.withInstances(parameters.getInstances())
				.withBuildpack(DEFAULT_BUILDPACK)
				.withMemory(DEFAULT_MEMORY)
				.withState("STOPPED")
				.withEnvironment(parameters.getEnvironment());

		CreateApplicationResponse createResponse;
		try {
			createResponse = this.client.createApplication(createRequest);
		}
		catch (RestClientException rce) {
			return null;
		}
		return createResponse.getMetadata().getId();
	}

	private boolean deleteBaseApplication(String appId) {
		DeleteApplicationRequest deleteRequest = new DeleteApplicationRequest()
				.withId(appId);
		DeleteApplicationResponse response;
		try {
			response = this.client.deleteApplication(deleteRequest);
		}
		catch (RestClientException rce) {
			return false;
		}
		return response.isDeleted();
	}

	private boolean deleteBoundRoutes(String appName, String appId) {
		ListRoutesRequest listRoutesRequest = new ListRoutesRequest()
				.withDomainId(this.domainId)
				.withHost(appName.replaceAll("[^A-Za-z0-9]", "-"));
		try {
			ListRoutesResponse listRoutesResponse = this.client.listRoutes(listRoutesRequest);
			for (RouteResourceResponse resource : listRoutesResponse.getResources()) {
				String routeId = resource.getMetadata().getId();
				if (appId != null) {
					this.client.unmapRoute(new RouteMappingRequest()
									.withAppId(appId)
									.withRouteId(routeId)
					);
				}
				this.client.deleteRoute(new DeleteRouteRequest().withId(routeId));
			}
		}
		catch (RestClientException rce) {
			return false;
		}
		return true;
	}

	private boolean deleteOldRoutes(String appName) {
		return deleteBoundRoutes(appName, null);
	}

	private String findApplicationId(String appName) {
		ListApplicationsRequest listRequest = new ListApplicationsRequest()
				.withSpaceId(this.spaceId)
				.withName(appName);

		List<ApplicationResourceResponse> applications;
		try {
			applications = this.client.listApplications(listRequest).getResources();
		}
		catch (RestClientException rce) {
			return null;
		}

		if (applications.isEmpty()) {
			return null;
		}
		return applications.get(0).getMetadata().getId();
	}

	private boolean startApplication(String appId) {
		UpdateApplicationRequest updateRequest = new UpdateApplicationRequest()
				.withId(appId)
				.withState("STARTED");
		try {
			this.client.updateApplication(updateRequest);
		}
		catch (RestClientException rce) {
			return false;
		}
		return true;
	}

	private boolean unbindServices(String appId) {
		ListServiceBindingsRequest listServiceBindingsRequest = new ListServiceBindingsRequest()
				.withAppId(appId);
		try {
			List<BindingResourceResponse> serviceBindings = this.client.listServiceBindings(listServiceBindingsRequest).getResources();
			for (BindingResourceResponse serviceBinding : serviceBindings) {
				this.client.removeServiceBinding(new RemoveServiceBindingRequest()
								.withAppId(appId)
								.withBindingId(serviceBinding.getMetadata().getId())
				);
			}
		}
		catch (RestClientException rce) {
			return false;
		}
		return true;
	}

	private boolean uploadBits(String appId, Resource resource) {
		UploadBitsRequest uploadBitsRequest = new UploadBitsRequest()
				.withId(appId)
				.withResource(resource);
		try {
			this.client.uploadBits(uploadBitsRequest);
		}
		catch (RestClientException rce) {
			return false;
		}
		return true;
	}

	private static String getSpaceId(CloudControllerOperations client, String organizationName, String spaceName) {
		ListOrganizationsRequest organizationsRequest = new ListOrganizationsRequest()
				.withName(organizationName);

		List<NamedResourceResponse> orgs = client.listOrganizations(organizationsRequest).getResources();
		if (orgs.size() != 1) {
			return null;
		}
		String orgId = orgs.get(0).getMetadata().getId();

		ListSpacesRequest spacesRequest = new ListSpacesRequest()
				.withOrgId(orgId)
				.withName(spaceName);

		List<NamedResourceResponse> spaces = client.listSpaces(spacesRequest).getResources();
		if (spaces.size() != 1) {
			return null;
		}
		return spaces.get(0).getMetadata().getId();
	}

	private static String getDomainId(CloudControllerOperations client, String domain) {
		ListSharedDomainsRequest sharedDomainsRequest = new ListSharedDomainsRequest()
				.withName(domain);

		ListSharedDomainsResponse listSharedDomainsResponse = client.listSharedDomains(sharedDomainsRequest);
		List<NamedResourceResponse> domains = listSharedDomainsResponse.getResources();
		if (domains.size() != 1) {
			return null;
		}
		return domains.get(0).getMetadata().getId();
	}
}
