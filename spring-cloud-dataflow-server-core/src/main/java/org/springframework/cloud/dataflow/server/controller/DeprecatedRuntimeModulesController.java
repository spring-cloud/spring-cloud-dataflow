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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
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
 */
@RestController
@RequestMapping("/runtime/modules")
@ExposesResourceFor(AppStatusResource.class)
@Deprecated
public class DeprecatedRuntimeModulesController {

	private static final Comparator<? super ModuleInstanceStatus> INSTANCE_SORTER = new Comparator<ModuleInstanceStatus>() {
		@Override
		public int compare(ModuleInstanceStatus i1, ModuleInstanceStatus i2) {
			return i1.getId().compareTo(i2.getId());
		}
	};

	private final Collection<ModuleDeployer> moduleDeployers;

	private final ResourceAssembler<ModuleStatus, AppStatusResource> statusAssembler = new Assembler();

	@Autowired
	public DeprecatedRuntimeModulesController(Collection<ModuleDeployer> moduleDeployers) {
		this.moduleDeployers = new HashSet<>(moduleDeployers);
	}

	@RequestMapping
	public PagedResources<AppStatusResource> list(PagedResourcesAssembler<ModuleStatus> assembler) {
		List<ModuleStatus> values = new ArrayList<>();
		for (ModuleDeployer moduleDeployer : moduleDeployers) {
			values.addAll(moduleDeployer.status().values());
		}
		Collections.sort(values, new Comparator<ModuleStatus>() {
			@Override
			public int compare(ModuleStatus o1, ModuleStatus o2) {
				return o1.getModuleDeploymentId().toString().compareTo(o2.getModuleDeploymentId().toString());
			}
		});
		return assembler.toResource(new PageImpl<>(values), statusAssembler);
	}

	@RequestMapping("/{id}")
	public AppStatusResource display(@PathVariable String id) {
		ModuleDeploymentId moduleDeploymentId = ModuleDeploymentId.parse(id);
		for (ModuleDeployer moduleDeployer : moduleDeployers) {
			ModuleStatus status = moduleDeployer.status(moduleDeploymentId);
			if (status != null) {
				return statusAssembler.toResource(status);
			}
		}
		throw new ResourceNotFoundException();
	}

	private class Assembler extends ResourceAssemblerSupport<ModuleStatus, AppStatusResource> {

		public Assembler() {
			super(DeprecatedRuntimeModulesController.class, AppStatusResource.class);
		}

		@Override
		public AppStatusResource toResource(ModuleStatus entity) {
			return createResourceWithId(entity.getModuleDeploymentId(), entity);
		}

		@Override
		protected AppStatusResource instantiateResource(ModuleStatus entity) {
			AppStatusResource resource = new AppStatusResource(entity.getModuleDeploymentId().toString(), entity.getState().name());
			List<AppInstanceStatusResource> instanceStatusResources = new ArrayList<>();
			InstanceAssembler instanceAssembler = new InstanceAssembler(entity);
			List<ModuleInstanceStatus> instanceStatuses = new ArrayList<>(entity.getInstances().values());
			Collections.sort(instanceStatuses, INSTANCE_SORTER);
			for (ModuleInstanceStatus moduleInstanceStatus : instanceStatuses) {
				instanceStatusResources.add(instanceAssembler.toResource(moduleInstanceStatus));
			}
			resource.setInstances(new Resources<>(instanceStatusResources));
			return resource;
		}
	}

	@RestController
	@RequestMapping("/runtime/modules/{moduleId}/instances")
	@ExposesResourceFor(AppInstanceStatusResource.class)
	public static class DeprecatedInstanceController {

		private final Collection<ModuleDeployer> moduleDeployers;

		@Autowired
		public DeprecatedInstanceController(Collection<ModuleDeployer> moduleDeployers) {
			this.moduleDeployers = new HashSet<>(moduleDeployers);
		}

		@RequestMapping
		public PagedResources<AppInstanceStatusResource> list(@PathVariable String moduleId,
				PagedResourcesAssembler<ModuleInstanceStatus> assembler) {
			ModuleDeploymentId moduleDeploymentId = ModuleDeploymentId.parse(moduleId);
			for (ModuleDeployer moduleDeployer : moduleDeployers) {
				ModuleStatus status = moduleDeployer.status(moduleDeploymentId);
				if (status != null) {
					List<ModuleInstanceStatus> moduleInstanceStatuses = new ArrayList<>(status.getInstances().values());
					Collections.sort(moduleInstanceStatuses, INSTANCE_SORTER);
					return assembler.toResource(new PageImpl<>(moduleInstanceStatuses), new InstanceAssembler(status));
				}
			}
			throw new ResourceNotFoundException();
		}

		@RequestMapping("/{instanceId}")
		public AppInstanceStatusResource display(@PathVariable String moduleId, @PathVariable String instanceId) {
			ModuleDeploymentId moduleDeploymentId = ModuleDeploymentId.parse(moduleId);
			for (ModuleDeployer moduleDeployer : moduleDeployers) {
				ModuleStatus status = moduleDeployer.status(moduleDeploymentId);
				if (status != null) {
					ModuleInstanceStatus moduleInstanceStatus = status.getInstances().get(instanceId);
					if (moduleInstanceStatus == null) {
						throw new ResourceNotFoundException();
					}
					return new InstanceAssembler(status).toResource(moduleInstanceStatus);
				}
			}
			throw new ResourceNotFoundException();
		}

	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	private static class ResourceNotFoundException extends RuntimeException {

	}

	private static class InstanceAssembler extends ResourceAssemblerSupport<ModuleInstanceStatus, AppInstanceStatusResource> {

		private final ModuleStatus owningModule;

		public InstanceAssembler(ModuleStatus owningModule) {
			super(DeprecatedInstanceController.class, AppInstanceStatusResource.class);
			this.owningModule = owningModule;
		}

		@Override
		public AppInstanceStatusResource toResource(ModuleInstanceStatus entity) {
			return createResourceWithId("/" + entity.getId(), entity, owningModule.getModuleDeploymentId().toString());
		}

		@Override
		protected AppInstanceStatusResource instantiateResource(ModuleInstanceStatus entity) {
			return new AppInstanceStatusResource(entity.getId(), entity.getState().name(), entity.getAttributes());
		}
	}
}
