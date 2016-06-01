/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.server.repository.support.DefinitionRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class, RdbmsDeploymentIdRepositoryTests.TestConfig.class})
public class RdbmsDeploymentIdRepositoryTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private DeploymentIdRepository repository;

	private JdbcTemplate template;

	@Configuration
	static class TestConfig {

		@Bean
		public DefinitionRepositoryInitializer definitionRepositoryInitializer(DataSource dataSource) {
			DefinitionRepositoryInitializer definitionRepositoryInitializer = new  DefinitionRepositoryInitializer();
			definitionRepositoryInitializer.setDataSource(dataSource);
			return definitionRepositoryInitializer;
		}

		@Bean
		public DeploymentIdRepository rdbmsDeploymentIdRepository(DataSource dataSource) {
			return new RdbmsDeploymentIdRepository(dataSource);
		}
	}

	@Before
	public void setup() throws Exception{
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
}
