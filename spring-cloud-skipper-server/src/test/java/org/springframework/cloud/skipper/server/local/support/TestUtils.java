/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.skipper.server.local.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Utils for tests.
 *
 * @author Janne Valkealahti
 * @author Soby Chacko
 * @author gunnar Hillert
 */
public class TestUtils {

	public static Map<String, String> toImmutableMap(
			String... entries) {
		if (entries.length % 2 == 1) {
			throw new IllegalArgumentException("Entries should contain even number of values");
		}
		return Collections.unmodifiableMap(IntStream.range(0, entries.length / 2).map(i -> i * 2)
				.collect(HashMap::new,
						(m, i) -> m.put(entries[i], entries[i + 1]),
						Map::putAll));
	}

}
