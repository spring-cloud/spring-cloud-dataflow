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

package org.springframework.cloud.dataflow.schema.service.impl;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersions;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.SchemaVersionTargets;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.stereotype.Service;

/**
 * Implements a simple service to provide Schema versions and targets.
 * In the future this will use a database to store the {@link SchemaVersionTarget}
 * @author Corneil du Plessis
 */
public class DefaultSchemaService implements SchemaService {
	private static final Logger logger = LoggerFactory.getLogger(DefaultSchemaService.class);
	private final Map<String, SchemaVersionTarget> targets;

	public DefaultSchemaService() {
		targets = Arrays.stream(AppBootSchemaVersion.values())
				.map(SchemaVersionTarget::createDefault)
				.collect(Collectors.toMap(SchemaVersionTarget::getName, Function.identity()));
	}

	@Override
	public AppBootSchemaVersions getVersions() {
		return new AppBootSchemaVersions(AppBootSchemaVersion.defaultVersion(), Arrays.asList(AppBootSchemaVersion.values()));
	}

	@Override
	public SchemaVersionTargets getTargets() {
		return new SchemaVersionTargets(getDefaultSchemaTarget(), new ArrayList<>(targets.values()));
	}

	private static String getDefaultSchemaTarget() {
		return AppBootSchemaVersion.defaultVersion().name().toLowerCase();
	}

	@Override
	public SchemaVersionTarget getTarget(String name) {
		if (name == null) {
			name = getDefaultSchemaTarget();
		}
		return targets.get(name);
	}
	@PostConstruct
	public void setup() {
		logger.info("created: org.springframework.cloud.dataflow.schema.service.impl.DefaultSchemaService");
	}
}
