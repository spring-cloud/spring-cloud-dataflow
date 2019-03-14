/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.rest.support.jackson;

import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.util.StdDateFormat;

/**
 * Wrapper for the {@link StdDateFormat} to use {@link TimeZone} and {@link Locale} based constructor.
 *
 * @author Gunnar Hillert
 * @author Daniel Serleg
 */
@SuppressWarnings("serial")
public class ISO8601DateFormatWithMilliSeconds extends StdDateFormat {

	public ISO8601DateFormatWithMilliSeconds(TimeZone tz, Locale loc, Boolean lenient) {
		super(tz, loc, lenient);
	}

	public ISO8601DateFormatWithMilliSeconds() {
		super();
	}
}
