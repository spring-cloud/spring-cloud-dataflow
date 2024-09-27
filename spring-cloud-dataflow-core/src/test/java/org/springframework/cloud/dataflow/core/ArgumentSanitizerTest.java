/*
 * Copyright 2018-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
class ArgumentSanitizerTest {

	private ArgumentSanitizer sanitizer;

	private static final String[] keys = { "password", "secret", "key", "token", ".*credentials.*",
			"vcap_services", "url" };

	@BeforeEach
	void before() {
		sanitizer = new ArgumentSanitizer();
	}

	@Test
	void sanitizeProperties() {
		for (String key : keys) {
			assertThat(sanitizer.sanitize("--" + key + "=foo")).isEqualTo("--" + key + "=******");
			assertThat(sanitizer.sanitize(key, "bar")).isEqualTo("******");
		}
	}

	@Test
	void testSanitizeArguments() {
		final List<String> arguments = new ArrayList<>();

		for (String key : keys) {
			arguments.add("--" + key + "=foo");
		}

		final List<String> sanitizedArguments = sanitizer.sanitizeArguments(arguments);

		assertThat(sanitizedArguments).hasSize(keys.length);

		int order = 0;
		for(String sanitizedString : sanitizedArguments) {
			assertThat(sanitizedString).isEqualTo("--" + keys[order] + "=******");
			order++;
		}
	}


	@Test
	void multipartProperty() {
		assertThat(sanitizer.sanitize("--password=boza")).isEqualTo("--password=******");
		assertThat(sanitizer.sanitize("--one.two.password=boza")).isEqualTo("--one.two.password=******");
		assertThat(sanitizer.sanitize("--one_two_password=boza")).isEqualTo("--one_two_password=******");
	}

//	@Test
//	public void testHierarchicalPropertyNames() {
//		assertEquals("time --password='******' | log",
//				sanitizer.(new StreamDefinition("stream", "time --password=bar | log")));
//	}
//
//	@Test
//	public void testStreamPropertyOrder() {
//		assertEquals("time --some.password='******' --another-secret='******' | log",
//				sanitizer.sanitizeStream(new StreamDefinition("stream", "time --some.password=foobar --another-secret=kenny | log")));
//	}
//
//	@Test
//	public void testStreamMatcherWithHyphenDotChar() {
//		assertEquals("twitterstream --twitter.credentials.access-token-secret='******' "
//						+ "--twitter.credentials.access-token='******' --twitter.credentials.consumer-secret='******' "
//						+ "--twitter.credentials.consumer-key='******' | "
//						+ "filter --expression=#jsonPath(payload,'$.lang')=='en' | "
//						+ "twitter-sentiment --vocabulary=https://dl.bintray.com/test --model-fetch=output/test "
//						+ "--model=https://dl.bintray.com/test | field-value-counter --field-name=sentiment --name=sentiment",
//				sanitizer.sanitizeStream(new StreamDefinition("stream", "twitterstream "
//						+ "--twitter.credentials.consumer-key=dadadfaf --twitter.credentials.consumer-secret=dadfdasfdads "
//						+ "--twitter.credentials.access-token=58849055-dfdae "
//						+ "--twitter.credentials.access-token-secret=deteegdssa4466 | filter --expression='#jsonPath(payload,''$.lang'')==''en''' | "
//						+ "twitter-sentiment --vocabulary=https://dl.bintray.com/test --model-fetch=output/test --model=https://dl.bintray.com/test | "
//						+ "field-value-counter --field-name=sentiment --name=sentiment")));
//	}
//
//	@Test
//	public void testStreamSanitizeOriginalDsl() {
//		StreamDefinition streamDefinition = new StreamDefinition("test", "time --password='******' | log --password='******'", "time --password='******' | log");
//		assertEquals("time --password='******' | log", sanitizer.sanitizeOriginalStreamDsl(streamDefinition));
//	}
}
