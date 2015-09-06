/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.core.dsl;

/**
 * @author Andy Clement
 */
public enum ChannelType {
	QUEUE("queue:", null),
	TOPIC("topic:", null),
	TAP_QUEUE("tap:queue:", TapSource.QUEUE),
	TAP_TOPIC("tap:topic:", TapSource.TOPIC),
	TAP_STREAM("tap:stream:", TapSource.STREAM),
	TAP_JOB("tap:job:", TapSource.JOB);

	enum TapSource {
		QUEUE, TOPIC, STREAM, JOB;
	}

	private final TapSource tapSource;

	private final String stringRepresentation;

	ChannelType(String stringRepresentation, TapSource tapSource) {
		this.stringRepresentation = stringRepresentation;
		this.tapSource = tapSource;
	}

	public String getStringRepresentation() {
		return this.stringRepresentation;
	}

	public boolean isTap() {
		return this.tapSource != null;
	}

	public TapSource tapSource() {
		return this.tapSource;
	}
}
