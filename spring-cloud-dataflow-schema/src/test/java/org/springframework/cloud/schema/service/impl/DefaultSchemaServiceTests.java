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

package org.springframework.cloud.schema.service.impl;


import java.util.HashSet;


import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersions;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.SchemaVersionTargets;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.schema.service.impl.DefaultSchemaService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests targeting {@link SchemaService} and the implementation {@link DefaultSchemaService}
 * @author Corneil du Plessis
 */
public class DefaultSchemaServiceTests {

	protected SchemaService schemaService = new DefaultSchemaService();
	@Test
	public void testVersions() {
		// when
		AppBootSchemaVersions versions = schemaService.getVersions();
		// then
		assertThat(versions).isNotNull();
		assertThat(versions.getDefaultSchemaVersion()).isEqualTo(AppBootSchemaVersion.defaultVersion());
		assertThat(versions.getVersions().size()).isEqualTo(AppBootSchemaVersion.values().length);
		assertThat(new HashSet<>(versions.getVersions()).size()).isEqualTo(AppBootSchemaVersion.values().length);
	}
	@Test
	public void testTargets() {
		// when
		SchemaVersionTargets targets = schemaService.getTargets();
		// then
		assertThat(targets).isNotNull();
		assertThat(targets.getDefaultSchemaTarget()).isEqualTo(AppBootSchemaVersion.defaultVersion().name().toLowerCase());
		assertThat(targets.getSchemas().size()).isEqualTo(AppBootSchemaVersion.values().length);
		for(final AppBootSchemaVersion schemaVersion: AppBootSchemaVersion.values()) {
			assertThat(targets.getSchemas().stream().filter(t -> t.getSchemaVersion() == schemaVersion).findFirst()).isPresent();
		}
	}
	@Test
	public void testBoot3Target() {
		// when
		SchemaVersionTarget target = schemaService.getTarget("boot3");
		// then
		assertThat(target).isNotNull();
		assertThat(target.getSchemaVersion()).isEqualTo(AppBootSchemaVersion.BOOT3);
		assertThat(target.getBatchPrefix()).isEqualTo("BOOT3_BATCH_");
		assertThat(target.getTaskPrefix()).isEqualTo("BOOT3_TASK_");
	}
	@Test
	public void testInvalidTarget() {
		// when
		SchemaVersionTarget target = schemaService.getTarget("1");
		// then
		assertThat(target).isNull();
	}
}
