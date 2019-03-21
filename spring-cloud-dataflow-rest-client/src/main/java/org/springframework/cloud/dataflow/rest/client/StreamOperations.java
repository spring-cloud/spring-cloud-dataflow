/*
 * Copyright 2015 the original author or authors.
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

import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.hateoas.PagedResources;

/**
 * Interface defining operations available against streams.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public interface StreamOperations {

	/**
	 * @return the list streams known to the system.
	 */
	public PagedResources<StreamDefinitionResource> list();

	/**
	 * Create a new stream, optionally deploying it.
	 *
	 * @param name the name of the stream
	 * @param definition the stream definition DSL
	 * @param deploy whether to deploy the stream after creating its definition
	 * @return the new stream definition
	 */
	public StreamDefinitionResource createStream(String name, String definition, boolean deploy);

	/**
	 * Deploy an already created stream.
	 *
	 * @param name the name of the stream
	 * @param properties the deployment properties
	 */
	public void deploy(String name, Map<String, String> properties);

	/**
	 * Undeploy a deployed stream, retaining its definition.
	 *
	 * @param name the name of the stream
	 */
	public void undeploy(String name);

	/**
	 * Undeploy all currently deployed streams.
	 */
	public void undeployAll();

	/**
	 * Destroy an existing stream.
	 *
	 * @param name the name of the stream
	 */
	public void destroy(String name);

	/**
	 * Destroy all streams known to the system.
	 */
	public void destroyAll();

}
