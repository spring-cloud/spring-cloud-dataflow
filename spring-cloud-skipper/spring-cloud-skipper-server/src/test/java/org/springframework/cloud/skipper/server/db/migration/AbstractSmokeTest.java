/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.cloud.skipper.server.db.migration;

import java.util.Collections;

import javax.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.cloud.skipper.server.EnableSkipperServer;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.jpa.AppDeployerDataRepository;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides for testing some basic database schema and JPA tests to catch potential issues with specific databases early.
 *
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = AbstractSmokeTest.LocalTestSkipperServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "repo-test"})
@TestPropertySource(properties = {
	"spring.jpa.hibernate.ddl-auto=none",
	"logging.level.org.springframework.cloud=info",
	"logging.level.org.hibernate=debug"
})
public abstract class AbstractSmokeTest {
	private static final Logger logger = LoggerFactory.getLogger(AbstractSmokeTest.class);

	protected static JdbcDatabaseContainer<?> container;

	@Autowired
	AppDeployerDataRepository appDeployerDataRepository;

	@Autowired
	Environment environment;

	@Autowired
	EntityManagerFactory entityManagerFactory;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", container::getJdbcUrl);
		registry.add("spring.datasource.username", container::getUsername);
		registry.add("spring.datasource.password", container::getPassword);
		registry.add("spring.datasource.driver-class-name", container::getDriverClassName);
	}


	@Test
	public void testStart() {
		logger.info("started:{}", getClass().getSimpleName());
		AppDeployerData deployerData = new AppDeployerData();
		deployerData.setDeploymentDataUsingMap(Collections.singletonMap("a", "b"));
		deployerData.setReleaseVersion(1);
		deployerData.setReleaseName("a");
		deployerData = appDeployerDataRepository.save(deployerData);
		assertThat(deployerData.getId()).isNotNull();
		assertThat(deployerData.getId()).isNotEqualTo(0);
		assertThat(deployerData.getDeploymentDataAsMap()).isNotEmpty();
		assertThat(deployerData.getDeploymentDataAsMap()).containsEntry("a", "b");
		logger.info("completed:{}", getClass().getSimpleName());
	}

	@SpringBootApplication(exclude = {CloudFoundryDeployerAutoConfiguration.class,
		LocalDeployerAutoConfiguration.class,
		KubernetesAutoConfiguration.class,
		SessionAutoConfiguration.class,
		CommonSecurityAutoConfiguration.class
	})
	@EnableSkipperServer
	public static class LocalTestSkipperServer {
		public static void main(String[] args) {
			SpringApplication.run(LocalTestSkipperServer.class, args);
		}
	}
}
