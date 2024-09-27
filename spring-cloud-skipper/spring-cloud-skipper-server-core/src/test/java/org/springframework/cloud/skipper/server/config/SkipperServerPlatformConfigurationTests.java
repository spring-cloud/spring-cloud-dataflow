/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.cloud.skipper.server.config;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.autoconfigure.ResourceLoadingAutoConfiguration;
import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Donovan Muller
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Corneil du Plessis
 */

@Suite
@SelectClasses({
		SkipperServerPlatformConfigurationTests.AllPlatformsConfigurationTest.class,
		SkipperServerPlatformConfigurationTests.ExternalPlatformsOnlyConfigurationTest.class,
		SkipperServerPlatformConfigurationTests.SinglePlatformConfigurationTest.class
})
public class SkipperServerPlatformConfigurationTests {

	@SpringBootTest(classes = TestConfig.class, properties = "spring.main.allow-bean-definition-overriding=true")
	@ActiveProfiles({"platform-configuration", "local"})
	@Nested
	public class AllPlatformsConfigurationTest {

		@Autowired
		private List<Platform> platforms;

		@Test
		public void allPlatformsConfiguredTest() {
			assertThat(platforms).extracting("name").containsExactlyInAnyOrder("Local", "Test");
		}
	}

	@Nested
    @SpringBootTest(classes = TestConfig.class,
			properties = {"spring.main.allow-bean-definition-overriding=true"})
    public class SinglePlatformConfigurationTest {

		@Autowired
		private List<Platform> platforms;

		@Test
		public void singlePlatformsConfiguredTest() {
			assertThat(platforms.get(0).getDeployers()).extracting("name").containsExactly("test");
		}
	}

	@SpringBootTest(classes = TestConfig.class,
			properties = {"spring.main.allow-bean-definition-overriding=true"})
	@ActiveProfiles("platform-configuration")
	@Nested
	public class ExternalPlatformsOnlyConfigurationTest {

		@Autowired
		private List<Platform> platforms;

		@Test
		public void localPlatformDisabledTest() {
			assertThat(platforms).extracting("name").containsExactly("Test");
		}
	}

	@Configuration
	@ImportAutoConfiguration(classes = { EmbeddedDataSourceConfiguration.class, HibernateJpaAutoConfiguration.class,
			StateMachineJpaRepositoriesAutoConfiguration.class, SkipperServerPlatformConfiguration.class,
			TestPlatformAutoConfiguration.class, ResourceLoadingAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	static class TestConfig {
	}

	@Configuration
	static class TestPlatformAutoConfiguration {

		@Bean
		public Platform testPlatform() {
			return new Platform("Test", Collections.singletonList(
					new Deployer("test", "test", new AppDeployer() {

						@Override
						public String deploy(AppDeploymentRequest request) {
							return null;
						}

						@Override
						public void undeploy(String id) {
						}

						@Override
						public AppStatus status(String id) {
							return null;
						}

						@Override
						public RuntimeEnvironmentInfo environmentInfo() {
							return null;
						}

						@Override
						public String getLog(String id) {
							return null;
						}
					}, new ActuatorOperations() {

						@Override
						public <T> T getFromActuator(String deploymentId,
								String guid, String endpoint, Class<T> responseType,
								Optional<HttpHeaders> requestHeaders) {
							return null;
						}

						@Override
						public <T, R> R postToActuator(String deploymentId, String guid,
								String endpoint, T body, Class<R> responseType,
								Optional<HttpHeaders> requestHeaders) {
							return null;
						}
					})));
		}
	}
}
