/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.job.support;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Provides a commons set of time-related helper methods and also defines common date/time
 * formats.
 *
 * @author Gunnar Hillert
 * @author Glenn Renfro
 * @since 1.0
 */
public final class TimeUtils {

	public static final String DEFAULT_DATAFLOW_DATE_FORMAT_PATTERN = "yyyy-MM-dd";

	public static final String DEFAULT_DATAFLOW_TIME_FORMAT_PATTERN = "HH:mm:ss";

	public static final String DEFAULT_DATAFLOW_DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

	public static final String DEFAULT_DATAFLOW_DURATION_FORMAT_PATTERN = "HH:mm:ss";

	public static final String DEFAULT_DATAFLOW_TIMEZONE_ID = "UTC";

	public static final String DEFAULT_DATAFLOW_DATE_TIME_PARAMETER_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss,SSS";

	/**
	 * Prevent instantiation.
	 */
	private TimeUtils() {
		throw new AssertionError();
	}

	/**
	 * @return Default Spring Cloud DataFLow {@link TimeZone} using ID
	 */
	public static TimeZone getDefaultTimeZone() {
		return TimeZone.getTimeZone(DEFAULT_DATAFLOW_TIMEZONE_ID);
	}

	/**
	 * @return The JVM {@link TimeZone}.
	 */
	public static TimeZone getJvmTimeZone() {
		return TimeZone.getDefault();
	}

	/**
	 * @return The Default Spring Cloud Data Flow {@link DateFormat} using pattern
	 * {@link TimeUtils#DEFAULT_DATAFLOW_DATE_FORMAT_PATTERN}
	 */
	public static DateFormat getDefaultDateFormat() {
		return new SimpleDateFormat(DEFAULT_DATAFLOW_DATE_FORMAT_PATTERN);
	}

	/**
	 * @return The Default Spring Cloud Data Flow time format using {@link DateFormat}
	 * pattern {@link TimeUtils#DEFAULT_DATAFLOW_TIME_FORMAT_PATTERN}
	 */
	public static DateFormat getDefaultTimeFormat() {
		return new SimpleDateFormat(DEFAULT_DATAFLOW_TIME_FORMAT_PATTERN);
	}

	/**
	 * @return The Default Spring Cloud Data Flow date/time format using
	 * {@link DateFormat} pattern
	 * {@link TimeUtils#DEFAULT_DATAFLOW_DATE_TIME_FORMAT_PATTERN}
	 */
	public static DateFormat getDefaultDateTimeFormat() {
		return new SimpleDateFormat(DEFAULT_DATAFLOW_DATE_TIME_FORMAT_PATTERN);
	}

	/**
	 * @return The Default Spring Cloud Data Flow duration format using {@link DateFormat}
	 * pattern {@link TimeUtils#DEFAULT_DATAFLOW_DURATION_FORMAT_PATTERN}
	 */
	public static DateFormat getDefaultDurationFormat() {
		return new SimpleDateFormat(DEFAULT_DATAFLOW_DURATION_FORMAT_PATTERN);
	}

}
