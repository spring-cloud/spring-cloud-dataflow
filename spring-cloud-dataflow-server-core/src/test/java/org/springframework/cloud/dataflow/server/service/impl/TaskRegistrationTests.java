/*
 * Copyright 2023-2023 the original author or authors.
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

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepository;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepositoryCustom;
import org.springframework.cloud.dataflow.registry.repository.AppRegistrationRepositoryImpl;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.registry.service.DefaultAppRegistryService;
import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
		classes = {TaskRegistrationTests.TestConfiguration.class},
		properties = {"spring.main.allow-bean-definition-overriding=true"}
)
@ImportAutoConfiguration({
		HibernateJpaAutoConfiguration.class,
		JacksonAutoConfiguration.class,
		FlywayAutoConfiguration.class
})
@EntityScan({
		"org.springframework.cloud.dataflow.registry.domain",
		"org.springframework.cloud.dataflow.core"
})
@EnableMapRepositories("org.springframework.cloud.dataflow.server.job")
@EnableJpaRepositories(basePackages = {
		"org.springframework.cloud.dataflow.registry.repository",
		"org.springframework.cloud.dataflow.server.repository",
		"org.springframework.cloud.dataflow.audit.repository"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TaskRegistrationTests {
	@Autowired
	AppRegistryService appRegistryService;

	@Test
	void registration() throws URISyntaxException {
		// given
		appRegistryService.save("timestamp", ApplicationType.task, "2.0.2", new URI("maven://io.spring:timestamp-task:2.0.2"), null);
		appRegistryService.save("timestamp", ApplicationType.task, "3.0.0", new URI("maven://io.spring:timestamp-task:3.0.0"), null);
		// when
		AppRegistration timestamp = appRegistryService.find("timestamp", ApplicationType.task, "2.0.2");
		AppRegistration timestamp3 = appRegistryService.find("timestamp", ApplicationType.task, "3.0.0");
		// then
		assertThat(timestamp).isNotNull();

		assertThat(timestamp3).isNotNull();

	}


	@Configuration
	static class TestConfiguration {

		@Bean
		public AuditRecordService auditRecordService() {
			return mock(DefaultAuditRecordService.class);
		}

		@Bean
		public AppResourceCommon appResourceCommon(@Nullable MavenProperties mavenProperties,
												   DelegatingResourceLoader delegatingResourceLoader) {
			return new AppResourceCommon(mavenProperties, delegatingResourceLoader);
		}

		@Bean
		public DelegatingResourceLoader resourceLoader(MavenProperties mavenProperties) {
			Map<String, ResourceLoader> resourceLoaders = new HashMap<>();
			resourceLoaders.put("maven", new MavenResourceLoader(mavenProperties));
			resourceLoaders.put("file", new FileSystemResourceLoader());

			return new DelegatingResourceLoader(resourceLoaders);
		}

		@Bean
		public AppRegistrationRepositoryCustom appRegistrationRepositoryCustom(EntityManager entityManager) {
			return new AppRegistrationRepositoryImpl(entityManager);
		}

		@Bean
		public AppRegistryService appRegistryService(
				AppRegistrationRepository appRegistrationRepository,
				AppResourceCommon appResourceCommon,
				AuditRecordService auditRecordService
		) {
			return new DefaultAppRegistryService(appRegistrationRepository, appResourceCommon, auditRecordService);
		}

		@Bean
		public MavenProperties mavenProperties() {
			MavenProperties mavenProperties = new MavenProperties();
			Map<String, MavenProperties.RemoteRepository> repositories = new HashMap<>();
			repositories.put("springRepo", new MavenProperties.RemoteRepository("https://repo.spring.io/snapshot"));
			repositories.put("springMilestone", new MavenProperties.RemoteRepository("https://repo.spring.io/milestone"));
			repositories.put("mavenCentral", new MavenProperties.RemoteRepository("https://repo.maven.apache.org/maven2"));
			mavenProperties.setRemoteRepositories(repositories);
			return mavenProperties;
		}
	}
}
