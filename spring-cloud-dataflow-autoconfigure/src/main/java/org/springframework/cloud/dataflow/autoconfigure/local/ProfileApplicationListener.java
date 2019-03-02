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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.cloud.dataflow.server.config.CloudProfileProvider;
import org.springframework.cloud.dataflow.server.config.kubernetes.KubernetesCloudProfileProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Programmatically sets the profile using implementations of {@link CloudProfileProvider} discovered from the
 * {@link ServiceLoader}.
 * If no cloud providers are found, the {@literal local} profile is set.
 * You can disable this class by setting the environment variable
 * {@literal SPRING_CLOUD_SKIPPER_SERVER_PROFILEAPPLICATIONLISTENER_IGNORE} to {@literal true}.
 * If the kubernetes profile has not been detected, then the property {@literal spring.cloud.kubernetes.enabled}
 * is set to false in order to disable functionality in the spring-cloud-kubernetes library that would result
 * in the logging of warning message.
 *
 * @author Chris Schaefer
 * @author Mark Pollack
 */
public class ProfileApplicationListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	/**
	 * System property that when set to {@code true} will not set the active profiles using
	 * CloudProfileProvider implementations discovered from the ServiceLoader.
	 */
	public static final String IGNORE_PROFILEAPPLICATIONLISTENER_PROPERTY_NAME = "spring.cloud.dataflow.server.profileapplicationlistener.ignore";

	public static final String IGNORE_PROFILEAPPLICATIONLISTENER_ENVVAR_NAME = "SPRING_CLOUD_DATAFLOW_SERVER_PROFILEAPPLICATIONLISTENER_IGNORE";

	private static final Logger logger = LoggerFactory.getLogger(ProfileApplicationListener.class);
	private ConfigurableEnvironment environment;

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		this.environment = event.getEnvironment();
		Iterable<CloudProfileProvider> cloudProfileProviders = ServiceLoader.load(CloudProfileProvider.class);

		if (ignoreFromSystemProperty()
				|| ignoreFromEnvironmentVariable()
				|| cloudProfilesAlreadySet(cloudProfileProviders)) {
			return;
		}

		boolean addedCloudProfile = false;
		boolean addedKubernetesProfile = false;
		for (CloudProfileProvider cloudProfileProvider : cloudProfileProviders) {
			if (cloudProfileProvider.isCloudPlatform(environment)) {
				String profileToAdd = cloudProfileProvider.getCloudProfile();
				if (!Arrays.asList(environment.getActiveProfiles()).contains(profileToAdd)) {
					if (profileToAdd.equals(KubernetesCloudProfileProvider.PROFILE)) {
						addedKubernetesProfile = true;
					}
					environment.addActiveProfile(profileToAdd);
					addedCloudProfile = true;
				}
			}
		}

		if (!addedKubernetesProfile) {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("spring.cloud.kubernetes.enabled", false);
			logger.info("Setting property 'spring.cloud.kubernetes.enabled' to false.");
			MutablePropertySources propertySources = environment.getPropertySources();
			if (propertySources != null) {
				if (propertySources.contains(
						CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
					propertySources.addAfter(
							CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
							new MapPropertySource("skipperProfileApplicationListener", properties));
				}
				else {
					propertySources
							.addFirst(new MapPropertySource("skipperProfileApplicationListener", properties));
				}
			}
		}

		if (!addedCloudProfile) {
			environment.addActiveProfile("local");
		}

	}

	private boolean ignoreFromSystemProperty() {
		return Boolean.getBoolean(IGNORE_PROFILEAPPLICATIONLISTENER_PROPERTY_NAME);
	}

	private boolean ignoreFromEnvironmentVariable() {
		return Boolean.parseBoolean(System.getenv(IGNORE_PROFILEAPPLICATIONLISTENER_ENVVAR_NAME));
	}

	@Override
	public int getOrder() {
		return 0;
	}

	private boolean cloudProfilesAlreadySet(Iterable<CloudProfileProvider> cloudProfileProviders) {
		List<String> cloudProfileNames = new ArrayList<>();
		for (CloudProfileProvider cloudProfileProvider : cloudProfileProviders) {
			cloudProfileNames.add(cloudProfileProvider.getCloudProfile());
		}

		boolean cloudProfilesAlreadySet = false;
		for (String cloudProfileName : cloudProfileNames) {
			if (Arrays.asList(environment.getActiveProfiles()).contains(cloudProfileName)) {
				cloudProfilesAlreadySet = true;
			}
		}

		return cloudProfilesAlreadySet;
	}
}
