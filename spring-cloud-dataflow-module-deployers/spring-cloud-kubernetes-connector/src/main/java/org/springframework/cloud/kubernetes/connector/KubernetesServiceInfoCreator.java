/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.kubernetes.connector;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Service;

import org.springframework.cloud.ServiceInfoCreator;
import org.springframework.cloud.service.ServiceInfo;

/**
 * Base class for ServiceInfoCreators running on Kubernetes. Needed for service discovery via AbstractCloudConnector.
 *
 * @author Eric Bottard
 */
public abstract class KubernetesServiceInfoCreator<SI extends ServiceInfo> implements ServiceInfoCreator<SI, Service> {

	public static final String SPRING_CLOUD_SERVICE_LABEL = "spring-cloud-service";

	private final String identifyingLabelValue;

	protected KubernetesServiceInfoCreator(String identifyingLabelValue) {
		this.identifyingLabelValue = identifyingLabelValue;
	}

	@Override
	public boolean accept(Service serviceData) {
		Map<String, String> labels = serviceData.getMetadata().getLabels();
		return labels != null && identifyingLabelValue.equalsIgnoreCase(labels.get(SPRING_CLOUD_SERVICE_LABEL));
	}

	public String getIdentifyingLabelValue() {
		return identifyingLabelValue;
	}
}
