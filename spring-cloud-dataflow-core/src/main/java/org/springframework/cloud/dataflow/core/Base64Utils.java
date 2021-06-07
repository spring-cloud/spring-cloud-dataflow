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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

/**
 * Base64 utils to encode/decode boot properties so that we can have special
 * characters in keys.
 *
 * @author Janne Valkealahti
 */
public class Base64Utils {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final String PREFIX = "base64_";

	public static String encode(String str) {
		if (!StringUtils.hasLength(str)) {
			return str;
		}
		// need to use without padding as boot will remove '=' chars from keys
		return PREFIX + new String(Base64.getEncoder().withoutPadding().encode(str.getBytes(DEFAULT_CHARSET)),
				DEFAULT_CHARSET);
	}

	public static String decode(String str) {
		if (!StringUtils.hasLength(str)) {
			return str;
		}
		if (str.startsWith(PREFIX)) {
			return new String(Base64.getDecoder().decode(str.substring(PREFIX.length())), DEFAULT_CHARSET);
		} else {
			return str;
		}
	}

	public static Map<String, String> decodeMap(Map<String, String> map) {
		if (map == null) {
			return map;
		}
		return map.entrySet().stream().collect(Collectors.toMap(e -> decode(e.getKey()), e -> decode(e.getValue())));
	}
}
