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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

/**
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
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
		JobParameter[] PARAMETERS = {new JobParameter("foo", String.class, true),
				new JobParameter("bar", String.class, true),
				new JobParameter("baz", String.class, true),
				new JobParameter(1L, Long.class, true),
				new JobParameter(1D, Double.class, true),
				new JobParameter(testDate, Date.class, false)};

		Map<String, JobParameter<?>> jobParamMap = new LinkedHashMap<>();
		for (int paramCount = 0; paramCount < JOB_PARAM_KEYS.length; paramCount++) {
			jobParamMap.put(JOB_PARAM_KEYS[paramCount], PARAMETERS[paramCount]);
		}
		JobParameters jobParameters = new JobParameters(jobParamMap);
		JobParameters sanitizedJobParameters = this.sanitizer.sanitizeJobParameters(jobParameters);
		for(Map.Entry<String, JobParameter<?>> entry : sanitizedJobParameters.getParameters().entrySet()) {
			if (entry.getKey().equals("username") || entry.getKey().equals("password")) {
				Assert.assertEquals("******", entry.getValue().getValue());
			}
			else if (entry.getKey().equals("name")) {
				Assert.assertEquals("{value=baz, type=class java.lang.String, identifying=true}", entry.getValue().getValue());
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
	public void testSanitizeNullArgument() {
		final List<String> arguments = new ArrayList<>();

		arguments.add(null);

		final List<String> sanitizedArguments = sanitizer.sanitizeArguments(arguments);

		Assert.assertEquals(1, sanitizedArguments.size());
		Assert.assertEquals(sanitizedArguments.get(0), "");
	}


	@Test
	public void testMultipartProperty() {
		Assert.assertEquals("--password=******", sanitizer.sanitize("--password=boza"));
		Assert.assertEquals("--one.two.password=******", sanitizer.sanitize("--one.two.password=boza"));
		Assert.assertEquals("--one_two_password=******", sanitizer.sanitize("--one_two_password=boza"));
	}
	private String loadStringFromResource(String uri) throws IOException {
		Resource resource = new DefaultResourceLoader().getResource(uri);
		try (Reader reader = new InputStreamReader(resource.getInputStream())) {
			return FileCopyUtils.copyToString(reader);
		}
	}
	@Test
	public void testJsonData() throws IOException {
		String input = loadStringFromResource("classpath:sanitizer1.json");
		String output = sanitizer.sanitizeJsonOrYamlString(input);
		System.out.println("Read:" + input);
		System.out.println("Sanitized:" + output);
		Assert.assertTrue(output.contains("*****"));
		Assert.assertFalse(output.contains("54321"));

	}

	@Test
	public void testYamlData() throws IOException {
		String input = loadStringFromResource("classpath:sanitizer2.yaml");
		String output = sanitizer.sanitizeJsonOrYamlString(input);
		System.out.println("Read:" + input);
		System.out.println("Sanitized:" + output);
		Assert.assertTrue(output.contains("*****"));
		Assert.assertFalse(output.contains("54321"));
	}

}
