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

package org.springframework.cloud.skipper.support.yaml;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code YamlConversionStatus} keeps more information around
 * for status of a conversion. For example warnings if something
 * is removed or errors if something cannot be converted.
 *
 * @author Kris De Volder
 * @author Janne Valkealahti
 *
 */
public class YamlConversionStatus {

	public static final YamlConversionStatus EMPTY = new YamlConversionStatus();
	public static final int OK = 0;
	public static final int WARNING = 1;
	public static final int ERROR = 2;
	private int severity;
	private List<ConversionMessage> entries = new ArrayList<>();

	void addError(String message) {
		entries.add(new ConversionMessage(ERROR, message));
		if (severity < ERROR) {
			severity = ERROR;
		}
	}

	void addWarning(String message) {
		entries.add(new ConversionMessage(WARNING, message));
		if (severity < WARNING) {
			severity = WARNING;
		}
	}

	/**
	 * Gets the status entries.
	 *
	 * @return the status entries
	 */
	public List<ConversionMessage> getEntries() {
		return entries;
	}

	/**
	 * Gets the severity.
	 *
	 * @return the severity
	 */
	public int getSeverity() {
		return severity;
	}

	static class ConversionMessage {

		private int severity;
		private String message;

		ConversionMessage(int severity, String message) {
			this.severity = severity;
			this.message = message;
		}

		public int getSeverity() {
			return severity;
		}

		public String getMessage() {
			return message;
		}
	}
}
