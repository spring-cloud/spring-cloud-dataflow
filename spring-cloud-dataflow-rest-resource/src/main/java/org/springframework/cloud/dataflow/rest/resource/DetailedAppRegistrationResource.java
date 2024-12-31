/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.hateoas.PagedModel;

/**
 * Extension of {@link AppRegistrationResource} that contains application options and
 * other detailed application information.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class DetailedAppRegistrationResource extends AppRegistrationResource {

	/**
	 * List of application options.
	 */
	private final List<ConfigurationMetadataProperty> options = new ArrayList<>();

	/**
	 * Optional short description of the application.
	 */
	private String shortDescription;

	/**
	 * Inbound port names configured for the app.
	 */
	private final Set<String> inboundPortNames = new HashSet<>();

	/**
	 * Outbound port names configured for the app.
	 */
	private final Set<String> outboundPortNames = new HashSet<>();

	/**
	 * Option groups configured for the app.
	 */
	private final Map<String, Set<String>> optionGroups = new HashMap<>();

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected DetailedAppRegistrationResource() {
	}

	/**
	 * Construct a {@code DetailedAppRegistrationResource} object.
	 *
	 * @param name application name
	 * @param type application type
	 * @param version application version
	 * @param coordinates Maven coordinates for the application artifact
	 * @param isDefault is this the default app
	 */
	public DetailedAppRegistrationResource(String name, String type, String version, String coordinates,  Boolean isDefault) {
		super(name, type, version, coordinates, null, isDefault);
	}

	/**
	 * Construct a {@code DetailedAppRegistrationResource} object based on the provided
	 * {@link AppRegistrationResource}.
	 *
	 * @param resource {@code AppRegistrationResource} from which to obtain app registration
	 * data
	 */
	public DetailedAppRegistrationResource(AppRegistrationResource resource) {
		super(resource.getName(), resource.getType(), resource.getVersion(), resource.getUri(), resource.getMetaDataUri(), resource.getDefaultVersion());
	}

	/**
	 * Add an application option.
	 *
	 * @param option application option to add
	 */
	public void addOption(ConfigurationMetadataProperty option) {
		options.add(option);
	}

	/**
	 * Return a list of application options.
	 *
	 * @return list of application options
	 */
	public List<ConfigurationMetadataProperty> getOptions() {
		return options;
	}

	/**
	 * Add application's inbound port name.
	 *
	 * @param inboundPortName application's inbound port name to add
	 */
	public void addInboundPortName(String inboundPortName) {
		this.inboundPortNames.add(inboundPortName);
	}

	/**
	 * Add application's outbound port name.
	 *
	 * @param outboundPortName application's outbound port name to add
	 */
	public void addOutboundPortName(String outboundPortName) {
		this.outboundPortNames.add(outboundPortName);
	}

	/**
	 * Return a set of application's inbound port names.
	 *
	 * @return set of application's inbound port names.
	 */
	public Set<String> getInboundPortNames() {
		return this.inboundPortNames;
	}


	/**
	 * Return a set of application's outbound port names.
	 *
	 * @return set of application's outbound port names.
	 */
	public Set<String> getOutboundPortNames() {
		return this.outboundPortNames;
	}


	/**
	 * Return a description for this application.
	 *
	 * @return description for this application
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * Return an option groups.
	 *
	 * @return the option groups
	 */
	public Map<String, Set<String>> getOptionGroups() {
		return optionGroups;
	}

	/**
	 * Set a description for this application.
	 *
	 * @param shortDescription description for application
	 */
	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 */
	public static class Page extends PagedModel<DetailedAppRegistrationResource> {
	}

}
