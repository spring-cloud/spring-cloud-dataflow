/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.server.autoconfigure;

import java.util.Collections;

import org.junit.Test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.skipper.server.config.SkipperServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

public class SkipperServerPropertiesTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void defaultNoPropSet() {
		this.contextRunner
			.withUserConfiguration(Config1.class)
			.run((context) -> {
				SkipperServerProperties properties = context.getBean(SkipperServerProperties.class);
				assertThat(properties.isEnableLocalPlatform()).isTrue();
				assertThat(context).hasBean("myBean1");
				assertThat(context).hasBean("myBean2");
			});
	}

	@Test
	public void noUnderscore() {
		this.contextRunner
			// env variables can only be tests by using SystemEnvironmentPropertySource
			// and setting its name to 'systemEnvironment'
			.withInitializer(context -> {
				context.getEnvironment().getPropertySources().addLast(new SystemEnvironmentPropertySource(
						StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						Collections.singletonMap("SPRING_CLOUD_SKIPPER_SERVER_ENABLELOCALPLATFORM", "false")));
			})
			.withUserConfiguration(Config1.class)
			.run((context) -> {
				SkipperServerProperties properties = context.getBean(SkipperServerProperties.class);
				assertThat(properties.isEnableLocalPlatform()).isFalse();
				assertThat(context).doesNotHaveBean("myBean1");
				assertThat(context).doesNotHaveBean("myBean2");
			});
	}

	@Test
	public void useUnderscore() {
		this.contextRunner
			.withInitializer(context -> {
				context.getEnvironment().getPropertySources().addLast(new SystemEnvironmentPropertySource(
						StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						Collections.singletonMap("SPRING_CLOUD_SKIPPER_SERVER_ENABLE_LOCAL_PLATFORM", "false")));
			})
			.withUserConfiguration(Config1.class)
			.run((context) -> {
				SkipperServerProperties properties = context.getBean(SkipperServerProperties.class);
				assertThat(properties.isEnableLocalPlatform()).isFalse();
				// shows how underscores in a property name from env variable doesn't work
				// if binding uses camel case format but works with kebab case naming.
				// leaving this assert here for future reference in case it starts failing
				assertThat(context).hasBean("myBean1");
				assertThat(context).doesNotHaveBean("myBean2");
			});
	}

	@EnableConfigurationProperties({ SkipperServerProperties.class })
	private static class Config1 {

		@Bean
		@ConditionalOnProperty(value = "spring.cloud.skipper.server.enableLocalPlatform", matchIfMissing = true)
		public String myBean1() {
			return "hi";
		}

		@Bean
		@ConditionalOnProperty(value = "spring.cloud.skipper.server.enable-local-platform", matchIfMissing = true)
		public String myBean2() {
			return "hi";
		}
	}
}
