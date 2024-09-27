/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Corneil du Plessis
 */
class AssertionsTests {

	@Test
	void atMostOneWithNone() {
		Assertions.atMostOneOf("foo", null, "bar", null);
	}

	@Test
	void atMostOneWithOne() {
		Assertions.atMostOneOf("foo", "x", "bar", null);
	}

	@Test
	void atMostOneWithTwo() {
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
			Assertions.atMostOneOf("foo", "x", "bar", "y");
		});
	}

	@Test
	void atMostOneWithOddArgs() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			Assertions.atMostOneOf("foo", "x", "bar", null, "oops");
		});
	}

	@Test
	void atMostOneWithNonStringKey() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			assertThat(Assertions.atMostOneOf("foo", null, 99, "y")).isEqualTo(1);
		});
	}

	@Test
	void exactlyOneWithNone() {
		assertThatThrownBy(() -> {
			assertThat(Assertions.exactlyOneOf("foo", null, "bar", null, "baz", null)).isEqualTo(1);
		}).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void exactlyOneWithOne() {
		assertThat(Assertions.exactlyOneOf("foo", null, "bar", "y", "baz", null)).isEqualTo(1);
	}

	@Test
	void exactlyOneWithTwo() {
		assertThatThrownBy(() -> {
			assertThat(Assertions.exactlyOneOf("foo", "x", "bar", "y", "baz", null)).isEqualTo(1);
		}).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void exactlyOneWithOddArgs() {
		assertThatThrownBy(() -> {
			assertThat(Assertions.exactlyOneOf("foo", null, "bar", "y", "oops")).isEqualTo(1);
		}).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void exactlyOneWithNonStringKey() {
		assertThatThrownBy(() -> {
			assertThat(Assertions.exactlyOneOf("foo", null, 99, "y")).isEqualTo(1);
		}).isInstanceOf(IllegalArgumentException.class);
	}
}
