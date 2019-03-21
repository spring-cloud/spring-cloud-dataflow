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

package org.springframework.cloud.dataflow.server.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics;
import org.springframework.cloud.dataflow.server.controller.support.ControllerUtils;
import org.springframework.cloud.dataflow.server.controller.support.MetricStore;
import org.springframework.cloud.dataflow.server.stream.StreamDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes runtime status of deployed apps.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Janne Valkealahti
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @author Christian Tzolov
 */
@RestController
@RequestMapping("/runtime/apps")
@ExposesResourceFor(AppStatusResource.class)
public class RuntimeAppsController {

	private static final Comparator<? super AppInstanceStatus> INSTANCE_SORTER = new Comparator<AppInstanceStatus>() {
		@Override
		public int compare(AppInstanceStatus i1, AppInstanceStatus i2) {
			return i1.getId().compareTo(i2.getId());
		}
	};

	/**
	 * Common stream deployer logics
	 */
	private final StreamDeployer streamDeployer;

	private final ResourceAssembler<AppStatus, AppStatusResource> statusAssembler = new Assembler();

	private final MetricStore metricStore;

	/**
	 * Instantiates a new runtime apps controller.
	 *
	 * @param streamDeployer the deployer this controller will use to get the status of deployed stream apps
	 * @param metricStore the proxy to the metrics collector
	 */
	public RuntimeAppsController(StreamDeployer streamDeployer, MetricStore metricStore) {
		Assert.notNull(streamDeployer, "StreamDeployer must not be null");
		Assert.notNull(metricStore, "MetricStore must not be null");
		this.streamDeployer = streamDeployer;
		this.metricStore = metricStore;
	}

	@RequestMapping
	public PagedResources<AppStatusResource> list(Pageable pageable, PagedResourcesAssembler<AppStatus> assembler)
			throws ExecutionException, InterruptedException {

		Page<AppStatus> statuses = streamDeployer.getAppStatuses(pageable);

		enrichWithMetrics(statuses);
		// finally, pass in pageable and tell how many items we have in all pages
		return assembler.toResource(new PageImpl<>(statuses.getContent(), pageable, statuses.getTotalElements()), statusAssembler);

	}

	private void enrichWithMetrics(Page<AppStatus> statuses) {
		List<ApplicationsMetrics> metricsIn = metricStore.getMetrics();
		Map<String, ApplicationsMetrics.Instance> metricsInstanceMap = new HashMap<>();
		for (ApplicationsMetrics am : metricsIn) {
			for (ApplicationsMetrics.Application a : am.getApplications()) {
				for (ApplicationsMetrics.Instance i : a.getInstances()) {
					metricsInstanceMap.put(i.getGuid(), i);
				}
			}
		}

		for (AppStatus appStatus : statuses) {
			Map<String, AppInstanceStatus> appInstanceStatusMap = appStatus.getInstances();
			appInstanceStatusMap.forEach((k, appInstanceStatus) -> {
				Map<String, String> attributes = appInstanceStatus.getAttributes();
				if (attributes != null && !attributes.isEmpty()) {
					String trackingKey = attributes.get("guid");
					if (metricsInstanceMap.containsKey(trackingKey)) {
						ApplicationsMetrics.Instance metricsAppInstance = metricsInstanceMap.get(trackingKey);
						List<ApplicationsMetrics.Metric> metrics = metricsAppInstance.getMetrics();
						if (metrics != null) {
							for (ApplicationsMetrics.Metric m : metrics) {
								if (ObjectUtils.nullSafeEquals("integration.channel.input.send.mean", m.getName())) {
									appInstanceStatus.getAttributes()
											.put("metrics.integration.channel.input.receiveRate",
													String.format(Locale.US, "%.2f", m.getValue()));
								}
								else if (ObjectUtils
										.nullSafeEquals("integration.channel.output.send.mean", m.getName())) {
									appInstanceStatus.getAttributes().put("metrics.integration.channel.output.sendRate",
											String.format(Locale.US, "%.2f", m.getValue()));
								}
							}
						}
					}
				}
			});
		}
	}

	@RequestMapping("/{id}")
	public AppStatusResource display(@PathVariable String id) {
		AppStatus status = streamDeployer.getAppStatus(id);
		if (status.getState().equals(DeploymentState.unknown)) {
			throw new NoSuchAppException(id);
		}
		return statusAssembler.toResource(status);
	}

	private static class Assembler extends ResourceAssemblerSupport<AppStatus, AppStatusResource> {

		public Assembler() {
			super(RuntimeAppsController.class, AppStatusResource.class);
		}

		@Override
		public AppStatusResource toResource(AppStatus entity) {
			return createResourceWithId(entity.getDeploymentId(), entity);
		}

		@Override
		protected AppStatusResource instantiateResource(AppStatus entity) {
			AppStatusResource resource = new AppStatusResource(entity.getDeploymentId(),
					ControllerUtils.mapState(entity.getState()).getKey());
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
	@RequestMapping("/runtime/apps/{appId}/instances")
	@ExposesResourceFor(AppInstanceStatusResource.class)
	public static class AppInstanceController {

		private final StreamDeployer streamDeployer;

		public AppInstanceController(StreamDeployer streamDeployer) {
			this.streamDeployer = streamDeployer;
		}

		@RequestMapping
		public PagedResources<AppInstanceStatusResource> list(Pageable pageable, @PathVariable String appId,
				PagedResourcesAssembler<AppInstanceStatus> assembler) {
			AppStatus status = streamDeployer.getAppStatus(appId);
			if (status.getState().equals(DeploymentState.unknown)) {
				throw new NoSuchAppException(appId);
			}
			List<AppInstanceStatus> appInstanceStatuses = new ArrayList<>(status.getInstances().values());
			Collections.sort(appInstanceStatuses, INSTANCE_SORTER);
			return assembler.toResource(new PageImpl<>(appInstanceStatuses, pageable,
					appInstanceStatuses.size()), new InstanceAssembler(status));
		}

		@RequestMapping("/{instanceId}")
		public AppInstanceStatusResource display(@PathVariable String appId, @PathVariable String instanceId) {
			AppStatus status = streamDeployer.getAppStatus(appId);
			if (status.getState().equals(DeploymentState.unknown)) {
				throw new NoSuchAppException(appId);
			}
			AppInstanceStatus appInstanceStatus = status.getInstances().get(instanceId);
			if (appInstanceStatus == null) {
				throw new NoSuchAppInstanceException(instanceId);
			}
			return new InstanceAssembler(status).toResource(appInstanceStatus);
		}
	}

	private static class InstanceAssembler
			extends ResourceAssemblerSupport<AppInstanceStatus, AppInstanceStatusResource> {

		private final AppStatus owningApp;

		InstanceAssembler(AppStatus owningApp) {
			super(AppInstanceController.class, AppInstanceStatusResource.class);
			this.owningApp = owningApp;
		}

		@Override
		public AppInstanceStatusResource toResource(AppInstanceStatus entity) {
			return createResourceWithId("/" + entity.getId(), entity, owningApp.getDeploymentId());
		}

		@Override
		protected AppInstanceStatusResource instantiateResource(AppInstanceStatus entity) {
			return new AppInstanceStatusResource(entity.getId(), ControllerUtils.mapState(entity.getState()).getKey(),
					entity.getAttributes());
		}
	}
}
