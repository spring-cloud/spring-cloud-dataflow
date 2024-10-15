/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.autoconfigure.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.cloud.dataflow.server.config.cloudfoundry.CloudFoundryCloudProfileProvider;
import org.springframework.cloud.dataflow.server.config.kubernetes.KubernetesCloudProfileProvider;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link ProfileApplicationListener} test cases
 *
 * @author Chris Schaefer
 * @author Corneil du Plessis
 */
@ExtendWith(MockitoExtension.class)
class ProfileApplicationListenerTest {

	private MockEnvironment environment;

	@Mock
	private ApplicationEnvironmentPreparedEvent event;

	private ProfileApplicationListener profileApplicationListener;

	@BeforeEach
	void before() {
		environment = new MockEnvironment();
		when(event.getEnvironment()).thenReturn(environment);
		profileApplicationListener = new ProfileApplicationListener();
	}

	@Test
	void shouldEnableLocalProfile() {
		profileApplicationListener.onApplicationEvent(event);
		assertThat(environment.getActiveProfiles()).contains("local");
	}

	@Test
	void shouldNotEnableLocalProfileRunningOnKubernetes() {
		environment.setProperty("kubernetes_service_host", "true");
		profileApplicationListener.onApplicationEvent(event);
		assertThat(environment.getActiveProfiles()).doesNotContain("local");
		assertThat(environment.getActiveProfiles()).contains(KubernetesCloudProfileProvider.PROFILE);
	}

	@Test
	void shouldNotEnableLocalProfileRunningOnCloudFoundry() {
		environment.setProperty("VCAP_APPLICATION", "true");
		profileApplicationListener.onApplicationEvent(event);
		assertThat(environment.getActiveProfiles()).doesNotContain("local");
		assertThat(environment.getActiveProfiles()).contains(CloudFoundryCloudProfileProvider.PROFILE);
	}

	@Test
	void addedSpringCloudKubernetesConfigEnabledIsFalse() {
		profileApplicationListener.onApplicationEvent(event);
		PropertySource<?> propertySource = environment.getPropertySources().get("skipperProfileApplicationListener");
		assertThat(propertySource.containsProperty("spring.cloud.kubernetes.enabled")).isTrue();
		assertThat(propertySource.getProperty("spring.cloud.kubernetes.enabled")).isEqualTo(false);
	}

	@Test
	void backOffIfCloudProfileAlreadySet() {
		// kubernetes profile set by user
		environment.setActiveProfiles("kubernetes");
		// environment says we are on cloud foundry, the profile is 'cloud'
		environment.setProperty("VCAP_APPLICATION", "true");
		profileApplicationListener.onApplicationEvent(event);
		assertThat(environment.getActiveProfiles()).contains("kubernetes");
		// assert that we back off not setting the cloud profile
		assertThat(environment.getActiveProfiles()).doesNotContain("cloud");
	}

	@Test
	void doNotSetLocalIfKubernetesProfileIsSet() {
		// kubernetes profile set by user
		environment.setActiveProfiles("kubernetes");
		profileApplicationListener.onApplicationEvent(event);
		assertThat(environment.getActiveProfiles()).contains("kubernetes");
		// assert that we do not set local profile
		assertThat(environment.getActiveProfiles()).doesNotContain("local");
	}

	@Test
	void disableProfileApplicationListener() {
		try {
			System.setProperty(ProfileApplicationListener.IGNORE_PROFILEAPPLICATIONLISTENER_PROPERTY_NAME, "true");
			environment.setProperty("VCAP_APPLICATION", "true");
			profileApplicationListener.onApplicationEvent(event);
			assertThat(environment.getActiveProfiles()).isEmpty();
		}
		finally {
			System.clearProperty(ProfileApplicationListener.IGNORE_PROFILEAPPLICATIONLISTENER_PROPERTY_NAME);
		}
	}

}
