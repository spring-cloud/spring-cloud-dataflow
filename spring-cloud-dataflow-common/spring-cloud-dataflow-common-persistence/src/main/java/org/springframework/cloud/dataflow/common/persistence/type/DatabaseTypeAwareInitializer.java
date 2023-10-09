/*
 * Copyright 2019-2020 the original author or authors.
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
package org.springframework.cloud.dataflow.common.persistence.type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class DatabaseTypeAwareInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {
	private static final Logger logger = LoggerFactory.getLogger(DatabaseTypeAwareInitializer.class);
	private static Boolean postgresDatabase = null;

	public DatabaseTypeAwareInitializer() {
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment env = applicationContext.getEnvironment();
		String property = env.getProperty("spring.datasource.driver-class-name", "");
		logger.info("checking database driver type:{}", property);
		postgresDatabase = property.contains("postgres");
	}

	public static Boolean getPostgresDatabase() {
		return postgresDatabase;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
