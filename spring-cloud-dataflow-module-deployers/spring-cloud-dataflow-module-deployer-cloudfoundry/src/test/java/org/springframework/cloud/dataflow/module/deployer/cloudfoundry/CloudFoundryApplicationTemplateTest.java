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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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

		Results.DeleteApplication results = getDeleteApplicationResults();

		assertTrue(results.isDeleted() && results.isFound());

		verify(this.client).unmapRoute(new Requests.RouteMapping().withAppId("test-app-id").withRouteId("test-route-id"));
		verify(this.client).removeServiceBinding(new Requests.RemoveServiceBinding().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteRoute(new Requests.DeleteRoute().withId("test-route-id"));
	}

	@Test
	public void deleteApplicationFails() throws Exception {
		whenListApplications();
		whenListRoutes();
		whenListServiceBindings();
		when(this.client.deleteApplication(new Requests.DeleteApplication().withId("test-app-id")))
				.thenReturn(new Responses.DeleteApplication().withDeleted(false));

		Results.DeleteApplication results = getDeleteApplicationResults();

		assertTrue(!results.isDeleted() && results.isFound());

		verify(this.client).unmapRoute(new Requests.RouteMapping().withAppId("test-app-id").withRouteId("test-route-id"));
		verify(this.client).removeServiceBinding(new Requests.RemoveServiceBinding().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteRoute(new Requests.DeleteRoute().withId("test-route-id"));
	}

	@Test
	public void deleteApplicationNoApp() throws Exception {
		when(this.client.listApplications(new Requests.ListApplications().withName("test-app-name").withSpaceId("test-space-id")))
				.thenReturn(new Responses.ListApplications().withResources(Collections.<Responses.ApplicationResource>emptyList()));
		whenListRoutes();

		Results.DeleteApplication results = getDeleteApplicationResults();

		assertTrue(!results.isDeleted() && !results.isFound());
		verify(this.client, never()).unmapRoute(any(Requests.RouteMapping.class));
		verify(this.client).deleteRoute(new Requests.DeleteRoute().withId("test-route-id"));
	}

	@Test
	public void getApplicationsStatusMany() throws Exception {
		when(this.client.listApplications(new Requests.ListApplications().withSpaceId("test-space-id")))
				.thenReturn(testListApplicationsResponseTwo("test-app-name1", "test-app-id1", "test-app-name2", "test-app-id2"));

		Responses.ApplicationInstanceStatus testApplicationInstanceStatus1 = new Responses.ApplicationInstanceStatus();
		Responses.ApplicationInstanceStatus testApplicationInstanceStatus2 = new Responses.ApplicationInstanceStatus();

		when(this.client.getApplicationStatistics(new Requests.GetApplicationStatistics().withId("test-app-id1")))
				.thenReturn(testGetApplicationStatisticsResponse("test-app-instance-id1", testApplicationInstanceStatus1));
		when(this.client.getApplicationStatistics(new Requests.GetApplicationStatistics().withId("test-app-id2")))
				.thenReturn(testGetApplicationStatisticsResponse("test-app-instance-id2", testApplicationInstanceStatus2));
		when(this.client.getApplicationEnvironment(new Requests.GetApplicationEnvironment().withId("test-app-id1")))
				.thenReturn(new Responses.GetApplicationEnvironment());
		when(this.client.getApplicationEnvironment(new Requests.GetApplicationEnvironment().withId("test-app-id2")))
				.thenReturn(new Responses.GetApplicationEnvironment());

		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		Results.GetApplicationsStatus results = applicationOperations.getApplicationsStatus(new Parameters.GetApplicationsStatus());

		assertEquals(results.getApplications().size(), 2);
		assertTrue(results.getApplications().containsKey("test-app-name1"));

		Map<String, Responses.ApplicationInstanceStatus> instances = results.getApplications().get("test-app-name2").getInstances();

		assertEquals(instances.size(), 1);
		assertEquals(instances.get("test-app-instance-id2"), testApplicationInstanceStatus2);
	}

	@Test
	public void getApplicationsStatusNone() throws Exception {
		when(this.client.listApplications(new Requests.ListApplications().withSpaceId("test-space-id")))
				.thenReturn(new Responses.ListApplications().withResources(Collections.<Responses.ApplicationResource>emptyList()));

		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		Results.GetApplicationsStatus results = applicationOperations.getApplicationsStatus(new Parameters.GetApplicationsStatus());

		assertEquals(results.getApplications().size(), 0);
	}

	@Test
	public void getApplicationStatusNone() throws Exception {
		when(this.client.listApplications(new Requests.ListApplications().withName("test-app-name").withSpaceId("test-space-id")))
				.thenReturn(new Responses.ListApplications().withResources(Collections.<Responses.ApplicationResource>emptyList()));

		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		Results.GetApplicationsStatus results = applicationOperations.getApplicationsStatus(new Parameters.GetApplicationsStatus().withName("test-app-name"));

		assertEquals(results.getApplications().size(), 0);
	}

	@Test
	public void getApplicationStatusOne() throws Exception {
		whenListApplications();

		Responses.ApplicationInstanceStatus testApplicationInstanceStatus = new Responses.ApplicationInstanceStatus();
		when(this.client.getApplicationStatistics(new Requests.GetApplicationStatistics().withId("test-app-id")))
				.thenReturn(testGetApplicationStatisticsResponse("test-app-instance-id", testApplicationInstanceStatus));
		when(this.client.getApplicationEnvironment(new Requests.GetApplicationEnvironment().withId("test-app-id")))
				.thenReturn(new Responses.GetApplicationEnvironment());

		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		Results.GetApplicationsStatus results = applicationOperations.getApplicationsStatus(new Parameters.GetApplicationsStatus().withName("test-app-name"));

		assertEquals(results.getApplications().size(), 1);
		assertTrue(results.getApplications().containsKey("test-app-name"));

		Map<String, Responses.ApplicationInstanceStatus> instances = results.getApplications().get("test-app-name").getInstances();

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

		Results.PushBindAndStartApplication results = getPushBindAndStartApplicationResults();

		assertTrue(results.isCreateSucceeded());

		verify(this.client).createServiceBinding(new Requests.CreateServiceBinding().withAppId("test-app-id").withServiceInstanceId("test-service-instance-id"));
		verify(this.client).mapRoute(new Requests.RouteMapping().withAppId("test-app-id").withRouteId("test-route-id"));
		verify(this.client).uploadBits(new Requests.UploadBits().withId("test-app-id").withResource(this.testResource));
		verify(this.client).updateApplication(new Requests.UpdateApplication().withId("test-app-id").withState("STARTED"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnAppId() throws Exception {
		when(this.client.createApplication(new Requests.CreateApplication()
				.withName("test-app-name")
				.withState("STOPPED")
				.withEnvironment(null)
				.withInstances(1)
				.withSpaceId("test-space-id")))
				.thenThrow(new RestClientException("test-exception"));

		Results.PushBindAndStartApplication results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).uploadBits(any(Requests.UploadBits.class));
		verify(this.client, never()).updateApplication(any(Requests.UpdateApplication.class));
		verify(this.client, never()).mapRoute(any(Requests.RouteMapping.class));
		verify(this.client, never()).deleteApplication(any(Requests.DeleteApplication.class));
	}

	@Test
	public void pushBindAndStartApplicationFailOnBindService() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		when(this.client.listServiceInstances(new Requests.ListServiceInstances().withSpaceId("test-space-id").withName("test-service-name")))
				.thenThrow(new RestClientException("test-exception"));
		whenDeleteApplication();

		Results.PushBindAndStartApplication results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).uploadBits(any(Requests.UploadBits.class));
		verify(this.client, never()).updateApplication(any(Requests.UpdateApplication.class));
		verify(this.client, never()).mapRoute(any(Requests.RouteMapping.class));
		verify(this.client, never()).removeServiceBinding(any(Requests.RemoveServiceBinding.class));

		verify(this.client).deleteApplication(new Requests.DeleteApplication().withId("test-app-id"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnOldRoutes() throws Exception {
		whenCreateApplication();
		whenListRoutes();
		when(this.client.deleteRoute(any(Requests.DeleteRoute.class)))
				.thenThrow(new RestClientException("test-exception"));
		whenDeleteApplication();

		Results.PushBindAndStartApplication results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).uploadBits(any(Requests.UploadBits.class));
		verify(this.client, never()).updateApplication(any(Requests.UpdateApplication.class));
		verify(this.client, never()).mapRoute(any(Requests.RouteMapping.class));

		verify(this.client).deleteApplication(new Requests.DeleteApplication().withId("test-app-id"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnRouteCreate() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		whenListServiceInstances();
		when(this.client.createRoute(new Requests.CreateRoute().withSpaceId("test-space-id").withDomainId("test-domain-id").withHost("test-app-name")))
				.thenThrow(new RestClientException("test-exception"));
		whenListServiceBindings();
		whenDeleteApplication();

		Results.PushBindAndStartApplication results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).uploadBits(any(Requests.UploadBits.class));
		verify(this.client, never()).updateApplication(any(Requests.UpdateApplication.class));

		verify(this.client).removeServiceBinding(new Requests.RemoveServiceBinding().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteApplication(new Requests.DeleteApplication().withId("test-app-id"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnStart() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		whenListServiceInstances();
		whenCreateRoute();
		when(this.client.updateApplication(new Requests.UpdateApplication().withId("test-app-id").withState("STARTED")))
				.thenThrow(new RestClientException("test-exception"));
		whenListServiceBindings();
		whenDeleteApplication();

		Results.PushBindAndStartApplication results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client).deleteRoute(new Requests.DeleteRoute().withId("test-route-id"));
		verify(this.client).removeServiceBinding(new Requests.RemoveServiceBinding().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteApplication(new Requests.DeleteApplication().withId("test-app-id"));
	}

	@Test
	public void pushBindAndStartApplicationFailOnUpload() throws Exception {
		whenCreateApplication();
		whenMultipleListRoutes();
		whenListServiceInstances();
		whenCreateRoute();
		when(this.client.uploadBits(new Requests.UploadBits().withId("test-app-id").withResource(this.testResource)))
				.thenThrow(new RestClientException("test-exception"));
		whenListServiceBindings();
		whenDeleteApplication();

		Results.PushBindAndStartApplication results = getPushBindAndStartApplicationResults();

		assertFalse(results.isCreateSucceeded());

		verify(this.client, never()).updateApplication(any(Requests.UpdateApplication.class));

		verify(this.client).deleteRoute(new Requests.DeleteRoute().withId("test-route-id"));
		verify(this.client).removeServiceBinding(new Requests.RemoveServiceBinding().withAppId("test-app-id").withBindingId("test-binding-id"));
		verify(this.client).deleteApplication(new Requests.DeleteApplication().withId("test-app-id"));
	}

	private CloudFoundryApplicationOperations createApplicationOperations() throws Exception {
		when(this.client.listOrganizations(new Requests.ListOrganizations().withName("test-org-name")))
				.thenReturn(testListOrganisationsResponse("test-org-id"));
		when(this.client.listSpaces(new Requests.ListSpaces().withName("test-space-name").withOrgId("test-org-id")))
				.thenReturn(testListSpacesResponse("test-space-id"));
		when(this.client.listSharedDomains(new Requests.ListSharedDomains().withName("test-domain-name")))
				.thenReturn(testListSharedDomainsResponse("test-domain-id"));

		return new CloudFoundryApplicationTemplate(this.client, "test-org-name", "test-space-name", "test-domain-name");
	}

	private Results.DeleteApplication getDeleteApplicationResults() throws Exception {
		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		return applicationOperations.deleteApplication(new Parameters.DeleteApplication().withName("test-app-name"));
	}

	private Results.PushBindAndStartApplication getPushBindAndStartApplicationResults() throws Exception {
		CloudFoundryApplicationOperations applicationOperations = createApplicationOperations();
		return applicationOperations.pushBindAndStartApplication(new Parameters.PushBindAndStartApplication()
				.withName("test-app-name")
				.withInstances(1)
				.withServiceInstanceNames(Collections.singleton("test-service-name"))
				.withResource(this.testResource));
	}

	private void whenCreateApplication() {
		when(this.client.createApplication(new Requests.CreateApplication()
				.withName("test-app-name")
				.withState("STOPPED")
				.withEnvironment(null)
				.withInstances(1)
				.withSpaceId("test-space-id")))
				.thenReturn(new Responses.CreateApplication()
						.withMetadata(new Responses.Metadata().withId("test-app-id")));
	}

	private void whenCreateRoute() {
		when(this.client.createRoute(new Requests.CreateRoute().withSpaceId("test-space-id").withDomainId("test-domain-id").withHost("test-app-name")))
				.thenReturn(testCreateRouteResponse("test-route-id"));
	}

	private void whenDeleteApplication() {
		when(this.client.deleteApplication(new Requests.DeleteApplication().withId("test-app-id")))
				.thenReturn(new Responses.DeleteApplication().withDeleted(true));
	}

	private void whenListApplications() {
		when(this.client.listApplications(new Requests.ListApplications().withName("test-app-name").withSpaceId("test-space-id")))
				.thenReturn(testListApplicationsResponse("test-app-name", "test-app-id"));
	}

	private void whenListRoutes() {
		when(this.client.listRoutes(new Requests.ListRoutes().withDomainId("test-domain-id").withHost("test-app-name")))
				.thenReturn(testListRoutesResponse("test-route-id"));
	}

	private void whenListServiceBindings() {
		when(this.client.listServiceBindings(new Requests.ListServiceBindings().withAppId("test-app-id")))
				.thenReturn(testListServiceBindingsResponse("test-binding-id"));
	}

	private void whenListServiceInstances() {
		when(this.client.listServiceInstances(new Requests.ListServiceInstances().withSpaceId("test-space-id").withName("test-service-name")))
				.thenReturn(testListServiceInstancesResponse("test-service-instance-id"));
	}

	private void whenMultipleListRoutes() {
		when(this.client.listRoutes(new Requests.ListRoutes().withDomainId("test-domain-id").withHost("test-app-name")))
				.thenAnswer(new Answer() {
					private int count = 0;

					public Object answer(InvocationOnMock invocation) {
						count++;
						if (count == 1) {
							return new Responses.ListRoutes().withResources(Collections.<Responses.RouteResource>emptyList());
						}
						else {
							return testListRoutesResponse("test-route-id");
						}
					}
				});
	}

	private static Responses.CreateRoute testCreateRouteResponse(String routeId) {
		return new Responses.CreateRoute().withMetadata(new Responses.Metadata().withId(routeId));
	}

	private static Responses.GetApplicationStatistics testGetApplicationStatisticsResponse(String instanceId, Responses.ApplicationInstanceStatus applicationInstanceStatus) {
		Responses.GetApplicationStatistics response = new Responses.GetApplicationStatistics();
		response.put(instanceId, applicationInstanceStatus);
		return response;
	}

	private static Responses.ListApplications testListApplicationsResponse(String appName, String appId) {
		return new Responses.ListApplications()
				.withResources(Collections.singletonList(new Responses.ApplicationResource()
						.withMetadata(new Responses.Metadata().withId(appId))
						.withEntity(new Entities.ApplicationEntity().withName(appName))));
	}

	private static Responses.ListApplications testListApplicationsResponseTwo(String appName1, String appId1, String appName2, String appId2) {
		Responses.ListApplications response = new Responses.ListApplications();
		List<Responses.ApplicationResource> resourceResponseList = new ArrayList<>();
		resourceResponseList.add(new Responses.ApplicationResource()
				.withMetadata(new Responses.Metadata().withId(appId1))
				.withEntity(new Entities.ApplicationEntity().withName(appName1)));
		resourceResponseList.add(new Responses.ApplicationResource()
				.withMetadata(new Responses.Metadata().withId(appId2))
				.withEntity(new Entities.ApplicationEntity().withName(appName2)));
		return response.withResources(resourceResponseList);
	}

	private static Responses.ListOrganizations testListOrganisationsResponse(String orgId) {
		return new Responses.ListOrganizations()
				.withResources(Collections.singletonList(new Responses.NamedResource()
						.withMetadata(new Responses.Metadata().withId(orgId))));
	}

	private static Responses.ListRoutes testListRoutesResponse(String routeId) {
		return new Responses.ListRoutes()
				.withResources(Collections.singletonList(new Responses.RouteResource()
						.withMetadata(new Responses.Metadata().withId(routeId))));
	}

	private static Responses.ListServiceBindings testListServiceBindingsResponse(String bindingId) {
		return new Responses.ListServiceBindings()
				.withResources(Collections.singletonList(new Responses.BindingResource()
						.withMetadata(new Responses.Metadata().withId(bindingId))));
	}

	private static Responses.ListServiceInstances testListServiceInstancesResponse(String instanceId) {
		return new Responses.ListServiceInstances()
				.withResources(Collections.singletonList(new Responses.NamedResource()
						.withMetadata(new Responses.Metadata().withId(instanceId))));
	}

	private static Responses.ListSharedDomains testListSharedDomainsResponse(String domainId) {
		return new Responses.ListSharedDomains()
				.withResources(Collections.singletonList(new Responses.NamedResource()
						.withMetadata(new Responses.Metadata().withId(domainId))));
	}

	private static Responses.ListSpaces testListSpacesResponse(String spaceId) {
		return new Responses.ListSpaces()
				.withResources(Collections.singletonList(new Responses.NamedResource()
						.withMetadata(new Responses.Metadata().withId(spaceId))));
	}
}
