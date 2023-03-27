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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cloud.dataflow.core.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.core.AppBootSchemaVersions;
import org.springframework.cloud.dataflow.core.SchemaVersionTarget;
import org.springframework.cloud.dataflow.core.SchemaVersionTargets;
import org.springframework.cloud.dataflow.server.service.SchemaService;
import org.springframework.stereotype.Service;

/**
 * Implements a simple service to provide Schema versions and targets.
 *
 * @author Corneil du Plessis
 */
@Service
public class DefaultSchemaService implements SchemaService {
	private Map<String, SchemaVersionTarget> targets;

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
		return new SchemaVersionTargets(AppBootSchemaVersion.defaultVersion().name().toLowerCase(), new ArrayList<>(targets.values()));
	}

	@Override
	public SchemaVersionTarget getTarget(String name) {
		// TODO we can decide the throw an exception resulting in NOT_FOUND.
		return targets.get(name);
	}
}
