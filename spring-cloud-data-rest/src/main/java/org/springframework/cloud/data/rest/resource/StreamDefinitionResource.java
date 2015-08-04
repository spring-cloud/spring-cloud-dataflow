/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.data.rest.resource;

import org.springframework.cloud.data.core.StreamDefinition;
import org.springframework.cloud.data.rest.controller.StreamController;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;

/**
 * A HATEOAS representation of a {@link StreamDefinition}.
 * This class also includes a description of the
 * {@link #status stream status}.
 * <p>
 * Note: this implementation is not thread safe.
 *
 * @author Patrick Peralta
 */
public class StreamDefinitionResource extends ResourceSupport {

	/**
	 * Stream definition.
	 */
	private StreamDefinition definition;

	/**
	 * Stream status (i.e. deployed, undeployed, etc).
	 */
	private String status;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected StreamDefinitionResource() {
	}

	/**
	 * Construct a {@code StreamDefinitionResource}.
	 *
	 * @param definition stream definition object
	 */
	public StreamDefinitionResource(StreamDefinition definition) {
		this.definition = definition;
	}

	/**
	 * Return the name of this stream.
	 *
	 * @return stream name
	 */
	public String getName() {
		return definition.getName();
	}

	/**
	 * Return the DSL definition for this stream.
	 *
	 * @return stream definition DSL
	 */
	public String getDefinition() {
		return definition.getDslText();
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
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link StreamDefinition}s to {@link StreamDefinitionResource}s.
	 */
	public static class Assembler
			extends ResourceAssemblerSupport<StreamDefinition, StreamDefinitionResource> {

		public Assembler() {
			super(StreamController.class, StreamDefinitionResource.class);
		}

		@Override
		public StreamDefinitionResource toResource(StreamDefinition entity) {
			return new StreamDefinitionResource(entity);
		}
	}

	public static class Page extends PagedResources<StreamDefinitionResource> {

	}

}