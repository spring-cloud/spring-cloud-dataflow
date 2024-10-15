/*
 * Copyright 2021 the original author or authors.
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

/**
 * Tests for {@code Base64Utils}.
 *
 * @author Janne Valkealahti
 * @author Corneil du Plessis
 */
class Base64UtilsTests {

	@Test
	void base64() {
		assertThat(Base64Utils.decode(null)).isNull();
		assertThat(Base64Utils.encode(null)).isNull();
		assertThat(Base64Utils.decode(Base64Utils.encode("foo"))).isEqualTo("foo");
		assertThat(Base64Utils.decode(Base64Utils.encode("foo.*.1"))).isEqualTo("foo.*.1");
		assertThat(Base64Utils.decode("juststring")).isEqualTo("juststring");
	}
}
