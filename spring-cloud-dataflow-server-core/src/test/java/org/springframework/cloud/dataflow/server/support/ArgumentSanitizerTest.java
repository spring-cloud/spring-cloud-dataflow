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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.rest.util.ArgumentSanitizer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

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
	void testSanitizeJobParameters() {
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
				assertThat(entry.getValue().getValue()).isEqualTo("******");
			}
			else if (entry.getKey().equals("name")) {
				assertThat(entry.getValue().getValue()).isEqualTo("{value=baz, type=class java.lang.String, identifying=true}");
			}
			else if (entry.getKey().equals("C")) {
				assertThat(entry.getValue().getValue()).isEqualTo(1L);
			}
			else if (entry.getKey().equals("D")) {
				assertThat(entry.getValue().getValue()).isEqualTo(1D);
			}
			else if (entry.getKey().equals("E")) {
				assertThat(entry.getValue().getValue()).isEqualTo(testDate);
			}
		}
	}

	@Test
	void sanitizeTaskDefinition() {
		TaskDefinition taskDefinition = new TaskDefinition("mytask", "task1 --some.password=foobar --another-secret=kenny");
		assertThat(this.sanitizer.sanitizeTaskDsl(taskDefinition)).isEqualTo("task1 --some.password='******' --another-secret='******'");
	}


	@Test
	void sanitizeComposedTaskDefinition() {
		TaskDefinition taskDefinition = new TaskDefinition("mytask", "task1 --some.password=foobar && task2 --some.password=woof");
		assertThat(this.sanitizer.sanitizeTaskDsl(taskDefinition)).isEqualTo("task1 --some.password='******' && task2 --some.password='******'");
	}

	@Test
	void sanitizeComposedTaskSplitDefinition() {
		TaskDefinition taskDefinition = new TaskDefinition(
				"mytask", "<task1 --some.password=foobar || task2 --some.password=woof> && task3  --some.password=foobar");
		assertThat(this.sanitizer.sanitizeTaskDsl(taskDefinition)).isEqualTo("<task1 --some.password='******' || task2 --some.password='******'> && task3 --some.password='******'");
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
	void sanitizeNullArgument() {
		final List<String> arguments = new ArrayList<>();

		arguments.add(null);

		final List<String> sanitizedArguments = sanitizer.sanitizeArguments(arguments);

		assertThat(sanitizedArguments).hasSize(1);
		assertThat(sanitizedArguments.get(0)).isEmpty();
	}


	@Test
	void multipartProperty() {
		assertThat(sanitizer.sanitize("--password=boza")).isEqualTo("--password=******");
		assertThat(sanitizer.sanitize("--one.two.password=boza")).isEqualTo("--one.two.password=******");
		assertThat(sanitizer.sanitize("--one_two_password=boza")).isEqualTo("--one_two_password=******");
	}
	private String loadStringFromResource(String uri) throws IOException {
		Resource resource = new DefaultResourceLoader().getResource(uri);
		try (Reader reader = new InputStreamReader(resource.getInputStream())) {
			return FileCopyUtils.copyToString(reader);
		}
	}

	@Test
	void jsonData() throws IOException {
		String input = loadStringFromResource("classpath:sanitizer1.json");
		String output = sanitizer.sanitizeJsonOrYamlString(input);
		System.out.println("Read:" + input);
		System.out.println("Sanitized:" + output);
		assertThat(output).contains("*****");
		assertThat(output).doesNotContain("54321");

	}

	@Test
	void yamlData() throws IOException {
		String input = loadStringFromResource("classpath:sanitizer2.yaml");
		String output = sanitizer.sanitizeJsonOrYamlString(input);
		System.out.println("Read:" + input);
		System.out.println("Sanitized:" + output);
		assertThat(output).contains("*****");
		assertThat(output).doesNotContain("54321");
	}

}
