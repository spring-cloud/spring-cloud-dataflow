/*
 * Copyright 2017 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.springframework.cloud.dataflow.rest.resource.AppMetricResource;
import org.springframework.cloud.dataflow.rest.resource.AppMetricResource.MetricsHolder;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics.Application;
import org.springframework.cloud.dataflow.server.controller.support.ApplicationsMetrics.Instance;
import org.springframework.cloud.dataflow.server.controller.support.MetricStore;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handling metrics requests for 'runtime apps'.
 *
 * @author Janne Valkealahti
 *
 */
@RestController
@RequestMapping("/metrics/runtime")
@ExposesResourceFor(AppMetricResource.class)
public class MetricsController {

	private final ResourceAssembler<MetricsHolder, AppMetricResource> statusAssembler = new Assembler();
	private final MetricStore metricStore;

	public MetricsController(MetricStore metricStore) {
		this.metricStore = metricStore;
	}

	@RequestMapping(method = RequestMethod.POST)
	public PagedResources<AppMetricResource> list(@RequestBody(required = true) Map<String, Map<String, String>> instanceIdToGuid,
			PagedResourcesAssembler<MetricsHolder> assembler) throws ExecutionException, InterruptedException {
		// instanceIdToGuid maps deploymentId's to maps of its instanceId/guid and have format
		// {"foostream.log120RS":{"foostream.log120RS-0":"56575"},"foostream.time120RS":{"foostream.time120RS-0":"61998"}}
		// this allows us to rely of what UI currently needs and we don't need to re-query
		// deployers to get same map. It's essentially same info what RuntimeAppsController returns to UI.

		// logic for this response is. UI builds its 'runtime apps' structure based on
		// deploymentId(s)/instanced(s). instance attributes have guid added by a deployer.
		// UI passes in this structure and we build matching metrics response
		// so that UI can backmap metrics into correct positions in UI.
		// this is needed because response from collector only knows about stream and
		// label names and here we're working on deploymentId/instanceId level.

		List<ApplicationsMetrics> metricsIn = metricStore.getMetrics();

		// build response structure
		Map<String, MetricsHolder> guidToHolderMap = new HashMap<>();
		for (Entry<String, Map<String, String>> deploymentIdEntry : instanceIdToGuid.entrySet()) {
			for (Entry<String, String> instanceIdEntry : deploymentIdEntry.getValue().entrySet()) {
				guidToHolderMap.put(instanceIdEntry.getValue(),
						new MetricsHolder(instanceIdEntry.getValue(), deploymentIdEntry.getKey(), instanceIdEntry.getKey()));
			}
		}

		// update metrics to response
		for (ApplicationsMetrics am : metricsIn) {
			for (Application a : am.getApplications()) {
				for (Instance i : a.getInstances()) {
					MetricsHolder holder = guidToHolderMap.get(i.getGuid());
					if (holder != null) {
						holder.getMetrics().put("incomingRate", i.getIncomingRate());
						holder.getMetrics().put("outgoingRate", i.getOutgoingRate());
					}
				}
			}
		}

		// build hal response
		List<MetricsHolder> holders = new ArrayList<>(guidToHolderMap.values());
		return assembler.toResource(new PageImpl<>(holders), statusAssembler);
	}

	private static class Assembler extends ResourceAssemblerSupport<MetricsHolder, AppMetricResource> {

		public Assembler() {
			super(MetricsController.class, AppMetricResource.class);
		}

		@Override
		public AppMetricResource toResource(MetricsHolder entity) {
			return new AppMetricResource(entity);
		}
	}
}
