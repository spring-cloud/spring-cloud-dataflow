/*
 * Copyright 2015-2017 the original author or authors.
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

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 * A HATEOAS representation of a {@link StreamDefinition}. This class also includes a
 * description of the {@link #status stream status}.
 * <p>
 * Note: this implementation is not thread safe.
 *
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
public class StreamDefinitionResource extends ResourceSupport {

	/**
	 * Stream name.
	 */
	private String name;

	/**
	 * Stream definition DSL text.
	 */
	private String dslText;

	/**
	 * Stream status (i.e. deployed, undeployed, etc).
	 */
	private String status;

	/**
	 * Description of the Stream status.
	 */
	private String statusDescription;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected StreamDefinitionResource() {
	}

	/**
	 * Construct a {@code StreamDefinitionResource}.
	 *
	 * @param name stream name
	 * @param dslText stream definition DSL text
	 */
	public StreamDefinitionResource(String name, String dslText) {
		this.name = name;
		this.dslText = dslText;
	}

	/**
	 * Return the name of this stream.
	 *
	 * @return stream name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the DSL definition for this stream.
	 *
	 * @return stream definition DSL
	 */
	public String getDslText() {
		return this.dslText;
	}

	/**
	 * Return the status of this stream (i.e. deployed, undeployed, etc).
	 *
	 * @return stream status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status of this stream (i.e. deployed, undeployed, etc).
	 *
	 * @param status stream status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Get a descriptive text of the stream's deployment status. See also
	 * {@link DeploymentStateResource}.
	 *
	 * @return a descriptive text of the stream's deployment status
	 */
	public String getStatusDescription() {
		return statusDescription;
	}

	/**
	 * Set the descriptive text of the stream's deployment status. See also
	 * {@link DeploymentStateResource}
	 *
	 * @param statusDescription the stream's deployment status description
	 */
	public void setStatusDescription(String statusDescription) {
		this.statusDescription = statusDescription;
	}

	public static class Page extends PagedResources<StreamDefinitionResource> {

	}

}
