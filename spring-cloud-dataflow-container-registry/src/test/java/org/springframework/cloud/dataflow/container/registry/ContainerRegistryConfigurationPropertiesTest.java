/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.container.registry;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class ContainerRegistryConfigurationPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void registryConfigurationProperties() {
		this.contextRunner
				.withInitializer(context -> {
					Map<String, Object> map = new HashMap<>();

					// harbor - dockeroauth2
					map.put("spring.cloud.dataflow.container.registry-configurations[goharbor].registry-host", "demo.goharbor.io");
					map.put("spring.cloud.dataflow.container.registry-configurations[goharbor].authorization-type", "dockeroauth2");
					map.put("spring.cloud.dataflow.container.registry-configurations[goharbor].user", "admin");
					map.put("spring.cloud.dataflow.container.registry-configurations[goharbor].secret", "Harbor12345");
					map.put("spring.cloud.dataflow.container.registry-configurations[goharbor].disable-ssl-verification", "true");

					// amazon ecr - awsecr
					map.put("spring.cloud.dataflow.container.registry-configurations[myamazonaws].registry-host", "283191309520.dkr.ecr.us-west-1.amazonaws.com");
					map.put("spring.cloud.dataflow.container.registry-configurations[myamazonaws].authorization-type", "awsecr");
					map.put("spring.cloud.dataflow.container.registry-configurations[myamazonaws].user", "myawsuser");
					map.put("spring.cloud.dataflow.container.registry-configurations[myamazonaws].secret", "myawspassword");
					map.put("spring.cloud.dataflow.container.registry-configurations[myamazonaws].extra[region]", "us-west-1");
					map.put("spring.cloud.dataflow.container.registry-configurations[myamazonaws].extra[registryIds]", "283191309520");

					context.getEnvironment().getPropertySources().addLast(new SystemEnvironmentPropertySource(
							StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, map));
				})
				.withUserConfiguration(Config1.class)
				.run((context) -> {
					ContainerRegistryProperties properties = context.getBean(ContainerRegistryProperties.class);
					assertThat(properties.getRegistryConfigurations()).hasSize(2);

					ContainerRegistryConfiguration goharborConf = properties.getRegistryConfigurations().get("goharbor");
					assertThat(goharborConf).isNotNull();
					assertThat(goharborConf.getRegistryHost()).isEqualTo("demo.goharbor.io");
					assertThat(goharborConf.getAuthorizationType()).isEqualTo(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
					assertThat(goharborConf.getUser()).isEqualTo("admin");
					assertThat(goharborConf.getSecret()).isEqualTo("Harbor12345");
					assertThat(goharborConf.isDisableSslVerification()).isTrue();
					assertThat(goharborConf.getExtra()).isEmpty();

					ContainerRegistryConfiguration myamazonawsConf = properties.getRegistryConfigurations().get("myamazonaws");
					assertThat(myamazonawsConf).isNotNull();
					assertThat(myamazonawsConf.getRegistryHost()).isEqualTo("283191309520.dkr.ecr.us-west-1.amazonaws.com");
					assertThat(myamazonawsConf.getAuthorizationType()).isEqualTo(ContainerRegistryConfiguration.AuthorizationType.awsecr);
					assertThat(myamazonawsConf.getUser()).isEqualTo("myawsuser");
					assertThat(myamazonawsConf.getSecret()).isEqualTo("myawspassword");
					assertThat(myamazonawsConf.isDisableSslVerification()).isFalse();
					assertThat(myamazonawsConf.getExtra()).hasSize(2);
					assertThat(myamazonawsConf.getExtra().get("region")).isEqualTo("us-west-1");
					assertThat(myamazonawsConf.getExtra().get("registryIds")).isEqualTo("283191309520");
				});
	}

	@EnableConfigurationProperties({ ContainerRegistryProperties.class })
	private static class Config1 {
	}
}
