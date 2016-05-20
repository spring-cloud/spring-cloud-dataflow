/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.registry;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistryPopulator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A {@link UriRegistryPopulator} implementation that provides stream and task app URIs for a Data Flow Server.
 *
 * @author Mark Fisher
 */
public class DataFlowUriRegistryPopulator implements InitializingBean {

	private final UriRegistry registry;

	private final UriRegistryPopulator populator;

	private final DataFlowUriRegistryPopulatorProperties properties;

	/**
	 * Populates a {@link UriRegistry} on startup if
	 * {@link DataFlowUriRegistryPopulatorProperties#isEnabled()} returns {@literal true}.
	 *
	 * @param registry the {@link UriRegistry} to populate
	 * @param populator the {@link UriRegistryPopulator} to invoke
	 * @param properties the {@link DataFlowUriRegistryPopulatorProperties} to use
	 */
	public DataFlowUriRegistryPopulator(UriRegistry registry, UriRegistryPopulator populator, DataFlowUriRegistryPopulatorProperties properties) {
		Assert.notNull(registry, "registry must not be null");
		Assert.notNull(populator, "populator must not be null");
		Assert.notNull(properties, "properties must not be null");
		this.registry = registry;
		this.populator = populator;
		this.properties = properties;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.properties.isEnabled() && !ObjectUtils.isEmpty(this.properties.getLocations())) {
			this.populator.populateRegistry(this.properties.isOverwrite(), this.registry, this.properties.getLocations());
		}
	}

}
