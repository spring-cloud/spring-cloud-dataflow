/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.core.dsl.tck;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.dsl.StreamNode;
import org.springframework.cloud.dataflow.core.dsl.StreamParser;

class StreamDslTests extends AbstractStreamDslTests {

	@Test
	void test() {
	}

	@Override
	protected StreamNode parse(String streamDefinition) {
		return new StreamParser(streamDefinition).parse();
	}

	@Override
	protected StreamNode parse(String streamName, String streamDefinition) {
		return new StreamParser(streamName, streamDefinition).parse();
	}
}
