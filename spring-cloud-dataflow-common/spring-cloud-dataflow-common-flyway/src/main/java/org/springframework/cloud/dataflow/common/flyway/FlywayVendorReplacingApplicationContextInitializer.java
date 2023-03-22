/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.common.flyway;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * An {@link ApplicationContextInitializer} that replaces any configured 'spring.flyways.locations'
 * properties that contain the '{vendor}' token with 'mysql' when using the MariaDB driver
 * to access a MySQL database.
 *
 * <p>Typically property manipulation like this is implemented as an {@link EnvironmentPostProcessor} but
 * in order to work with applications that are using Config server it must be a context initializer
 * so it can run after the {@code org.springframework.cloud.bootstrap.config.PropertySourceBootstrapConfiguration}
 * context initializer.
 *
 * @author Chris Bono
 */
public class FlywayVendorReplacingApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

	private final Logger log = LoggerFactory.getLogger(FlywayVendorReplacingApplicationContextInitializer.class);

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {

		ConfigurableEnvironment env = applicationContext.getEnvironment();

		// If there is a spring.datasource.url prefixed w/ "jdbc:mysql:" and using the MariaDB driver then replace {vendor}
		boolean usingMariaDriver = env.getProperty("spring.datasource.driver-class-name", "").equals("org.mariadb.jdbc.Driver");
		boolean usingMySqlUrl = env.getProperty("spring.datasource.url", "").startsWith("jdbc:mysql:");
		if (!(usingMariaDriver && usingMySqlUrl)) {
			return;
		}

		log.info("Using MariaDB driver w/ MySQL url - looking for '{vendor}' in 'spring.flyway.locations'");

		// Look for spring.flyway.locations[0..N] and if found then override it w/ vendor replaced version
		Map<String, Object> replacedLocations = new HashMap<>();

		int prodIdx = 0;
		while (true) {
			String locationPropName = String.format("spring.flyway.locations[%d]", prodIdx++);
			String configuredLocation = env.getProperty(locationPropName);
			if (configuredLocation == null) {
				break;
			}
			if (configuredLocation.contains("{vendor}")) {
				String replaceLocation = configuredLocation.replace("{vendor}", "mysql");
				replacedLocations.put(locationPropName, replaceLocation);
			}
		}

		if (replacedLocations.isEmpty()) {
			log.info("No properties with '{vendor}' found to replace");
			return;
		}

		log.info("Replacing '{vendor}' in {}", replacedLocations);

		env.getPropertySources().addFirst(new MapPropertySource("overrideVendorInFlywayLocations", replacedLocations));
	}

	/**
	 * The precedence for execution order - should execute last.
	 *
	 * @return lowest precedence to ensure it executes after other initializers
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}
