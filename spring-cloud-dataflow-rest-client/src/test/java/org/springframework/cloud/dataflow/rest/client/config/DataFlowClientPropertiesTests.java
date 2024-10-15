/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.client.config;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;


class DataFlowClientPropertiesTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void defaults() {
		this.contextRunner
			.withUserConfiguration(Config1.class)
			.run((context) -> {
				DataFlowClientProperties properties = context.getBean(DataFlowClientProperties.class);
				assertThat(properties.getServerUri()).isEqualTo("http://localhost:9393");
				assertThat(properties.isEnableDsl()).isFalse();
				assertThat(properties.isSkipSslValidation()).isFalse();
				assertThat(properties.getAuthentication().getAccessToken()).isNull();
				assertThat(properties.getAuthentication().getClientId()).isNull();
				assertThat(properties.getAuthentication().getClientSecret()).isNull();
				assertThat(properties.getAuthentication().getTokenUri()).isNull();
				assertThat(properties.getAuthentication().getScope()).isNull();
				assertThat(properties.getAuthentication().getBasic().getUsername()).isNull();
				assertThat(properties.getAuthentication().getBasic().getPassword()).isNull();
			});
	}

	@Test
	void basicAuth() {
		this.contextRunner
			.withInitializer(context -> {
				Map<String, Object> map = new HashMap<>();
				map.put("spring.cloud.dataflow.client.authentication.basic.username", "user1");
				map.put("spring.cloud.dataflow.client.authentication.basic.password", "pw1");
				context.getEnvironment().getPropertySources().addLast(new SystemEnvironmentPropertySource(
					StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, map));
			})
			.withUserConfiguration(Config1.class)
			.run((context) -> {
				DataFlowClientProperties properties = context.getBean(DataFlowClientProperties.class);
				assertThat(properties.getAuthentication().getBasic().getUsername()).isEqualTo("user1");
				assertThat(properties.getAuthentication().getBasic().getPassword()).isEqualTo("pw1");
			});
	}

	@Test
	void legacyOauth() {
		this.contextRunner
			.withInitializer(context -> {
				Map<String, Object> map = new HashMap<>();
				map.put("spring.cloud.dataflow.client.authentication.access-token", "token1");
				map.put("spring.cloud.dataflow.client.authentication.client-id", "id1");
				map.put("spring.cloud.dataflow.client.authentication.client-secret", "secret1");
				map.put("spring.cloud.dataflow.client.authentication.token-uri", "uri1");
				map.put("spring.cloud.dataflow.client.authentication.scope", "s1,s2");
				context.getEnvironment().getPropertySources().addLast(new SystemEnvironmentPropertySource(
					StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, map));
			})
			.withUserConfiguration(Config1.class)
			.run((context) -> {
				DataFlowClientProperties properties = context.getBean(DataFlowClientProperties.class);
				assertThat(properties.getAuthentication().getAccessToken()).isEqualTo("token1");
				assertThat(properties.getAuthentication().getClientId()).isEqualTo("id1");
				assertThat(properties.getAuthentication().getClientSecret()).isEqualTo("secret1");
				assertThat(properties.getAuthentication().getTokenUri()).isEqualTo("uri1");
				assertThat(properties.getAuthentication().getScope()).containsExactlyInAnyOrder("s1", "s2");
			});
	}

	@Test
	void commonSpringSecurity() {
		this.contextRunner
			.withInitializer(context -> {
				Map<String, Object> map = new HashMap<>();
				map.put("spring.cloud.dataflow.client.authentication.oauth2.client-registration-id", "regid1");
				map.put("spring.cloud.dataflow.client.authentication.oauth2.username", "user1");
				map.put("spring.cloud.dataflow.client.authentication.oauth2.password", "pw1");
				context.getEnvironment().getPropertySources().addLast(new SystemEnvironmentPropertySource(
					StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, map));
			})
			.withUserConfiguration(Config1.class)
			.run((context) -> {
				DataFlowClientProperties properties = context.getBean(DataFlowClientProperties.class);
				assertThat(properties.getAuthentication().getOauth2().getClientRegistrationId()).isEqualTo("regid1");
				assertThat(properties.getAuthentication().getOauth2().getUsername()).isEqualTo("user1");
				assertThat(properties.getAuthentication().getOauth2().getPassword()).isEqualTo("pw1");
			});
	}

	@EnableConfigurationProperties({ DataFlowClientProperties.class })
	private static class Config1 {
	}
}
