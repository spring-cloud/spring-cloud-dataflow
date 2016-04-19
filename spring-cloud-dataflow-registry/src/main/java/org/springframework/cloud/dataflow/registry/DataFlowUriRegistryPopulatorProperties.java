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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for a {@link DataFlowUriRegistryPopulator}
 *
 * @author Mark Fisher
 */
@ConfigurationProperties(prefix = "spring.cloud.dataflow.registry.populator")
public class DataFlowUriRegistryPopulatorProperties {

	/**
	 * Indicates whether to populate the registry on startup. Default is {@literal true}.
	 */
	private boolean enabled = true;

	/**
	 * Resource locations for one or more (comma-delimited) properties files where the keys are
	 * stream or task app names (e.g. "source.foo" or "task.bar") and the values are Resource URIs.
	 * Default is an empty array. An example of a valid value for this property would be:
	 * {@code "classpath:META-INF/stream-apps.properties,classpath:META-INF/task-apps.properties"}.
	 */
	private String[] locations = new String[] {};

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String[] getLocations() {
		return locations;
	}

	public void setLocations(String[] locations) {
		this.locations = locations;
	}
}
