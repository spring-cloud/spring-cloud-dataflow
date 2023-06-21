/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.cloud.dataflow.schema.service;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.schema.service.impl.DefaultSchemaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for schema service and related components.
 * @author Corneil du Plessis
 */
@Configuration
public class SchemaServiceConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(SchemaServiceConfiguration.class);
	@Bean
	public SchemaService schemaService() {
		logger.info("schemaService:starting");
		try {
			return new DefaultSchemaService();
		} finally {
			logger.info("schemaService:started");
		}
	}

	@PostConstruct
	public void setup() {
		logger.info("created: org.springframework.cloud.dataflow.schema.service.SchemaServiceConfiguration");
	}
}
