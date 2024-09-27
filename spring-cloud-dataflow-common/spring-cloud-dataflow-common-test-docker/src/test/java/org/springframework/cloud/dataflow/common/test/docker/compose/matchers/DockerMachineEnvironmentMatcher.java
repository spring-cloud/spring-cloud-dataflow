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
package org.springframework.cloud.dataflow.common.test.docker.compose.matchers;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.hasEntry;

import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Description;
import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerMachine;

public class DockerMachineEnvironmentMatcher extends ValueCachingMatcher<DockerMachine> {

	private final Map<String, String> expected;

	public DockerMachineEnvironmentMatcher(Map<String, String> expected) {
		this.expected = expected;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("Docker Machine to have these environment variables:\n");
		description.appendValue(expected);
	}

	@Override
	protected boolean matchesSafely() {
		return missingEnvironmentVariables().isEmpty();
	}

	@Override
	protected void describeMismatchSafely(DockerMachine item, Description mismatchDescription) {
		mismatchDescription.appendText("\nThese environment variables were missing:\n");
		mismatchDescription.appendValue(missingEnvironmentVariables());
	}

	public static DockerMachineEnvironmentMatcher containsEnvironment(Map<String, String> environment) {
		return new DockerMachineEnvironmentMatcher(new HashMap<>(environment));
	}

	private Map<String, String> missingEnvironmentVariables() {
		Map<String, String> environment = value().configuredDockerComposeProcess()
											   .environment();
		return expected.entrySet()
					   .stream()
					   .filter(required -> !hasEntry(required.getKey(), required.getValue()).matches(environment))
					   .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

}
