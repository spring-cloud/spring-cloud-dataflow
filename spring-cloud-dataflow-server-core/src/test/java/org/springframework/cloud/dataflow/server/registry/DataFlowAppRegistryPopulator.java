/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.registry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Populates the registry with stream and task app URIs.
 *
 * @author Mark Fisher
 */
public class DataFlowAppRegistryPopulator implements InitializingBean, ResourceLoaderAware {

	private final AppRegistryService registry;

	private final String[] locations;

	private ResourceLoader resourceLoader;

	/**
	 * Populates a {@link org.springframework.cloud.dataflow.registry.service.AppRegistryService} on
	 * startup.
	 *
	 * @param registry the {@link AppRegistryService} to populate
	 * @param locations the properties file(s) listing apps to import into the registry
	 */
	public DataFlowAppRegistryPopulator(AppRegistryService registry, String... locations) {
		Assert.notNull(registry, "AppRegistryService must not be null");
		this.registry = registry;
		this.locations = locations;
	}

	@Override
	public void afterPropertiesSet() {
		if (!ObjectUtils.isEmpty(this.locations)) {
			Resource[] resources = new Resource[locations.length];
			for (int i = 0; i < resources.length; i++) {
				resources[i] = resourceLoader.getResource(locations[i]);
			}
			registry.importAll(true, resources);
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
