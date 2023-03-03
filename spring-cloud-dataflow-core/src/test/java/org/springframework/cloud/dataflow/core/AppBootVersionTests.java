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

package org.springframework.cloud.dataflow.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link AppBootVersion}.
 *
 * @author Chris Bono
 */
public class AppBootVersionTests {

	@Test
	void bootVersion2() {
		assertThat(AppBootVersion.BOOT2.getBootVersion()).isEqualTo("2");
		assertThat(AppBootVersion.BOOT2.getDescription()).isEqualTo("Boot 2 Tasks/Jobs");
		assertThat(AppBootVersion.BOOT2.getTaskPrefix()).isNull();
		assertThat(AppBootVersion.BOOT2.getBatchPrefix()).isNull();
		assertThat(AppBootVersion.BOOT2.toString()).isEqualTo(
				"AppBootVersion{description='Boot 2 Tasks/Jobs', bootVersion='2', taskPrefix='null', batchPrefix='null'}");
	}

	@Test
	void bootVersion3() {
		assertThat(AppBootVersion.BOOT3.getBootVersion()).isEqualTo("3");
		assertThat(AppBootVersion.BOOT3.getDescription()).isEqualTo("Boot 3 Tasks/Jobs");
		assertThat(AppBootVersion.BOOT3.getTaskPrefix()).isEqualTo("BOOT3_TASK_");
		assertThat(AppBootVersion.BOOT3.getBatchPrefix()).isEqualTo("BOOT3_BATCH_");
		assertThat(AppBootVersion.BOOT3.toString()).isEqualTo(
				"AppBootVersion{description='Boot 3 Tasks/Jobs', bootVersion='3', taskPrefix='BOOT3_TASK_', batchPrefix='BOOT3_BATCH_'}");

	}

	@Test
	void fromBootVersion() {
		assertThat(AppBootVersion.fromBootVersion("2")).isEqualTo(AppBootVersion.BOOT2);
		assertThat(AppBootVersion.fromBootVersion("3")).isEqualTo(AppBootVersion.BOOT3);
		assertThatIllegalArgumentException().isThrownBy(() -> AppBootVersion.fromBootVersion(null))
				.withMessage("Unsupported bootVersion: null");
		assertThatIllegalArgumentException().isThrownBy(() -> AppBootVersion.fromBootVersion("4"))
				.withMessage("Unsupported bootVersion: 4");
	}

}
