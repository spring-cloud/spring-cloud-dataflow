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

package org.springframework.cloud.dataflow.server.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Contributes the values from {@code META-INF/dataflow-server-defaults.yml} and
 * {@code dataflow-server.yml} if it exists, before any of Spring Boot's normal
 * configuration contributions apply. This has the effect of supplying overridable defaults to the
 * various Spring Cloud Data Flow Deployer SPI implementations that in turn override the defaults
 * provided by Spring Boot.
 *
 * @author Josh Long
 * @author Janne Valkealahti
 */
public class DefaultEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static Log logger = LogFactory.getLog(DefaultEnvironmentPostProcessor.class);
	private final Resource serverResource = new ClassPathResource("/dataflow-server.yml");
	private final Resource serverDefaultsResource = new ClassPathResource("META-INF/dataflow-server-defaults.yml");

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> internalDefaults = new HashMap<>();
		Map<String, Object> defaults = new HashMap<>();
		MutablePropertySources existingPropertySources = environment.getPropertySources();

		contributeDefaults(internalDefaults, serverDefaultsResource);
		contributeDefaults(defaults, serverResource);

		String defaultPropertiesKey = "defaultProperties";

		if (!existingPropertySources.contains(defaultPropertiesKey) ||
				existingPropertySources.get(defaultPropertiesKey) == null) {
			existingPropertySources.addLast(new MapPropertySource(defaultPropertiesKey, internalDefaults));
			existingPropertySources.addLast(new MapPropertySource(defaultPropertiesKey, defaults));
		}
		else {
			PropertySource<?> propertySource = existingPropertySources.get(defaultPropertiesKey);
			@SuppressWarnings("unchecked")
			Map<String, Object> mapOfProperties = Map.class.cast(propertySource.getSource());
			for (String k : internalDefaults.keySet()) {
				Set<String> setOfPropertyKeys = mapOfProperties.keySet();
				if (!setOfPropertyKeys.contains(k)) {
					mapOfProperties.put(k, internalDefaults.get(k));
					logger.debug(k + '=' + internalDefaults.get(k));
				}
			}
			for (String k : defaults.keySet()) {
				Set<String> setOfPropertyKeys = mapOfProperties.keySet();
				if (!setOfPropertyKeys.contains(k)) {
					mapOfProperties.put(k, defaults.get(k));
					logger.debug(k + '=' + defaults.get(k));
				}
			}
		}

	}

	@Override
	public int getOrder() {
		return 0;
	}

	private static void contributeDefaults(Map<String, Object> defaults, Resource resource) {
		if (resource.exists()) {
			YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
			yamlPropertiesFactoryBean.setResources(resource);
			yamlPropertiesFactoryBean.afterPropertiesSet();
			Properties p = yamlPropertiesFactoryBean.getObject();
			for (Object k : p.keySet()) {
				String key = k.toString();
				defaults.put(key, p.get(key));
			}
		}
	}
}
