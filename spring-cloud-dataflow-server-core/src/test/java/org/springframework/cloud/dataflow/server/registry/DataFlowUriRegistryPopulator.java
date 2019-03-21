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
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistryPopulator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Populates the registry with stream and task app URIs.
 *
 * @author Mark Fisher
 */
public class DataFlowUriRegistryPopulator implements InitializingBean {

	private final UriRegistry registry;

	private final UriRegistryPopulator populator;

	private final String[] locations;

	/**
	 * Populates a {@link UriRegistry} on startup.
	 *
	 * @param registry the {@link UriRegistry} to populate
	 * @param populator {@link DataFlowUriRegistryPopulator} to use 
	 * @param locations the properties file(s) listing apps to import into the registry
	 */
	public DataFlowUriRegistryPopulator(UriRegistry registry, UriRegistryPopulator populator, String... locations) {
		Assert.notNull(registry, "UriRegistry must not be null");
		Assert.notNull(populator, "UriRegistryPopulator must not be null");
		this.registry = registry;
		this.populator = populator;
		this.locations = locations;
	}

	@Override
	public void afterPropertiesSet() {
		if (!ObjectUtils.isEmpty(this.locations)) {
			this.populator.populateRegistry(true, this.registry, this.locations);
		}
	}

}
