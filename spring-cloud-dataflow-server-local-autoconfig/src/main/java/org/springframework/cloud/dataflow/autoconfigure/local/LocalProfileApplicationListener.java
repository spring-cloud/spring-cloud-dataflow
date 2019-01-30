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
import java.util.ServiceLoader;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.cloud.dataflow.server.config.CloudProfileProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Sets an active Spring profile of "local" when determined the current DataFlow instance
 * is not running on a cloud platform.
 *
 * @author Chris Schaefer
 * @author Mark Pollack
 */
public class LocalProfileApplicationListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private ConfigurableEnvironment environment;

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		this.environment = event.getEnvironment();

		Iterable<CloudProfileProvider> cloudProfileProviders = ServiceLoader.load(CloudProfileProvider.class);
		boolean addedCloudProfile = false;
		for (CloudProfileProvider cloudProfileProvider : cloudProfileProviders) {
			if (cloudProfileProvider.isCloudPlatform(environment)) {
				String profileToAdd = cloudProfileProvider.getCloudProfile();
				if (!Arrays.asList(environment.getActiveProfiles()).contains(profileToAdd)) {
					environment.addActiveProfile(profileToAdd);
					addedCloudProfile = true;
				}
			}
		}
		if (!addedCloudProfile) {
			environment.addActiveProfile("local");
		}

	}

	@Override
	public int getOrder() {
		return 0;
	}
}
