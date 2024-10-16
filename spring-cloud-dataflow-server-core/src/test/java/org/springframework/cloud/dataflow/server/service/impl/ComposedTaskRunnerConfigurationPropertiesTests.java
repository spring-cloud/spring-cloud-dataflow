/*
 * Copyright 2024 the original author or authors.
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

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ComposedTaskRunnerConfigurationPropertiesTests {

	private static final String DEFAULT_URI = "https://testuri.something";

	@Test
	void useUserAccessTokenFromCTRPropsTrue() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
			new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUseUserAccessToken(true);
		assertThat(composedTaskRunnerConfigurationProperties.isUseUserAccessToken()).as("Use user access token should be true").isTrue();
	}

	@Test
	void useUserAccessTokenFromCTRPropFalse() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
			new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUseUserAccessToken(false);
		assertThat(composedTaskRunnerConfigurationProperties.isUseUserAccessToken()).as("Use user access token should be false").isFalse();
	}

	@Test
	void useUserAccessTokenFromCTRPropNotSet() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
			new ComposedTaskRunnerConfigurationProperties();
		assertThat(composedTaskRunnerConfigurationProperties.isUseUserAccessToken()).as("Use user access token should be false").isNull();
	}

	@Test
	void uriFromComposedTaskRunnerConfigurationProperties() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
			new ComposedTaskRunnerConfigurationProperties();
		composedTaskRunnerConfigurationProperties.setUri(DEFAULT_URI);

		assertThat(composedTaskRunnerConfigurationProperties.getUri()).as("DEFAULT_URI is not being returned from properties").isEqualTo(DEFAULT_URI);
	}

	@Test
	void emptyUriFromComposedTaskRunnerConfigurationProperties() {
		ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
			new ComposedTaskRunnerConfigurationProperties();

		assertThat(composedTaskRunnerConfigurationProperties.getUri()).as("URI should be empty").isNull();
	}
}
