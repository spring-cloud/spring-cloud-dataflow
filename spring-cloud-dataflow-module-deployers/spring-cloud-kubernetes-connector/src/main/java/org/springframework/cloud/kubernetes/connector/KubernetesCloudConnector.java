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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.cloud.AbstractCloudConnector;
import org.springframework.cloud.FallbackServiceInfoCreator;
import org.springframework.cloud.ServiceInfoCreator;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.cloud.app.BasicApplicationInstanceInfo;
import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.util.EnvironmentAccessor;
import org.springframework.util.Assert;

/**
 * A Cloud Connecter for Kubernetes.
 *
 * @author Eric Bottard
 */
public class KubernetesCloudConnector extends AbstractCloudConnector<Service> {

	private final EnvironmentAccessor environment = new EnvironmentAccessor();

	private final KubernetesClient kubernetes;

	private final String[] supportedLabelValueSelectors;

	public KubernetesCloudConnector() {
		super((Class) KubernetesServiceInfoCreator.class);
		String masterURL = environment.getEnvValue("SPRING_CLOUD_KUBERNETES_MASTER_URL");
		if (masterURL == null) {
			// Default value for vagrant VM install
			masterURL = "https://10.245.1.2";
		}
		this.kubernetes = new DefaultKubernetesClient(masterURL);

		Set<String> values = new HashSet<>();
		for (ServiceInfoCreator<?, Service> serviceInfoCreator : serviceInfoCreators) {
			String value = ((KubernetesServiceInfoCreator)serviceInfoCreator).getIdentifyingLabelValue();
			Assert.state(!values.contains(value), "Multiple ServiceInfoCreators implemented to recognize "
					+ KubernetesServiceInfoCreator.SPRING_CLOUD_SERVICE_LABEL + "=" + value);
			values.add(value);
		}
		supportedLabelValueSelectors = values.toArray(new String[values.size()]);
	}


	@Override
	protected List<Service> getServicesData() {
		return kubernetes.services()
				.withLabelIn(KubernetesServiceInfoCreator.SPRING_CLOUD_SERVICE_LABEL, supportedLabelValueSelectors)
				.list().getItems();
	}

	@Override
	protected FallbackServiceInfoCreator<?, Service> getFallbackServiceInfoCreator() {
		return new FallbackServiceInfoCreator<BaseServiceInfo, Service>() {
			@Override
			public BaseServiceInfo createServiceInfo(Service service) {
				return new BaseServiceInfo(service.getMetadata().getUid());
			}
		};
	}

	@Override
	public boolean isInMatchingCloud() {
		return environment.getEnvValue("KUBERNETES_PORT") != null;
	}

	@Override
	public ApplicationInstanceInfo getApplicationInstanceInfo() {
		return new BasicApplicationInstanceInfo("TODO", "TOTO", Collections.<String, Object>emptyMap());
	}
}
