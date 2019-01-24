/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.autoconfigure.local;

import java.util.Arrays;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Sets an active Spring profile of "local" when determined the current DataFlow instance
 * is a local server.
 *
 * @author Chris Schaefer
 */
public class LocalProfileApplicationListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {
	private static final String VCAP_SERVICES = "VCAP_SERVICES";

	protected static final String LOCAL_PROFILE_NAME = "local";

	protected static final String VCAP_APPLICATION = "VCAP_APPLICATION";

	protected static final String KUBERNETES_SERVICE_HOST = "kubernetes_service_host";

	private ConfigurableEnvironment environment;

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		this.environment = event.getEnvironment();

		if (isLocalServer()) {
			if (!Arrays.asList(environment.getActiveProfiles()).contains(LOCAL_PROFILE_NAME)) {
				environment.addActiveProfile(LOCAL_PROFILE_NAME);
			}
		}
	}

	private boolean isLocalServer() {
		return !isKubernetes() && !isCloudFoundry();
	}

	private boolean isKubernetes() {
		return environment.containsProperty(KUBERNETES_SERVICE_HOST);
	}

	private boolean isCloudFoundry() {
		return environment.containsProperty(VCAP_APPLICATION) || environment.containsProperty(VCAP_SERVICES);
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
