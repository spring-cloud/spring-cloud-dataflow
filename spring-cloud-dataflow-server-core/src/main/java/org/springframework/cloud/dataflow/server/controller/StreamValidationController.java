/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.cloud.dataflow.rest.resource.StreamAppStatusResource;
import org.springframework.cloud.dataflow.server.service.StreamService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link ValidationStatus}.
 *
 * @author Glenn Renfro

 */
@RestController
@RequestMapping("/streams/validation")
@ExposesResourceFor(StreamAppStatusResource.class)
public class StreamValidationController {

	/**
	 * The service that is responsible for validating streams.
	 */
	private final StreamService streamService;

	/**
	 * Create a {@code StreamValidationController} that delegates to {@link StreamService}.
	 *
	 * @param streamService the stream service to use
	 */
	public StreamValidationController(StreamService streamService) {
		Assert.notNull(streamService, "StreamService must not be null");
		this.streamService = streamService;
	}

	/**
	 * Return {@link StreamAppStatusResource} showing the validation status of the apps in a stream.
	 *
	 * @param name name of the stream definition
	 * @return The status for the apps in a stream definition.
	 */
	@GetMapping("/{name}")
	@ResponseStatus(HttpStatus.OK)
	public StreamAppStatusResource validate(
			@PathVariable String name) {
		ValidationStatus result = this.streamService.validateStream(name);
		return new Assembler().toModel(result);
	}

	/**
	 * {@link org.springframework.hateoas.server.ResourceAssembler} implementation that converts
	 * {@link ValidationStatus}s to {@link StreamAppStatusResource}s.
	 */
	class Assembler extends RepresentationModelAssemblerSupport<ValidationStatus, StreamAppStatusResource> {

		public Assembler() {
			super(StreamValidationController.class, StreamAppStatusResource.class);
		}

		@Override
		public StreamAppStatusResource toModel(ValidationStatus entity) {
			return new StreamAppStatusResource(entity.getDefinitionName(), entity.getDefinitionDsl(), entity.getDescription(), entity.getAppsStatuses());
		}
	}
}
