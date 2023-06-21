/*
 * Copyright 2023 the original author or authors.
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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.rest.resource.SchemaVersionTargetResource;
import org.springframework.cloud.dataflow.rest.resource.SchemaVersionTargetsResource;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersions;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.SchemaVersionTargets;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Provides REST endpoint for {@link SchemaService}
 *
 * @author Corneil du Plessis
 */
@RestController
@RequestMapping("/schema")
public class SchemaController {
	private final SchemaService schemaService;
	private final SchemaVersionTargetResourceAssembler targetAssembler = new SchemaVersionTargetResourceAssembler();
	private final SchemaVersionTargetsResourceAssembler targetsAssembler = new SchemaVersionTargetsResourceAssembler(targetAssembler);

	public SchemaController(SchemaService schemaService) {
		this.schemaService = schemaService;
	}

	@RequestMapping(value = "/versions", method = RequestMethod.GET)
	public ResponseEntity<AppBootSchemaVersions> getVersions() {
		return ResponseEntity.ok(schemaService.getVersions());
	}

	@RequestMapping(value = "/targets", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public SchemaVersionTargetsResource getTargets() {
		return targetsAssembler.toModel(schemaService.getTargets());
	}

	@RequestMapping(value = "/targets/{schemaTarget}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public SchemaVersionTargetResource getTarget(@PathVariable("schemaTarget") String schemaTarget) {
		SchemaVersionTarget target = schemaService.getTarget(schemaTarget);
		if (target == null) {
			throw new NoSuchSchemaTargetException(schemaTarget);
		}
		return targetAssembler.toModel(target);
	}

	public static class SchemaVersionTargetResourceAssembler extends RepresentationModelAssemblerSupport<SchemaVersionTarget, SchemaVersionTargetResource> {
		public SchemaVersionTargetResourceAssembler() {
			super(SchemaController.class, SchemaVersionTargetResource.class);
		}

		@Override
		public SchemaVersionTargetResource toModel(SchemaVersionTarget entity) {
			SchemaVersionTargetResource resource = new SchemaVersionTargetResource(entity.getName(), entity.getSchemaVersion(), entity.getTaskPrefix(), entity.getBatchPrefix(), entity.getDatasource());
			resource.add(linkTo(methodOn(SchemaController.class).getTarget(entity.getName())).withSelfRel());
			return resource;
		}
	}


	static class SchemaVersionTargetsResourceAssembler extends RepresentationModelAssemblerSupport<SchemaVersionTargets, SchemaVersionTargetsResource> {
		private final RepresentationModelAssembler<SchemaVersionTarget, SchemaVersionTargetResource> assembler;

		public SchemaVersionTargetsResourceAssembler(RepresentationModelAssembler<SchemaVersionTarget, SchemaVersionTargetResource> assembler) {
			super(SchemaController.class, SchemaVersionTargetsResource.class);
			this.assembler = assembler;
		}

		@Override
		public SchemaVersionTargetsResource toModel(SchemaVersionTargets entity) {
			List<SchemaVersionTargetResource> targets = entity.getSchemas().stream()
					.map(target -> assembler.toModel(target))
					.collect(Collectors.toList());
			SchemaVersionTargetsResource resource = new SchemaVersionTargetsResource(entity.getDefaultSchemaTarget(), targets);
			resource.add(linkTo(methodOn(SchemaController.class).getTargets()).withSelfRel());
			return resource;
		}
	}
}
