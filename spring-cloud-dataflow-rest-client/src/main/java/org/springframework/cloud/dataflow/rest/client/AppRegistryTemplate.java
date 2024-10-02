/*
 * Copyright 2015-2023 the original author or authors.
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

import java.util.Properties;

import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.DetailedAppRegistrationResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of {@link AppRegistryOperations} that uses {@link RestTemplate} to issue
 * commands to the Data Flow server.
 *
 * @author Eric Bottard
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Patrick Peralta
 * @author Christian Tzolov
 * @author Chris Schaefer
 * @author Chris Bono
 * @author Corneil du Plessis
 */
public class AppRegistryTemplate implements AppRegistryOperations {
	/**
	 * Apps relation path
	 */
	private static final String APPS_REL = "apps";

	/**
	 * {@link Link} to apps
	 */
	private Link appsLink;

	/**
	 * Template used for http interaction.
	 */
	protected RestTemplate restTemplate;

	/**
	 * Construct a {@code AppRegistryTemplate} object.
	 *
	 * @param restTemplate    template for HTTP/rest commands
	 * @param resourceSupport HATEOAS link support
	 */
	public AppRegistryTemplate(RestTemplate restTemplate, RepresentationModel<?> resourceSupport) {
		Assert.notNull(resourceSupport, "URI CollectionModel can't be null");
		Assert.notNull(resourceSupport.getLink(APPS_REL), "Apps relation is required");

		this.restTemplate = restTemplate;
		this.appsLink = resourceSupport.getLink(APPS_REL).get();
	}

	@Override
	public PagedModel<AppRegistrationResource> list() {
		return list(/* ApplicationType */null);
	}

	@Override
	public PagedModel<AppRegistrationResource> list(ApplicationType type) {
		String uri = appsLink.getHref() + "?size=2000" + ((type == null) ? "" : "&type=" + type.name());
		return restTemplate.getForObject(uri, AppRegistrationResource.Page.class);
	}

	@Override
	public void unregister(String name, ApplicationType applicationType) {
		String uri = appsLink.getHref() + "/{type}/{name}";
		restTemplate.delete(uri, applicationType.name(), name);
	}

	@Override
	public void unregister(String name, ApplicationType applicationType, String version) {
		String uri = appsLink.getHref() + "/{type}/{name}/{version}";
		restTemplate.delete(uri, applicationType.name(), name, version);
	}

	@Override
	public void unregisterAll() {
		restTemplate.delete(appsLink.getHref());
	}

	@Override
	public DetailedAppRegistrationResource info(String name, ApplicationType type, boolean exhaustive) {
		String uri = appsLink.getHref() + "/{type}/{name}?exhaustive={exhaustive}";
		return restTemplate.getForObject(uri, DetailedAppRegistrationResource.class, type, name, exhaustive);
	}

	@Override
	public DetailedAppRegistrationResource info(String name, ApplicationType type, String version, boolean exhaustive) {
		String uri = appsLink.getHref() + "/{type}/{name}/{version}?exhaustive={exhaustive}";
		return restTemplate.getForObject(uri, DetailedAppRegistrationResource.class, type, name, version, exhaustive);
	}

	@Override
	public AppRegistrationResource register(
			String name,
			ApplicationType type,
			String uri,
			String metadataUri,
			boolean force
	) {
		MultiValueMap<String, Object> values = valuesForRegisterPost(uri, metadataUri, force);
		return restTemplate.postForObject(appsLink.getHref() + "/{type}/{name}", values,
				AppRegistrationResource.class, type, name);
	}

	@Override
	public AppRegistrationResource register(
			String name,
			ApplicationType type,
			String version,
			String uri,
			String metadataUri,
			boolean force
	) {
		MultiValueMap<String, Object> values = valuesForRegisterPost(uri, metadataUri, force);
		return restTemplate.postForObject(appsLink.getHref() + "/{type}/{name}/{version}", values,
				AppRegistrationResource.class, type, name, version);
	}

	private MultiValueMap<String, Object> valuesForRegisterPost(
			String uri,
			String metadataUri,
			boolean force
	) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("uri", uri);
		if (metadataUri != null) {
			values.add("metadata-uri", metadataUri);
		}
		values.add("force", Boolean.toString(force));
		return values;
	}

	@Override
	public PagedModel<AppRegistrationResource> importFromResource(String uri, boolean force) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("uri", uri);
		values.add("force", Boolean.toString(force));
		return restTemplate.postForObject(appsLink.getHref(), values, AppRegistrationResource.Page.class);
	}

	@Override
	public PagedModel<AppRegistrationResource> registerAll(Properties apps, boolean force) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		StringBuffer buffer = new StringBuffer();
		for (String key : apps.stringPropertyNames()) {
			buffer.append(String.format("%s=%s\n", key, apps.getProperty(key)));
		}
		values.add("apps", buffer.toString());
		values.add("force", Boolean.toString(force));
		return restTemplate.postForObject(appsLink.getHref(), values, AppRegistrationResource.Page.class);
	}

	@Override
	public void makeDefault(String name, ApplicationType type, String version) {
		restTemplate.put(appsLink.getHref() + "/{type}/{name}/{version}", null, type, name, version);
	}
}
