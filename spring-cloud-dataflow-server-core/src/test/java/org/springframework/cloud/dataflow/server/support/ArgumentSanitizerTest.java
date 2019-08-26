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

package org.springframework.cloud.dataflow.server.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 */
public class ArgumentSanitizerTest {

	private ArgumentSanitizer sanitizer;

	private static final String[] keys = { "password", "secret", "key", "token", ".*credentials.*",
			"vcap_services", "url" };

	@Before
	public void before() {
		sanitizer = new ArgumentSanitizer();
	}

	@Test
	public void testSanitizeProperties() {
		for (String key : keys) {
			Assert.assertEquals("--" + key + "=******", sanitizer.sanitize("--" + key + "=foo"));
			Assert.assertEquals("******", sanitizer.sanitize(key, "bar"));
		}
	}

	@Test
	public void testSanitizeJobParameters() {
		String[] JOB_PARAM_KEYS = {"username", "password", "name", "C", "D", "E"};
		Date testDate = new Date();
		JobParameter[] PARAMETERS = {new JobParameter("foo", true),
				new JobParameter("bar", true),
				new JobParameter("baz", true),
				new JobParameter(1L, true),
				new JobParameter(1D, true),
				new JobParameter(testDate, false)};

		Map<String, JobParameter> jobParamMap = new LinkedHashMap<>();
		for (int paramCount = 0; paramCount < JOB_PARAM_KEYS.length; paramCount++) {
			jobParamMap.put(JOB_PARAM_KEYS[paramCount], PARAMETERS[paramCount]);
		}
		JobParameters jobParameters = new JobParameters(jobParamMap);
		JobParameters sanitizedJobParameters = this.sanitizer.sanitizeJobParameters(jobParameters);
		for(Map.Entry<String, JobParameter> entry : sanitizedJobParameters.getParameters().entrySet()) {
			if (entry.getKey().equals("username") || entry.getKey().equals("password")) {
				Assert.assertEquals("******", entry.getValue().getValue());
			}
			else if (entry.getKey().equals("name")) {
				Assert.assertEquals("baz", entry.getValue().getValue());
			}
			else if (entry.getKey().equals("C")) {
				Assert.assertEquals(1L, entry.getValue().getValue());
			}
			else if (entry.getKey().equals("D")) {
				Assert.assertEquals(1D, entry.getValue().getValue());
			}
			else if (entry.getKey().equals("E")) {
				Assert.assertEquals(testDate, entry.getValue().getValue());
			}
		}
	}

	@Test
	public void testSanitizeTaskDefinition() {
		TaskDefinition taskDefinition = new TaskDefinition("mytask", "task1 --some.password=foobar --another-secret=kenny");
		Assert.assertEquals("task1 --some.password='******' --another-secret='******'", this.sanitizer.sanitizeTaskDsl(taskDefinition));
	}


	@Test
	public void testSanitizeComposedTaskDefinition() {
		TaskDefinition taskDefinition = new TaskDefinition("mytask", "task1 --some.password=foobar && task2 --some.password=woof");
		Assert.assertEquals("task1 --some.password='******' && task2 --some.password='******'", this.sanitizer.sanitizeTaskDsl(taskDefinition));
	}

	@Test
	public void testSanitizeComposedTaskSplitDefinition() {
		TaskDefinition taskDefinition = new TaskDefinition(
				"mytask", "<task1 --some.password=foobar || task2 --some.password=woof> && task3  --some.password=foobar");
		Assert.assertEquals(
				"<task1 --some.password='******' || task2 --some.password='******'> && task3 --some.password='******'",
				this.sanitizer.sanitizeTaskDsl(taskDefinition));
	}

	@Test
	public void testSanitizeArguments() {
		final List<String> arguments = new ArrayList<>();

		for (String key : keys) {
			arguments.add("--" + key + "=foo");
		}

		final List<String> sanitizedArguments = sanitizer.sanitizeArguments(arguments);

		Assert.assertEquals(keys.length, sanitizedArguments.size());

		int order = 0;
		for(String sanitizedString : sanitizedArguments) {
			Assert.assertEquals("--" + keys[order] + "=******", sanitizedString);
			order++;
		}
	}


	@Test
	public void testMultipartProperty() {
		Assert.assertEquals("--password=******", sanitizer.sanitize("--password=boza"));
		Assert.assertEquals("--one.two.password=******", sanitizer.sanitize("--one.two.password=boza"));
		Assert.assertEquals("--one_two_password=******", sanitizer.sanitize("--one_two_password=boza"));
	}

	@Test
	public void testHierarchicalPropertyNames() {
		Assert.assertEquals("time --password='******' | log",
				sanitizer.sanitizeStream(new StreamDefinition("stream", "time --password=bar | log")));
	}

	@Test
	public void testStreamPropertyOrder() {
		Assert.assertEquals("time --some.password='******' --another-secret='******' | log",
				sanitizer.sanitizeStream(new StreamDefinition("stream", "time --some.password=foobar --another-secret=kenny | log")));
	}

	@Test
	public void testStreamMatcherWithHyphenDotChar() {
		Assert.assertEquals("twitterstream --twitter.credentials.access-token-secret='******' "
						+ "--twitter.credentials.access-token='******' --twitter.credentials.consumer-secret='******' "
						+ "--twitter.credentials.consumer-key='******' | "
						+ "filter --expression=#jsonPath(payload,'$.lang')=='en' | "
						+ "twitter-sentiment --vocabulary=https://dl.bintray.com/test --model-fetch=output/test "
						+ "--model=https://dl.bintray.com/test | field-value-counter --field-name=sentiment --name=sentiment",
				sanitizer.sanitizeStream(new StreamDefinition("stream", "twitterstream "
						+ "--twitter.credentials.consumer-key=dadadfaf --twitter.credentials.consumer-secret=dadfdasfdads "
						+ "--twitter.credentials.access-token=58849055-dfdae "
						+ "--twitter.credentials.access-token-secret=deteegdssa4466 | filter --expression='#jsonPath(payload,''$.lang'')==''en''' | "
						+ "twitter-sentiment --vocabulary=https://dl.bintray.com/test --model-fetch=output/test --model=https://dl.bintray.com/test | "
						+ "field-value-counter --field-name=sentiment --name=sentiment")));
	}

	@Test
	public void testStreamSanitizeOriginalDsl() {
		StreamDefinition streamDefinition = new StreamDefinition("test", "time --password='******' | log --password='******'", "time --password='******' | log");
		Assert.assertEquals("time --password='******' | log", sanitizer.sanitizeOriginalStreamDsl(streamDefinition));
	}
}
