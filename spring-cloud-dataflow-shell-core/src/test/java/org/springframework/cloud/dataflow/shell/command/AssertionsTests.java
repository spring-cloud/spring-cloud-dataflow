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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mark Fisher
 * @author Corneil du Plessis
 */
public class AssertionsTests {

	@Test
	public void atMostOneWithNone() {
		Assertions.atMostOneOf("foo", null, "bar", null);
	}

	@Test
	public void atMostOneWithOne() {
		Assertions.atMostOneOf("foo", "x", "bar", null);
	}

	@Test
	public void atMostOneWithTwo() {
		assertThrows(IllegalStateException.class, () -> {
			Assertions.atMostOneOf("foo", "x", "bar", "y");
		});
	}

	@Test
	public void atMostOneWithOddArgs() {
		assertThrows(IllegalArgumentException.class, () -> {
			Assertions.atMostOneOf("foo", "x", "bar", null, "oops");
		});
	}

	@Test
	public void atMostOneWithNonStringKey() {
		assertThrows(IllegalArgumentException.class, () -> {
			assertEquals(1, Assertions.atMostOneOf("foo", null, 99, "y"));
		});
	}

	@Test
	public void exactlyOneWithNone() {
		assertThrows(IllegalStateException.class, () -> {
			assertEquals(1, Assertions.exactlyOneOf("foo", null, "bar", null, "baz", null));
		});
	}

	@Test
	public void exactlyOneWithOne() {
		assertEquals(1, Assertions.exactlyOneOf("foo", null, "bar", "y", "baz", null));
	}

	@Test
	public void exactlyOneWithTwo() {
		assertThrows(IllegalStateException.class, () -> {
			assertEquals(1, Assertions.exactlyOneOf("foo", "x", "bar", "y", "baz", null));
		});
	}

	@Test
	public void exactlyOneWithOddArgs() {
		assertThrows(IllegalArgumentException.class, () -> {
			assertEquals(1, Assertions.exactlyOneOf("foo", null, "bar", "y", "oops"));
		});
	}

	@Test
	public void exactlyOneWithNonStringKey() {
		assertThrows(IllegalArgumentException.class, () -> {
			assertEquals(1, Assertions.exactlyOneOf("foo", null, 99, "y"));
		});
	}
}
