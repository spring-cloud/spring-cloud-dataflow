/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.cloud.dataflow.server.stream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Tzolov
 */
public class YamlEscapeUtilTest {

	private YamlEscapeUtility yamlEscapeUtility;

	@Before
	public void before() {
		yamlEscapeUtility = new YamlEscapeUtility();
	}

	@Test
	public void testSkipEscapeForYamlSpecialCharacters() {
		assertThat(yamlEscapeUtility.backslashEscape("\\d \\\\ \\0 \\a \\b \t \\t \\n \\v \\f \\r \\e \\N \\_ \\L \\\" \\P \\ "),
				is("\\\\d \\\\ \\0 \\a \\b \t \\t \\n \\v \\f \\r \\e \\N \\_ \\L \\\" \\P \\ "));
	}

	@Test
	public void testDefaultFilterKeywords() {
		Map<String, String> map = new HashMap<>();
		map.put("foo.expression", "\\d");
		map.put("foo.simple", "\\d");

		Map escapedMap = yamlEscapeUtility.escapeSingleBackslashInProperties(map);
		assertThat(escapedMap.size(), is(2));
		assertThat(escapedMap.get("foo.expression"), is("\\\\d"));
		assertThat(escapedMap.get("foo.simple"), is("\\d"));
	}

	@Test
	public void testAlternativeFilterKeywords() {

		Map<String, String> map = new HashMap<>();
		map.put("expression", "\\d");
		map.put("foo.blue", "\\d");
		map.put("green.bar", "\\d");

		Map escapedMap = new YamlEscapeUtility(Arrays.asList("foo", "bar")).escapeSingleBackslashInProperties(map);
		assertThat(escapedMap.size(), is(3));
		assertThat(escapedMap.get("expression"), is("\\d"));
		assertThat(escapedMap.get("foo.blue"), is("\\\\d"));
		assertThat(escapedMap.get("green.bar"), is("\\\\d"));
	}
}
