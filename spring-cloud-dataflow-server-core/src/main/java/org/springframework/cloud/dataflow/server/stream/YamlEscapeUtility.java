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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;

/**
 * @author Christian Tzolov
 */
public class YamlEscapeUtility {

	private List<String> filterKeywords;

	public YamlEscapeUtility() {
		this.filterKeywords = new ArrayList<>();
		this.filterKeywords.add("expression");
	}

	public YamlEscapeUtility(List<String> filterKeywords) {
		this.filterKeywords = filterKeywords;
	}

	/**
	 * In Java '\\\\' sands for escaped backslash. E.g. it corresponds to '\\' in regular expression.
	 *
	 * (?<!\\) - (negative lookbehind) - the previous character is not a backslash.
	 * (\\)    - (non-capturing group) - current character is a backslash
	 * (?![\\0abtnvfreN_LP\s"]) - (negative lookforward) - next character is neither of \, 0, a, b, t, n, v, f, r, e, N, _, L, P, ,\s, "
	 */
	private static final Pattern SINGLE_BACKSLASH = Pattern.compile("(?<!\\\\)(\\\\)(?![\\\\0abtnvfreN_LP\\s\"])");

	public Map escapeSingleBackslashInProperties(Map<String, String> properties) {
		return MapUtils.unmodifiableMap(properties.entrySet().stream()
				.collect(Collectors.toMap(
						e -> e.getKey(),
						e -> this.isEscapeCandidate(e.getKey()) ? backslashEscape(e.getValue()) : e.getValue())));
	}

	private boolean isEscapeCandidate(String text) {
		return this.filterKeywords.stream().filter(v -> text.contains(v)).findFirst().isPresent();
	}

	public String backslashEscape(String text) {
		return SINGLE_BACKSLASH.matcher(text).replaceAll("\\\\\\\\");
	}
}
