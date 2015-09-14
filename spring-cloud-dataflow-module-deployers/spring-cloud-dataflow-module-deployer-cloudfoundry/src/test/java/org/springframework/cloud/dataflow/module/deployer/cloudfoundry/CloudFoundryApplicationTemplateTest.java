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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.cloud.dataflow.module.deployer.cloudfoundry.ResourceResponse.Metadata;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClientException;

/**
 * @author Steve Powell
 * @author Paul Harris
 */
public class CloudFoundryApplicationTemplateTest {

	private CloudControllerOperations client = mock(CloudControllerOperations.class);

	private Resource testResource = new FileSystemResource("test-resource");

	@Test
	public void constructorSucceeds() throws Exception {
		createApplicationOperations();
	}

	@Test
	public void deleteApplication() throws Exception {
		whenListApplications();
		whenListRoutes();
		whenListServiceBindings();
		whenDeleteApplication();

		DeleteApplicationResults results = getDeleteApplicationResults();

		assertTrue(results.isDeleted() && results.isFound());

		verify(this.client).unmapRoute(new RouteMappingRequest().withAppId("test-app-id").withRouteId("test-route-id"));
		verify(this.client).removeServiceBinding(new RemoveServiceBindingRequest().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteRoute(new DeleteRouteRequest().withId("test-route-id"));
	}

	@Test
	public void deleteApplicationFails() throws Exception {
		whenListApplications();
		whenListRoutes();
		whenListServiceBindings();
		when(this.client.deleteApplication(new DeleteApplicationRequest().withId("test-app-id")))
				.thenReturn(new DeleteApplicationResponse().withDeleted(false));

		DeleteApplicationResults results = getDeleteApplicationResults();

		assertTrue(!results.isDeleted() && results.isFound());

		verify(this.client).unmapRoute(new RouteMappingRequest().withAppId("test-app-id").withRouteId("test-route-id"));
		verify(this.client).removeServiceBinding(new RemoveServiceBindingRequest().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteRoute(new DeleteRouteRequest().withId("test-route-id"));
	}

	@Test
	public void deleteApplicationNoApp() throws Exception {
		when(this.client.listApplications(new ListApplicationsRequest().withName("test-app-name").withSpaceId("test-space-id")))
				.thenReturn(new ListApplicationsResponse().withResources(Collections.<ApplicationResourceResponse>emptyList()));
		whenListRoutes();

		DeleteApplicationResults results = getDeleteApplicationResults();

		assertTrue(!results.isDeleted() && !results.isFound());
		verify(this.client, never()).unmapRoute(any(RouteMappingRequest.class));
		verify(this.client).deleteRoute(new DeleteRouteRequest().withId("test-route-id"));
	}

	@Test
	public void getApplicationsStatusMany() throws Exception {
		when(this.client.listApplications(new ListApplicationsRequest().withSpaceId("test-space-id")))
				.thenReturn(testListApplicationsResponseTwo("test-app-name1", "test-app-id1", "test-app-name2", "test-app-id2"));

		ApplicationInstanceStatus testApplicationInstanceStatus1 = new ApplicationInstanceStatus();
		ApplicationInstanceStatus testApplicationInstanceStatus2 = new ApplicationInstanceStatus();

		when(this.client.getApplicationStatistics(new GetApplicationStatisticsRequest().withId("test-app-id1")))
				.thenReturn(testGetApplicationStatisticsResponse("test-app-instance-id1", testApplicationInstanceStatus1));
		when(this.client.getApplicationStatistics(new GetApplicationStatisticsRequest().withId("test-app-id2")))
				.thenReturn(testGetApplicationStatisticsResponse("test-app-instance-id2", testApplicationInstanceStatus2));

		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		GetApplicationsStatusResults results = applicationOperations.getApplicationsStatus(new GetApplicationsStatusParameters());

		assertEquals(results.getApplications().size(), 2);
		assertTrue(results.getApplications().containsKey("test-app-name1"));

		Map<String, ApplicationInstanceStatus> instances = results.getApplications().get("test-app-name2").getInstances();

		assertEquals(instances.size(), 1);
		assertEquals(instances.get("test-app-instance-id2"), testApplicationInstanceStatus2);
	}

	@Test
	public void getApplicationsStatusNone() throws Exception {
		when(this.client.listApplications(new ListApplicationsRequest().withSpaceId("test-space-id")))
				.thenReturn(new ListApplicationsResponse().withResources(Collections.<ApplicationResourceResponse>emptyList()));

		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		GetApplicationsStatusResults results = applicationOperations.getApplicationsStatus(new GetApplicationsStatusParameters());

		assertEquals(results.getApplications().size(), 0);
	}

	@Test
	public void getApplicationStatusNone() throws Exception {
		when(this.client.listApplications(new ListApplicationsRequest().withName("test-app-name").withSpaceId("test-space-id")))
				.thenReturn(new ListApplicationsResponse().withResources(Collections.<ApplicationResourceResponse>emptyList()));

		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		GetApplicationsStatusResults results = applicationOperations.getApplicationsStatus(new GetApplicationsStatusParameters().withName("test-app-name"));

		assertEquals(results.getApplications().size(), 0);
	}

	@Test
	public void getApplicationStatusOne() throws Exception {
		whenListApplications();

		ApplicationInstanceStatus testApplicationInstanceStatus = new ApplicationInstanceStatus();
		when(this.client.getApplicationStatistics(new GetApplicationStatisticsRequest().withId("test-app-id")))
				.thenReturn(testGetApplicationStatisticsResponse("test-app-instance-id", testApplicationInstanceStatus));

		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		GetApplicationsStatusResults results = applicationOperations.getApplicationsStatus(new GetApplicationsStatusParameters().withName("test-app-name"));

		assertEquals(results.getApplications().size(), 1);
		assertTrue(results.getApplications().containsKey("test-app-name"));

		Map<String, ApplicationInstanceStatus> instances = results.getApplications().get("test-app-name").getInstances();

		assertEquals(instances.size(), 1);
		assertEquals(instances.get("test-app-instance-id"), testApplicationInstanceStatus);
	}

	@Test
	public void pushBindAndStartApplication() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		whenListServiceInstances();
		whenCreateRoute();
		whenListServiceBindings();

		PushBindAndStartApplicationResults results = getPushBindAndStartApplicationResults();

		assertTrue(results.isCreateSucceeded());

		verify(this.client).createServiceBinding(new CreateServiceBindingRequest().withAppId("test-app-id").withServiceInstanceId("test-service-instance-id"));
		verify(this.client).mapRoute(new RouteMappingRequest().withAppId("test-app-id").withRouteId("test-route-id"));
		verify(this.client).uploadBits(new UploadBitsRequest().withId("test-app-id").withResource(this.testResource));
		verify(this.client).updateApplication(new UpdateApplicationRequest().withId("test-app-id").withState("STARTED"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnAppId() throws Exception {
		when(this.client.createApplication(new CreateApplicationRequest()
				.withName("test-app-name")
				.withState("STOPPED")
				.withEnvironment(null)
				.withInstances(1)
				.withSpaceId("test-space-id")))
				.thenThrow(new RestClientException("test-exception"));

		PushBindAndStartApplicationResults results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).uploadBits(any(UploadBitsRequest.class));
		verify(this.client, never()).updateApplication(any(UpdateApplicationRequest.class));
		verify(this.client, never()).mapRoute(any(RouteMappingRequest.class));
		verify(this.client, never()).deleteApplication(any(DeleteApplicationRequest.class));
	}

	@Test
	public void pushBindAndStartApplicationFailOnBindService() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		when(this.client.listServiceInstances(new ListServiceInstancesRequest().withSpaceId("test-space-id").withName("test-service-name")))
				.thenThrow(new RestClientException("test-exception"));
		whenDeleteApplication();

		PushBindAndStartApplicationResults results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).uploadBits(any(UploadBitsRequest.class));
		verify(this.client, never()).updateApplication(any(UpdateApplicationRequest.class));
		verify(this.client, never()).mapRoute(any(RouteMappingRequest.class));
		verify(this.client, never()).removeServiceBinding(any(RemoveServiceBindingRequest.class));

		verify(this.client).deleteApplication(new DeleteApplicationRequest().withId("test-app-id"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnOldRoutes() throws Exception {
		whenCreateApplication();
		whenListRoutes();
		when(this.client.deleteRoute(any(DeleteRouteRequest.class)))
				.thenThrow(new RestClientException("test-exception"));
		whenDeleteApplication();

		PushBindAndStartApplicationResults results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).uploadBits(any(UploadBitsRequest.class));
		verify(this.client, never()).updateApplication(any(UpdateApplicationRequest.class));
		verify(this.client, never()).mapRoute(any(RouteMappingRequest.class));

		verify(this.client).deleteApplication(new DeleteApplicationRequest().withId("test-app-id"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnRouteCreate() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		whenListServiceInstances();
		when(this.client.createRoute(new CreateRouteRequest().withSpaceId("test-space-id").withDomainId("test-domain-id").withHost("test-app-name")))
				.thenThrow(new RestClientException("test-exception"));
		whenListServiceBindings();
		whenDeleteApplication();

		PushBindAndStartApplicationResults results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).uploadBits(any(UploadBitsRequest.class));
		verify(this.client, never()).updateApplication(any(UpdateApplicationRequest.class));

		verify(this.client).removeServiceBinding(new RemoveServiceBindingRequest().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteApplication(new DeleteApplicationRequest().withId("test-app-id"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnStart() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		whenListServiceInstances();
		whenCreateRoute();
		when(this.client.updateApplication(new UpdateApplicationRequest().withId("test-app-id").withState("STARTED")))
				.thenThrow(new RestClientException("test-exception"));
		whenListServiceBindings();
		whenDeleteApplication();

		PushBindAndStartApplicationResults results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client).deleteRoute(new DeleteRouteRequest().withId("test-route-id"));
		verify(this.client).removeServiceBinding(new RemoveServiceBindingRequest().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteApplication(new DeleteApplicationRequest().withId("test-app-id"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnUpload() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		whenListServiceInstances();
		whenCreateRoute();
		when(this.client.uploadBits(new UploadBitsRequest().withId("test-app-id").withResource(this.testResource)))
				.thenThrow(new RestClientException("test-exception"));
		whenListServiceBindings();
		whenDeleteApplication();

		PushBindAndStartApplicationResults results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).updateApplication(any(UpdateApplicationRequest.class));

		verify(this.client).deleteRoute(new DeleteRouteRequest().withId("test-route-id"));
		verify(this.client).removeServiceBinding(new RemoveServiceBindingRequest().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteApplication(new DeleteApplicationRequest().withId("test-app-id"));
	}

	private CloudFoundryApplicationOperations createApplicationOperations() throws Exception {
		when(this.client.listOrganizations(new ListOrganizationsRequest().withName("test-org-name")))
				.thenReturn(testListOrganisationsResponse("test-org-id"));
		when(this.client.listSpaces(new ListSpacesRequest().withName("test-space-name").withOrgId("test-org-id")))
				.thenReturn(testListSpacesResponse("test-space-id"));
		when(this.client.listSharedDomains(new ListSharedDomainsRequest().withName("test-domain-name")))
				.thenReturn(testListSharedDomainsResponse("test-domain-id"));

		return new CloudFoundryApplicationTemplate(this.client, "test-org-name", "test-space-name", "test-domain-name");
	}

	private DeleteApplicationResults getDeleteApplicationResults() throws Exception {
		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		return applicationOperations.deleteApplication(new DeleteApplicationParameters().withName("test-app-name"));
	}

	private PushBindAndStartApplicationResults getPushBindAndStartApplicationResults() throws Exception {
		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		return applicationOperations.pushBindAndStartApplication(new PushBindAndStartApplicationParameters()
				.withName("test-app-name")
				.withInstances(1)
				.withServiceInstanceNames(Collections.singleton("test-service-name"))
				.withResource(this.testResource));
	}

	private void whenCreateApplication() {
		when(this.client.createApplication(new CreateApplicationRequest()
				.withName("test-app-name")
				.withState("STOPPED")
				.withEnvironment(null)
				.withInstances(1)
				.withSpaceId("test-space-id")))
				.thenReturn(new CreateApplicationResponse()
						.withMetadata(new Metadata().withId("test-app-id")));
	}

	private void whenCreateRoute() {
		when(this.client.createRoute(new CreateRouteRequest().withSpaceId("test-space-id").withDomainId("test-domain-id").withHost("test-app-name")))
				.thenReturn(testCreateRouteResponse("test-route-id"));
	}

	private void whenDeleteApplication() {
		when(this.client.deleteApplication(new DeleteApplicationRequest().withId("test-app-id")))
				.thenReturn(new DeleteApplicationResponse().withDeleted(true));
	}

	private void whenListApplications() {
		when(this.client.listApplications(new ListApplicationsRequest().withName("test-app-name").withSpaceId("test-space-id")))
				.thenReturn(testListApplicationsResponse("test-app-name", "test-app-id"));
	}

	private void whenListRoutes() {
		when(this.client.listRoutes(new ListRoutesRequest().withDomainId("test-domain-id").withHost("test-app-name")))
				.thenReturn(testListRoutesResponse("test-route-id"));
	}

	private void whenListServiceBindings() {
		when(this.client.listServiceBindings(new ListServiceBindingsRequest().withAppId("test-app-id")))
				.thenReturn(testListServiceBindingsResponse("test-binding-id"));
	}

	private void whenListServiceInstances() {
		when(this.client.listServiceInstances(new ListServiceInstancesRequest().withSpaceId("test-space-id").withName("test-service-name")))
				.thenReturn(testListServiceInstancesResponse("test-service-instance-id"));
	}

	private void whenMultipleListRoutes() {
		when(this.client.listRoutes(new ListRoutesRequest().withDomainId("test-domain-id").withHost("test-app-name")))
				.thenAnswer(new Answer() {
					private int count = 0;

					public Object answer(InvocationOnMock invocation) {
						count++;
						if (count == 1) {
							return new ListRoutesResponse().withResources(Collections.<RouteResourceResponse>emptyList());
						}
						else {
							return testListRoutesResponse("test-route-id");
						}
					}
				});
	}

	private static CreateRouteResponse testCreateRouteResponse(String routeId) {
		return new CreateRouteResponse().withMetadata(new Metadata().withId(routeId));
	}

	private static GetApplicationStatisticsResponse testGetApplicationStatisticsResponse(String instanceId, ApplicationInstanceStatus applicationInstanceStatus) {
		GetApplicationStatisticsResponse response = new GetApplicationStatisticsResponse();
		response.put(instanceId, applicationInstanceStatus);
		return response;
	}

	private static ListApplicationsResponse testListApplicationsResponse(String appName, String appId) {
		return new ListApplicationsResponse()
				.withResources(Collections.singletonList(new ApplicationResourceResponse()
						.withMetadata(new Metadata().withId(appId))
						.withEntity(new ApplicationEntity().withName(appName))));
	}

	private static ListApplicationsResponse testListApplicationsResponseTwo(String appName1, String appId1, String appName2, String appId2) {
		ListApplicationsResponse response = new ListApplicationsResponse();
		List<ApplicationResourceResponse> resourceResponseList = new ArrayList<>();
		resourceResponseList.add(new ApplicationResourceResponse()
				.withMetadata(new Metadata().withId(appId1))
				.withEntity(new ApplicationEntity().withName(appName1)));
		resourceResponseList.add(new ApplicationResourceResponse()
				.withMetadata(new Metadata().withId(appId2))
				.withEntity(new ApplicationEntity().withName(appName2)));
		return response.withResources(resourceResponseList);
	}

	private static ListOrganizationsResponse testListOrganisationsResponse(String orgId) {
		return new ListOrganizationsResponse()
				.withResources(Collections.singletonList(new NamedResourceResponse()
						.withMetadata(new Metadata().withId(orgId))));
	}

	private static ListRoutesResponse testListRoutesResponse(String routeId) {
		return new ListRoutesResponse()
				.withResources(Collections.singletonList(new RouteResourceResponse()
						.withMetadata(new Metadata().withId(routeId))));
	}

	private static ListServiceBindingsResponse testListServiceBindingsResponse(String bindingId) {
		return new ListServiceBindingsResponse()
				.withResources(Collections.singletonList(new BindingResourceResponse()
						.withMetadata(new Metadata().withId(bindingId))));
	}

	private static ListServiceInstancesResponse testListServiceInstancesResponse(String instanceId) {
		return new ListServiceInstancesResponse()
				.withResources(Collections.singletonList(new NamedResourceResponse()
						.withMetadata(new Metadata().withId(instanceId))));
	}

	private static ListSharedDomainsResponse testListSharedDomainsResponse(String domainId) {
		return new ListSharedDomainsResponse()
				.withResources(Collections.singletonList(new NamedResourceResponse()
						.withMetadata(new Metadata().withId(domainId))));
	}

	private static ListSpacesResponse testListSpacesResponse(String spaceId) {
		return new ListSpacesResponse()
				.withResources(Collections.singletonList(new NamedResourceResponse()
						.withMetadata(new Metadata().withId(spaceId))));
	}
}
