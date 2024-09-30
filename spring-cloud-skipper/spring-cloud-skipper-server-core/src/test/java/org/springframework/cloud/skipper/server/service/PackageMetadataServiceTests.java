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
package org.springframework.cloud.skipper.server.service;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.autoconfigure.ResourceLoadingAutoConfiguration;
import org.springframework.cloud.skipper.server.config.SkipperServerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.UrlResource;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = PackageMetadataServiceTests.TestConfig.class, properties = "spring.main.allow-bean-definition-overriding=true")
class PackageMetadataServiceTests {

	@Autowired
	private PackageMetadataService packageMetadataService;

	@Test
	void calculateFilename() throws IOException {
		UrlResource urlResource = new UrlResource("file:./spring-cloud-skipper-server/src/test/resources/index.yml");
		String filename = packageMetadataService.computeFilename(urlResource);
		assertThat(filename).isEqualTo("file_dot_spring-cloud-skipper-server_src_test_resources_index.yml");
		urlResource = new UrlResource(
				"file:/home/mpollack/projects/spring-cloud-skipper/spring-cloud-skipper-server/src/test/resources/index.yml");
		filename = packageMetadataService.computeFilename(urlResource);
		assertThat(filename).isEqualTo(
				"file_home_mpollack_projects_spring-cloud-skipper_spring-cloud-skipper-server_src_test_resources_index.yml");
		urlResource = new UrlResource("http://localhost:8081/index.yml");
		filename = packageMetadataService.computeFilename(urlResource);
		assertThat(filename).isEqualTo("localhost_index.yml");

		urlResource = new UrlResource("https://www.example.com/index.yml");
		filename = packageMetadataService.computeFilename(urlResource);
		assertThat(filename).isEqualTo("www.example.com_index.yml");
	}

	@Configuration
	@ImportAutoConfiguration(classes = { JacksonAutoConfiguration.class, EmbeddedDataSourceConfiguration.class,
			HibernateJpaAutoConfiguration.class, StateMachineJpaRepositoriesAutoConfiguration.class,
			ResourceLoadingAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	static class TestConfig {
	}
}
