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
package org.springframework.cloud.skipper.support;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.util.StringUtils;

/**
 * {@code DurationUtils} together with {@link DurationStyle} is to support Boot
 * 2.x style expressions while we're still staying on Boot 1.x. This is meant to
 * provide smooth upgrade path by not breaking expression formats exposed to
 * user.
 *
 * @author Janne Valkealahti
 *
 */
public final class DurationUtils {

	/**
	 * Converts a duration expression into {@link Duration}.
	 *
	 * @param expression the duration expression
	 * @return the parsed duration
	 */
	public static Duration convert(String expression) {
		return convert(expression, null);
	}

	/**
	 * Converts a duration expression into {@link Duration} with a given
	 * {@link ChronoUnit}.
	 *
	 * @param expression the duration expression
	 * @param unit the chrono unit
	 * @return the parsed duration
	 */
	public static Duration convert(String expression, ChronoUnit unit) {
		if (!StringUtils.hasText(expression)) {
			return null;
		}
		return DurationStyle.detect(expression).parse(expression, unit);
	}
}
