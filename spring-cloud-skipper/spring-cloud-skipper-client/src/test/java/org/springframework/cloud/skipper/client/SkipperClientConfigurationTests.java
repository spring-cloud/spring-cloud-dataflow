/*
 * Copyright 2017-2020 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SkipperClientConfiguration}.
 *
 * @author Janne Valkealahti
 * @author Corneil du Plessis
 *
 */
@SpringBootTest(classes = SkipperClientConfigurationTests.TestConfig.class)
public class SkipperClientConfigurationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testDefaultRestTemplateBeanName() {
		assertThat(context.containsBean(SkipperClientConfiguration.SKIPPERCLIENT_RESTTEMPLATE_BEAN_NAME)).isTrue();
	}

	@Configuration
	@ImportAutoConfiguration(classes = { JacksonAutoConfiguration.class, RestTemplateAutoConfiguration.class,
			SkipperClientConfiguration.class })
	static class TestConfig {
	}
}
