/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner.support;

import java.util.HashMap;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * If the composed task runner is running on Cloud Foundry and is bound to a service instance from SCDF for VMware
 * Tanzu, use the values from the service binding credentials to configure the composed task runner.
 *
 * @author Mike Heath
 */
public class CfServiceBindingPropertySourceInitializer
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	private static final Logger log = LoggerFactory.getLogger(CfServiceBindingPropertySourceInitializer.class);

	public static final String PROPERTY_SOURCE_NAME = "p-dataflow";

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		final CfEnv cfEnv = new CfEnv();
		try {
			final CfCredentials credentials = cfEnv.findCredentialsByLabel("p-dataflow");
			final HashMap<String, Object> propertiesMap = new HashMap<>();
			propertiesMap.put("dataflowServerUri", credentials.getString("dataflow-url"));
			propertiesMap.put("oauth2ClientCredentialsClientId", credentials.getString("client-id"));
			propertiesMap.put("oauth2ClientCredentialsClientSecret", credentials.getString("client-secret"));
			propertiesMap.put("oauth2ClientCredentialsTokenUri", credentials.getString("access-token-url"));

			final MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, propertiesMap);
			event.getEnvironment().getPropertySources()
					.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
		} catch (IllegalArgumentException e) {
			log.debug("No 'p-dataflow' service binding found in VCAP_SERVICES");
		}
	}

}
