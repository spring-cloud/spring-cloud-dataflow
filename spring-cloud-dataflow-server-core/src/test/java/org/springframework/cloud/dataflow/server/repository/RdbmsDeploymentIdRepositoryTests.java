/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.repository.support.DataflowRdbmsInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class, RdbmsDeploymentIdRepositoryTests.TestConfig.class})
public class RdbmsDeploymentIdRepositoryTests {

	@Autowired
	private DataSource dataSource;

	private DeploymentIdRepository repository;

	private JdbcTemplate template;

	@Before
	public void setup() throws Exception{
		repository = new RdbmsDeploymentIdRepository(dataSource);
		template = new JdbcTemplate(dataSource);
		template.execute("DELETE FROM DEPLOYMENT_IDS");
	}
	
	@Test
	public void testFindOne() {
		repository.save("key1", "time.1");
		repository.save("key2", "log.0");

		assertEquals("log.0", repository.findOne("key2"));
	}

	@Test
	public void testDelete() {
		repository.save("key1", "time.1");
		repository.save("key2", "log.0");

		assertEquals("log.0", repository.findOne("key2"));
		repository.delete("key2");
		assertNull(repository.findOne("key2"));
	}

	@Configuration
	protected static class TestConfig {

		@Bean
		public FeaturesProperties featuresProperties() {
			return new FeaturesProperties();
		}

		@Bean
		public DataflowRdbmsInitializer definitionRepositoryInitializer(DataSource dataSource) {
			DataflowRdbmsInitializer definitionRepositoryInitializer = new DataflowRdbmsInitializer(featuresProperties());
			definitionRepositoryInitializer.setDataSource(dataSource);
			return definitionRepositoryInitializer;
		}
	}
}
