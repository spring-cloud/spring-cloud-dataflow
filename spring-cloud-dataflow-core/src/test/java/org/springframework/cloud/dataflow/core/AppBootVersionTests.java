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
 * Unit tests for {@link AppBootSchemaVersion}.
 *
 * @author Chris Bono
 */
public class AppBootVersionTests {

	@Test
	void bootVersion2() {
		assertThat(AppBootSchemaVersion.BOOT2.getBootVersion()).isEqualTo("2");
	}

	@Test
	void bootVersion3() {
		assertThat(AppBootSchemaVersion.BOOT3.getBootVersion()).isEqualTo("3");
	}

	@Test
	void fromBootVersion() {
		assertThat(AppBootSchemaVersion.fromBootVersion("2")).isEqualTo(AppBootSchemaVersion.BOOT2);
		assertThatIllegalArgumentException().isThrownBy(() -> AppBootSchemaVersion.fromBootVersion("Boot2")).withMessage("Invalid AppBootSchemaVersion:Boot2");
		assertThatIllegalArgumentException().isThrownBy(() -> AppBootSchemaVersion.fromBootVersion("boot2")).withMessage("Invalid AppBootSchemaVersion:boot2");
		assertThatIllegalArgumentException().isThrownBy(() -> AppBootSchemaVersion.fromBootVersion("BOOT2")).withMessage("Invalid AppBootSchemaVersion:BOOT2");

		assertThat(AppBootSchemaVersion.fromBootVersion("3")).isEqualTo(AppBootSchemaVersion.BOOT3);
		assertThatIllegalArgumentException().isThrownBy(() -> AppBootSchemaVersion.fromBootVersion("Boot3")).withMessage("Invalid AppBootSchemaVersion:Boot3");
		assertThatIllegalArgumentException().isThrownBy(() -> AppBootSchemaVersion.fromBootVersion("boot3")).withMessage("Invalid AppBootSchemaVersion:boot3");
		assertThatIllegalArgumentException().isThrownBy(() -> AppBootSchemaVersion.fromBootVersion("BOOT3")).withMessage("Invalid AppBootSchemaVersion:BOOT3");

		assertThatIllegalArgumentException().isThrownBy(() -> AppBootSchemaVersion.fromBootVersion(null)).withMessage("Invalid AppBootSchemaVersion:null");
	}
}
