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

import java.net.URI;
import java.util.ArrayList;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST client specialised for module deployment requirements.
 *
 * @author Steve Powell
 */
class StandardCloudControllerRestClient implements CloudControllerRestClient {

	private final URI endpoint;

	private final ExtendedOAuth2RestOperations restOperations;

	StandardCloudControllerRestClient(URI endpoint, ExtendedOAuth2RestOperations restOperations) {
		this.endpoint = endpoint;
		this.restOperations = restOperations;
	}

	@Override
	public ListRoutesResponse listRoutes(ListRoutesRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "routes")
				.queryParam("q", "host:" + request.getHost())
				.queryParam("q", "domain_guid:" + request.getDomainId())
				.build().toUri();

		return this.restOperations.getForObject(uri, ListRoutesResponse.class);
	}

	@Override
	public CreateApplicationResponse createApplication(CreateApplicationRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "apps")
				.build().toUri();

		return this.restOperations.postForObject(uri, request, CreateApplicationResponse.class);
	}

	@Override
	public CreateRouteResponse createRoute(CreateRouteRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "routes")
				.build().toUri();

		return this.restOperations.postForObject(uri, request, CreateRouteResponse.class);
	}

	@Override
	public CreateServiceBindingResponse createServiceBinding(CreateServiceBindingRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "service_bindings")
				.build().toUri();

		return this.restOperations.postForObject(uri, request, CreateServiceBindingResponse.class);
	}

	@Override
	public DeleteApplicationResponse deleteApplication(DeleteApplicationRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "apps", request.getId())
				.build().toUri();

		this.restOperations.delete(uri);

		return new DeleteApplicationResponse().withDeleted(true);
	}

	@Override
	public void deleteRoute(DeleteRouteRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "routes", request.getId())
				.build().toUri();

		this.restOperations.delete(uri);

	}

	@Override
	public GetApplicationStatisticsResponse getApplicationStatistics(GetApplicationStatisticsRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "apps", request.getId(), "stats")
				.build().toUri();

		return this.restOperations.getForObject(uri, GetApplicationStatisticsResponse.class);
	}

	@Override
	public ListApplicationsResponse listApplications(ListApplicationsRequest request) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "apps")
				.queryParam("q", "space_guid:" + request.getSpaceId());
		if (!StringUtils.isEmpty(request.getName())) {
			builder.queryParam("q", "name:" + request.getName());
		}
		URI uri = builder.build().toUri();

		return this.restOperations.getForObject(uri, ListApplicationsResponse.class);
	}

	@Override
	public ListOrganizationsResponse listOrganizations(ListOrganizationsRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "organizations")
				.queryParam("q", "name:" + request.getName())
				.build().toUri();

		return this.restOperations.getForObject(uri, ListOrganizationsResponse.class);
	}

	@Override
	public ListServiceBindingsResponse listServiceBindings(ListServiceBindingsRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "apps", request.getAppId(), "service_bindings")
				.build().toUri();

		return this.restOperations.getForObject(uri, ListServiceBindingsResponse.class);
	}

	@Override
	public ListServiceInstancesResponse listServiceInstances(ListServiceInstancesRequest request) {
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "service_instances")
				.queryParam("q", "space_guid:" + request.getSpaceId());
		if (!StringUtils.isEmpty(request.getName())) {
			uriComponentsBuilder.queryParam("q", "name:" + request.getName());
		}
		URI uri = uriComponentsBuilder.build().toUri();

		return this.restOperations.getForObject(uri, ListServiceInstancesResponse.class);
	}

	@Override
	public ListSharedDomainsResponse listSharedDomains(ListSharedDomainsRequest request) {
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "shared_domains");
		if (!StringUtils.isEmpty(request.getName())) {
			uriComponentsBuilder.queryParam("q", "name:" + request.getName());
		}
		URI uri = uriComponentsBuilder.build().toUri();

		return this.restOperations.getForObject(uri, ListSharedDomainsResponse.class);
	}

	@Override
	public ListSpacesResponse listSpaces(ListSpacesRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "spaces")
				.queryParam("q", "name:" + request.getName())
				.queryParam("q", "organization_guid:" + request.getOrgId())
				.build().toUri();

		return this.restOperations.getForObject(uri, ListSpacesResponse.class);
	}

	@Override
	public RouteMappingResponse mapRoute(RouteMappingRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "routes", request.getRouteId(), "apps", request.getAppId())
				.build().toUri();

		return this.restOperations.putForObject(uri, null, RouteMappingResponse.class);
	}

	@Override
	public RemoveServiceBindingResponse removeServiceBinding(RemoveServiceBindingRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "apps", request.getAppId(), "service_bindings", request.getBindingId())
				.build().toUri();

		this.restOperations.delete(uri);
		return new RemoveServiceBindingResponse();
	}

	@Override
	public UpdateApplicationResponse updateApplication(UpdateApplicationRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "apps", request.getId())
				.build().toUri();

		return this.restOperations.putForObject(uri, request, UpdateApplicationResponse.class);
	}

	@Override
	public void unmapRoute(RouteMappingRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "routes", request.getRouteId(), "apps", request.getAppId())
				.build().toUri();

		this.restOperations.delete(uri);
	}

	@Override
	public UploadBitsResponse uploadBits(UploadBitsRequest request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment("v2", "apps", request.getId(), "bits")
				.queryParam("async", false)
				.build().toUri();

		MultiValueMap<String, Object> payload = new LinkedMultiValueMap<>();

		payload.add("application", request.getResource());
		payload.add("resources", new ArrayList());

		this.restOperations.put(uri, payload);
		return new UploadBitsResponse();
	}

}
