/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.deployer.metadata;

import java.util.List;

import org.junit.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class DeployerConfigurationMetadataResolverTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void testDeployerConfigurationMetadata() {
		this.contextRunner
			.run((context) -> {
				DeployerConfigurationMetadataResolver resolver = new DeployerConfigurationMetadataResolver();
				resolver.setApplicationContext(context);
				List<ConfigurationMetadataProperty> data = resolver.resolve();
				assertThat(data.size()).isGreaterThan(0);
			});
	}
}
