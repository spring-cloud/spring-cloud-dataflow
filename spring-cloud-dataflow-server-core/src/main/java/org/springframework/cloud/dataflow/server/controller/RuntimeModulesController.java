/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.server.repository.StreamDefinitionRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes runtime status of deployed modules.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
@RestController
@RequestMapping("/runtime/modules")
@ExposesResourceFor(AppStatusResource.class)
public class RuntimeModulesController {

	private static final Comparator<? super AppInstanceStatus> INSTANCE_SORTER = new Comparator<AppInstanceStatus>() {
		@Override
		public int compare(AppInstanceStatus i1, AppInstanceStatus i2) {
			return i1.getId().compareTo(i2.getId());
		}
	};

	private final StreamDefinitionRepository streamDefinitionRepository;

	private final AppDeployer appDeployer;

	private final ResourceAssembler<AppStatus, AppStatusResource> statusAssembler = new Assembler();

	public RuntimeModulesController(StreamDefinitionRepository streamDefinitionRepository, AppDeployer appDeployer) {
		this.streamDefinitionRepository = streamDefinitionRepository;
		this.appDeployer = appDeployer;
	}

	@RequestMapping
	public PagedResources<AppStatusResource> list(PagedResourcesAssembler<AppStatus> assembler) {
		List<AppStatus> values = new ArrayList<>();
		for (StreamDefinition streamDefinition : this.streamDefinitionRepository.findAll()) {
			String stream = streamDefinition.getName();
			for (ModuleDefinition moduleDefinition : streamDefinition.getModuleDefinitions()) {
				String module = moduleDefinition.getLabel();
				values.add(this.appDeployer.status(String.format("%s.%s", stream, module)));
			}
		}
		Collections.sort(values, new Comparator<AppStatus>() {
			@Override
			public int compare(AppStatus o1, AppStatus o2) {
				return o1.getDeploymentId().compareTo(o2.getDeploymentId());
			}
		});
		return assembler.toResource(new PageImpl<>(values), statusAssembler);
	}

	@RequestMapping("/{id}")
	public AppStatusResource display(@PathVariable String id) {
		AppStatus status = appDeployer.status(id);
		if (status != null) {
			return statusAssembler.toResource(status);
		}
		throw new ResourceNotFoundException();
	}

	private class Assembler extends ResourceAssemblerSupport<AppStatus, AppStatusResource> {

		public Assembler() {
			super(RuntimeModulesController.class, AppStatusResource.class);
		}

		@Override
		public AppStatusResource toResource(AppStatus entity) {
			return createResourceWithId(entity.getDeploymentId(), entity);
		}

		@Override
		protected AppStatusResource instantiateResource(AppStatus entity) {
			AppStatusResource resource = new AppStatusResource(entity.getDeploymentId(), entity.getState().name());
			List<AppInstanceStatusResource> instanceStatusResources = new ArrayList<>();
			InstanceAssembler instanceAssembler = new InstanceAssembler(entity);
			List<AppInstanceStatus> instanceStatuses = new ArrayList<>(entity.getInstances().values());
			Collections.sort(instanceStatuses, INSTANCE_SORTER);
			for (AppInstanceStatus appInstanceStatus : instanceStatuses) {
				instanceStatusResources.add(instanceAssembler.toResource(appInstanceStatus));
			}
			resource.setInstances(new Resources<>(instanceStatusResources));
			return resource;
		}
	}

	@RestController
	@RequestMapping("/runtime/modules/{moduleId}/instances")
	@ExposesResourceFor(AppInstanceStatusResource.class)
	public static class AppInstanceController {

		private final AppDeployer appDeployer;

		public AppInstanceController(AppDeployer appDeployer) {
			this.appDeployer = appDeployer;
		}

		@RequestMapping
		public PagedResources<AppInstanceStatusResource> list(@PathVariable String moduleId,
				PagedResourcesAssembler<AppInstanceStatus> assembler) {
			AppStatus status = appDeployer.status(moduleId);
			if (status != null) {
				List<AppInstanceStatus> appInstanceStatuses = new ArrayList<>(status.getInstances().values());
				Collections.sort(appInstanceStatuses, INSTANCE_SORTER);
				return assembler.toResource(new PageImpl<>(appInstanceStatuses), new InstanceAssembler(status));
			}
			throw new ResourceNotFoundException();
		}

		@RequestMapping("/{instanceId}")
		public AppInstanceStatusResource display(@PathVariable String moduleId, @PathVariable String instanceId) {
			AppStatus status = appDeployer.status(moduleId);
			if (status != null) {
				AppInstanceStatus appInstanceStatus = status.getInstances().get(instanceId);
				if (appInstanceStatus == null) {
					throw new ResourceNotFoundException();
				}
				return new InstanceAssembler(status).toResource(appInstanceStatus);
			}
			throw new ResourceNotFoundException();
		}

	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	private static class ResourceNotFoundException extends RuntimeException {

	}

	private static class InstanceAssembler extends ResourceAssemblerSupport<AppInstanceStatus, AppInstanceStatusResource> {

		private final AppStatus owningModule;

		public InstanceAssembler(AppStatus owningModule) {
			super(AppInstanceController.class, AppInstanceStatusResource.class);
			this.owningModule = owningModule;
		}

		@Override
		public AppInstanceStatusResource toResource(AppInstanceStatus entity) {
			return createResourceWithId("/" + entity.getId(), entity, owningModule.getDeploymentId().toString());
		}

		@Override
		protected AppInstanceStatusResource instantiateResource(AppInstanceStatus entity) {
			return new AppInstanceStatusResource(entity.getId(), entity.getState().name(), entity.getAttributes());
		}
	}
}
