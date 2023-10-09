/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.cloud.dataflow.integration.test.util;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.with;

/**
 * Various assert utils.
 *
 * @author Janne Valkealahti
 */
public abstract class AssertUtils {

	private static final Logger log = LoggerFactory.getLogger(AssertUtils.class);

	public static void assertDataflowServerNotRunning(String url) {
		assertThatThrownBy(() -> {
			assertServerResponse("Spring Cloud Data Flow", url, 5, TimeUnit.SECONDS, 120, TimeUnit.SECONDS);
		});
	}

	public static void assertDataflowServerRunning(String url) {
		assertServerResponse("Spring Cloud Data Flow", url, 5, TimeUnit.SECONDS, 120, TimeUnit.SECONDS);
	}

	public static void assertSkipperServerNotRunning(String url) {
		assertThatThrownBy(() -> {
			assertServerResponse("Spring Cloud Skipper Server", url, 5, TimeUnit.SECONDS, 120, TimeUnit.SECONDS);
		});
	}

	public static void assertSkipperServerRunning(String url) {
		assertServerResponse("Spring Cloud Skipper Server", url, 5, TimeUnit.SECONDS, 120, TimeUnit.SECONDS);
	}

	public static void assertServerResponse(
		String responseContains,
		String url,
		long pollInterval,
		TimeUnit pollTimeUnit,
		long awaitInterval,
		TimeUnit awaitTimeUnit
	) {
		RestTemplate template = new RestTemplate();
		with()
			.pollInterval(pollInterval, pollTimeUnit)
			.and()
			.await()
			.atMost(awaitInterval, awaitTimeUnit)
			.until(() -> {
				log.info("Checking url {}", url);
				try {
					String response = template.getForObject(url, String.class);
					log.info("Response is {}", response);
					boolean ok = response != null && response.contains(responseContains);
					if (!ok) {
						log.warn("Failed check {} for url {}", ok, url);
					}
					return ok;
				} catch (Throwable x) {
					log.warn("Exception:" + x);
					return false;
				}
			});
	}
}
