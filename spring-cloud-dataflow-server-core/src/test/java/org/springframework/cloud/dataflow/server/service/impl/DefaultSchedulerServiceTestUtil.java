/*
 * Copyright 2018-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.ListAssert;

import static org.assertj.core.api.Assertions.assertThat;

final class DefaultSchedulerServiceTestUtil {
	
	private DefaultSchedulerServiceTestUtil() {
	}

	static ListAssert<String> assertThatCommandLineArgsHaveNonDefaultArgs(List<String> args, String argsPrefix, String... nonDefaultArgs) {
		List<String> expectedArgs = new ArrayList<>();
		expectedArgs.addAll(DefaultSchedulerServiceTestUtil.defaultCommandLineArgs(argsPrefix));
		Arrays.stream(nonDefaultArgs).forEach(expectedArgs::add);
		return assertThat(args).containsExactlyInAnyOrder(expectedArgs.toArray(new String[0]));
	}

	static List<String> defaultCommandLineArgs(String prefix) {
		List<String> args = new ArrayList<>();
		args.add(prefix + ".spring.cloud.task.initialize-enabled=false");
		return args;
	}
}
