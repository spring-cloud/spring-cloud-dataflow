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
import java.util.Map;
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
	public Results.DeleteApplication deleteApplication(Parameters.DeleteApplication parameters) {
		String appName = parameters.getName();

		String appId = findApplicationId(appName);
		if (appId == null) {
			deleteOldRoutes(appName);
			return new Results.DeleteApplication().withFound(false);
		}

		deleteBoundRoutes(appName, appId);

		unbindServices(appId);

		return new Results.DeleteApplication().withFound(true).withDeleted(deleteBaseApplication(appId));
	}

	@Override
	public Results.GetApplicationsStatus getApplicationsStatus(Parameters.GetApplicationsStatus parameters) {
		Results.GetApplicationsStatus response = new Results.GetApplicationsStatus();

		Requests.ListApplications listRequest = new Requests.ListApplications()
				.withSpaceId(this.spaceId);

		if (parameters.getName() != null) {
			listRequest.withName(parameters.getName());
		}

		List<Responses.ApplicationResource> applications;
		try {
			applications = this.client.listApplications(listRequest).getResources();
			if (applications.isEmpty()) {
				return response;
			}
		}
		catch (RestClientException rce) {
			return response;
		}

		for (Responses.ApplicationResource application : applications) {
			String applicationName = application.getEntity().getName();
			String applicationId = application.getMetadata().getId();

			response.withApplication(applicationName, new ApplicationStatus()
					.withId(applicationId)
					.withInstances(safeGetApplicationStatistics(applicationId))
					.withEnvironment(safeGetApplicationEnvironment(applicationId)));
		}

		return response;
	}

	private Map<String, String> safeGetApplicationEnvironment(String applicationId) {
		try {
			Requests.GetApplicationEnvironment request = new Requests.GetApplicationEnvironment()
					.withId(applicationId);

			return this.client.getApplicationEnvironment(request).getEnvironment();
		}
		catch (RestClientException rce) {
			return null;
		}
	}

	private Responses.GetApplicationStatistics safeGetApplicationStatistics(String applicationId) {
		try {
			Requests.GetApplicationStatistics statsRequest = new Requests.GetApplicationStatistics()
					.withId(applicationId);

			return this.client.getApplicationStatistics(statsRequest);
		}
		catch (RestClientException rce) {
			return new Responses.GetApplicationStatistics();
		}
	}

	@Override
	public Results.PushBindAndStartApplication pushBindAndStartApplication(Parameters.PushBindAndStartApplication parameters) {
		Results.PushBindAndStartApplication pushResults = new Results.PushBindAndStartApplication();

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
			Requests.ListServiceInstances listServiceInstancesRequest = new Requests.ListServiceInstances()
					.withName(serviceInstanceName)
					.withSpaceId(this.spaceId);
			try {
				List<Responses.NamedResource> listServiceInstances = this.client.listServiceInstances(listServiceInstancesRequest).getResources();
				for (Responses.NamedResource serviceInstanceResource : listServiceInstances) {
					Requests.CreateServiceBinding createServiceBindingRequest = new Requests.CreateServiceBinding()
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
		Requests.CreateRoute createRouteRequest = new Requests.CreateRoute()
				.withDomainId(this.domainId)
				.withSpaceId(this.spaceId)
				.withHost(appName.replaceAll("[^A-Za-z0-9]", "-"));
		try {
			Responses.CreateRoute createRouteResponse = this.client.createRoute(createRouteRequest);

			Requests.RouteMapping mapRouteRequest = new Requests.RouteMapping()
					.withRouteId(createRouteResponse.getMetadata().getId())
					.withAppId(appId);
			this.client.mapRoute(mapRouteRequest);
		}
		catch (RestClientException rce) {
			return false;
		}
		return true;
	}

	private String createBaseApplication(Parameters.PushBindAndStartApplication parameters) {
		Requests.CreateApplication createRequest = new Requests.CreateApplication()
				.withSpaceId(this.spaceId)
				.withName(parameters.getName())
				.withInstances(parameters.getInstances())
				.withBuildpack(DEFAULT_BUILDPACK)
				.withMemory(DEFAULT_MEMORY)
				.withState("STOPPED")
				.withEnvironment(parameters.getEnvironment());

		Responses.CreateApplication createResponse;
		try {
			createResponse = this.client.createApplication(createRequest);
		}
		catch (RestClientException rce) {
			return null;
		}
		return createResponse.getMetadata().getId();
	}

	private boolean deleteBaseApplication(String appId) {
		Requests.DeleteApplication deleteRequest = new Requests.DeleteApplication()
				.withId(appId);
		Responses.DeleteApplication response;
		try {
			response = this.client.deleteApplication(deleteRequest);
		}
		catch (RestClientException rce) {
			return false;
		}
		return response.isDeleted();
	}

	private boolean deleteBoundRoutes(String appName, String appId) {
		Requests.ListRoutes listRoutesRequest = new Requests.ListRoutes()
				.withDomainId(this.domainId)
				.withHost(appName.replaceAll("[^A-Za-z0-9]", "-"));
		try {
			Responses.ListRoutes listRoutesResponse = this.client.listRoutes(listRoutesRequest);
			for (Responses.RouteResource resource : listRoutesResponse.getResources()) {
				String routeId = resource.getMetadata().getId();
				if (appId != null) {
					this.client.unmapRoute(new Requests.RouteMapping()
									.withAppId(appId)
									.withRouteId(routeId)
					);
				}
				this.client.deleteRoute(new Requests.DeleteRoute().withId(routeId));
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
		Requests.ListApplications listRequest = new Requests.ListApplications()
				.withSpaceId(this.spaceId)
				.withName(appName);

		List<Responses.ApplicationResource> applications;
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
		Requests.UpdateApplication updateRequest = new Requests.UpdateApplication()
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
		Requests.ListServiceBindings listServiceBindingsRequest = new Requests.ListServiceBindings()
				.withAppId(appId);
		try {
			List<Responses.BindingResource> serviceBindings = this.client.listServiceBindings(listServiceBindingsRequest).getResources();
			for (Responses.BindingResource serviceBinding : serviceBindings) {
				this.client.removeServiceBinding(new Requests.RemoveServiceBinding()
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
		Requests.UploadBits uploadBitsRequest = new Requests.UploadBits()
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
		Requests.ListOrganizations organizationsRequest = new Requests.ListOrganizations()
				.withName(organizationName);

		List<Responses.NamedResource> orgs = client.listOrganizations(organizationsRequest).getResources();
		if (orgs.size() != 1) {
			return null;
		}
		String orgId = orgs.get(0).getMetadata().getId();

		Requests.ListSpaces spacesRequest = new Requests.ListSpaces()
				.withOrgId(orgId)
				.withName(spaceName);

		List<Responses.NamedResource> spaces = client.listSpaces(spacesRequest).getResources();
		if (spaces.size() != 1) {
			return null;
		}
		return spaces.get(0).getMetadata().getId();
	}

	private static String getDomainId(CloudControllerOperations client, String domain) {
		Requests.ListSharedDomains sharedDomainsRequest = new Requests.ListSharedDomains()
				.withName(domain);

		Responses.ListSharedDomains listSharedDomainsResponse = client.listSharedDomains(sharedDomainsRequest);
		List<Responses.NamedResource> domains = listSharedDomainsResponse.getResources();
		if (domains.size() != 1) {
			return null;
		}
		return domains.get(0).getMetadata().getId();
	}
}
