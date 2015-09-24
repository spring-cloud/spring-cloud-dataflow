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
 * @author Paul Harris
 * @author Eric Bottard
 */
class CloudControllerTemplate implements CloudControllerOperations {

	private final URI endpoint;

	private final ExtendedOAuth2RestOperations restOperations;

	/**
	 * Cloud Controller REST API version
	 */
	private static final String CC_API_VERSION = "v2";

	CloudControllerTemplate(URI endpoint, ExtendedOAuth2RestOperations restOperations) {
		this.endpoint = endpoint;
		this.restOperations = restOperations;
	}

	@Override
	public Responses.ListRoutes listRoutes(Requests.ListRoutes request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "routes")
				.queryParam("q", "host:" + request.getHost())
				.queryParam("q", "domain_guid:" + request.getDomainId())
				.build().toUri();

		return this.restOperations.getForObject(uri, Responses.ListRoutes.class);
	}

	@Override
	public Responses.CreateApplication createApplication(Requests.CreateApplication request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps")
				.build().toUri();

		return this.restOperations.postForObject(uri, request, Responses.CreateApplication.class);
	}

	@Override
	public Responses.CreateRoute createRoute(Requests.CreateRoute request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "routes")
				.build().toUri();

		return this.restOperations.postForObject(uri, request, Responses.CreateRoute.class);
	}

	@Override
	public Responses.CreateServiceBinding createServiceBinding(Requests.CreateServiceBinding request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "service_bindings")
				.build().toUri();

		return this.restOperations.postForObject(uri, request, Responses.CreateServiceBinding.class);
	}

	@Override
	public Responses.DeleteApplication deleteApplication(Requests.DeleteApplication request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps", request.getId())
				.build().toUri();

		this.restOperations.delete(uri);

		return new Responses.DeleteApplication().withDeleted(true);
	}

	@Override
	public Responses.DeleteRoute deleteRoute(Requests.DeleteRoute request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "routes", request.getId())
				.build().toUri();

		this.restOperations.delete(uri);
		return new Responses.DeleteRoute();
	}

	@Override
	public Responses.GetApplicationEnvironment getApplicationEnvironment(Requests.GetApplicationEnvironment request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps", request.getId(), "env")
				.build().toUri();

		return this.restOperations.getForObject(uri, Responses.GetApplicationEnvironment.class);
	}

	@Override
	public Responses.GetApplicationStatistics getApplicationStatistics(Requests.GetApplicationStatistics request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps", request.getId(), "stats")
				.build().toUri();

		return this.restOperations.getForObject(uri, Responses.GetApplicationStatistics.class);
	}

	@Override
	public Responses.ListApplications listApplications(Requests.ListApplications request) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps")
				.queryParam("q", "space_guid:" + request.getSpaceId());
		if (!StringUtils.isEmpty(request.getName())) {
			builder.queryParam("q", "name:" + request.getName());
		}
		URI uri = builder.build().toUri();

		return this.restOperations.getForObject(uri, Responses.ListApplications.class);
	}

	@Override
	public Responses.ListOrganizations listOrganizations(Requests.ListOrganizations request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "organizations")
				.queryParam("q", "name:" + request.getName())
				.build().toUri();

		return this.restOperations.getForObject(uri, Responses.ListOrganizations.class);
	}

	@Override
	public Responses.ListServiceBindings listServiceBindings(Requests.ListServiceBindings request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps", request.getAppId(), "service_bindings")
				.build().toUri();

		return this.restOperations.getForObject(uri, Responses.ListServiceBindings.class);
	}

	@Override
	public Responses.ListServiceInstances listServiceInstances(Requests.ListServiceInstances request) {
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "service_instances")
				.queryParam("q", "space_guid:" + request.getSpaceId());
		if (!StringUtils.isEmpty(request.getName())) {
			uriComponentsBuilder.queryParam("q", "name:" + request.getName());
		}
		URI uri = uriComponentsBuilder.build().toUri();

		return this.restOperations.getForObject(uri, Responses.ListServiceInstances.class);
	}

	@Override
	public Responses.ListSharedDomains listSharedDomains(Requests.ListSharedDomains request) {
		UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "shared_domains");
		if (!StringUtils.isEmpty(request.getName())) {
			uriComponentsBuilder.queryParam("q", "name:" + request.getName());
		}
		URI uri = uriComponentsBuilder.build().toUri();

		return this.restOperations.getForObject(uri, Responses.ListSharedDomains.class);
	}

	@Override
	public Responses.ListSpaces listSpaces(Requests.ListSpaces request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "spaces")
				.queryParam("q", "name:" + request.getName())
				.queryParam("q", "organization_guid:" + request.getOrgId())
				.build().toUri();

		return this.restOperations.getForObject(uri, Responses.ListSpaces.class);
	}

	@Override
	public Responses.RouteMapping mapRoute(Requests.RouteMapping request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "routes", request.getRouteId(), "apps", request.getAppId())
				.build().toUri();

		return this.restOperations.putForObject(uri, null, Responses.RouteMapping.class);
	}

	@Override
	public Responses.RemoveServiceBinding removeServiceBinding(Requests.RemoveServiceBinding request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps", request.getAppId(), "service_bindings", request.getBindingId())
				.build().toUri();

		this.restOperations.delete(uri);
		return new Responses.RemoveServiceBinding();
	}

	@Override
	public Responses.UpdateApplication updateApplication(Requests.UpdateApplication request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps", request.getId())
				.build().toUri();

		return this.restOperations.putForObject(uri, request, Responses.UpdateApplication.class);
	}

	@Override
	public Responses.RouteMapping unmapRoute(Requests.RouteMapping request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "routes", request.getRouteId(), "apps", request.getAppId())
				.build().toUri();

		this.restOperations.delete(uri);
		return new Responses.RouteMapping();
	}

	@Override
	public Responses.UploadBits uploadBits(Requests.UploadBits request) {
		URI uri = UriComponentsBuilder.fromUri(this.endpoint)
				.pathSegment(CC_API_VERSION, "apps", request.getId(), "bits")
				.queryParam("async", false)
				.build().toUri();

		MultiValueMap<String, Object> payload = new LinkedMultiValueMap<>();

		payload.add("application", request.getResource());
		payload.add("resources", new ArrayList());

		this.restOperations.put(uri, payload);
		return new Responses.UploadBits();
	}

}
