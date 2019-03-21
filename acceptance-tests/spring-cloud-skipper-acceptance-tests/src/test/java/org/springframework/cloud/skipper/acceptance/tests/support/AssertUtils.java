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
package org.springframework.cloud.skipper.acceptance.tests.support;

import static com.jayway.awaitility.Awaitility.with;

import java.util.concurrent.TimeUnit;

import org.springframework.web.client.RestTemplate;

/**
 * Various assert utils.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AssertUtils {

	public static void assertServerRunning(String url) {
		assertServerRunning("Spring Cloud Skipper Server", url, 1, TimeUnit.SECONDS, 120, TimeUnit.SECONDS);
	}

	public static void assertServerRunning(String responseContains, String url, long pollInterval,
			TimeUnit pollTimeUnit, long awaitInterval, TimeUnit awaitTimeUnit) {
		RestTemplate template = new RestTemplate();
		with()
			.pollInterval(pollInterval, pollTimeUnit)
			.and()
			.await()
				.ignoreExceptions()
				.atMost(awaitInterval, awaitTimeUnit)
				.until(() -> template.getForObject(url, String.class).contains(responseContains));
	}
}
