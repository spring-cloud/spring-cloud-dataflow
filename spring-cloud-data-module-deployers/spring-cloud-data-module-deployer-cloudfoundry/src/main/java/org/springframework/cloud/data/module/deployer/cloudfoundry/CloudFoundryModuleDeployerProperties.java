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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Eric Bottard
 */
@ConfigurationProperties("cloudfoundry")
class CloudFoundryModuleDeployerProperties {

	/**
	 * The names of services to bind to each application deployed as a module.
	 * This should typically contain a service capable of playing the role of a binding transport.
	 */
	private Set<String> services = setOf(new HashSet<String>(), "redis");

	/**
	 * The domain to use when mapping routes for applications.
	 */
	private String domain;

	/**
	 * The organization to use when registering new applications.
	 */
	private String organization;

	/**
	 * The space to use when registering new applications.
	 */
	private String space;


	public Set<String> getServices() {
		return services;
	}

	public void setServices(Set<String> services) {
		this.services = services;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

	public String getSpace() {
		return space;
	}

	public void setSpace(String space) {
		this.space = space;
	}

	@SafeVarargs
	private static <T> Set<T> setOf(Set<T> set, T... elements) {
		Collections.addAll(set, elements);
		return set;
	}
}
